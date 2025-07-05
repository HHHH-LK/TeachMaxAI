package com.aiproject.smartcampus.model.router;

import com.aiproject.smartcampus.commons.client.ResultCilent;
import com.aiproject.smartcampus.commons.client.StatusCilent;
import com.aiproject.smartcampus.commons.easyuse.RateLimiterWrapper;
import com.aiproject.smartcampus.commons.easyuse.TaskExecutor;
import com.aiproject.smartcampus.commons.utils.CreateDiagram;
import com.aiproject.smartcampus.handler.contenthandler.ContentCheckClient;
import com.aiproject.smartcampus.model.summer.ModelSummer;
import com.aiproject.smartcampus.pojo.bo.TaskAction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.*;

/**
 * @program: SmartCampus
 * @description: 分层并行执行的子意图路由器
 * 同一级别任务并行，不同级别串行
 * @author: lk
 * @create: 2025-05-27 23:35
 **/

@Component
@Slf4j
@RequiredArgsConstructor
public class StepIntentRouter implements StepRouter {

    private final TaskExecutor taskExecutor;
    private final RateLimiterWrapper rateLimiter;
    private final CreateDiagram createDiagram;
    private final ResultCilent resultCilent;
    private final ContentCheckClient contentCheckClient;
    private final ModelSummer modelSummer;
    private final StatusCilent statusCilent;

    // 🔧 优化后的配置常量
    private static final int MAX_EXECUTION_TIME_MINUTES = 10;           // 总超时时间：10分钟
    private static final int MAX_LAYERS = 50;                           // 最大层数：50层
    private static final int MAX_EMPTY_RETRIES = 15;                   // 空层最大重试次数：5 -> 15
    private static final long EMPTY_LAYER_WAIT_MS = 10_000;           // 空层等待时间：5s -> 10s
    private static final long STATE_UPDATE_WAIT_MS = 2_000;           // 状态更新等待：500ms -> 2s
    private static final long LAYER_COMPLETION_WAIT_MS = 3_000;       // 层完成等待：5s -> 3s
    private static final int LAYER_TASK_TIMEOUT_SECONDS = 180;        // 层任务超时：120s -> 180s (3分钟)
    private static final int SINGLE_TASK_TIMEOUT_SECONDS = 180;       // 单任务超时：120s -> 180s (3分钟)

    @Override
    public String route(List<String> intents) {
        log.info("开始进行意图路由,意图为：{}", intents);
        // 内容检查
        boolean check = contentCheckClient.check(intents);
        if (!check) {
            log.error("内容检测不通过，包含违规内容");
            return "内容检测不通过,存在非法关键词,请检查输入内容是否符合规范";
        }
        try {
            // 按层级执行任务
            boolean success = executeTasksByLayers();
            if (!success) {
                log.error("任务执行失败或超时");
                return "任务执行失败，请稍后重试";
            }
            // 获取总结
            String summer = modelSummer.summer(intents);
            return summer;
        } catch (Exception e) {
            log.error("路由执行过程中发生异常", e);
            return "处理请求时发生异常，请稍后重试";
        }
    }

    /**
     * 按层级执行任务 - 核心方法
     * 同一层级并行，不同层级串行
     */
    private boolean executeTasksByLayers() {
        long startTime = System.currentTimeMillis();
        long maxExecutionTime = MAX_EXECUTION_TIME_MINUTES * 60 * 1000L; // 转换为毫秒
        int currentLayer = 0;
        int emptyLayerRetries = 0; // 空层重试计数

        log.info("开始执行任务，最大执行时间: {}分钟，最大重试次数: {}",
                MAX_EXECUTION_TIME_MINUTES, MAX_EMPTY_RETRIES);

        while (currentLayer < MAX_LAYERS) {
            // 检查超时
            long elapsedTime = System.currentTimeMillis() - startTime;
            if (elapsedTime > maxExecutionTime) {
                log.error("整体执行超时，已执行{}ms，超过限制{}ms", elapsedTime, maxExecutionTime);
                return false;
            }

            // 获取当前层级可执行的任务（入度为0的任务）
            List<String> currentLayerTasks = createDiagram.getReadyList();
            if (currentLayerTasks == null || currentLayerTasks.isEmpty()) {
                // 检查是否还有未完成的任务
                if (createDiagram.hasUnfinishedTasks()) {
                    emptyLayerRetries++;
                    if (emptyLayerRetries >= MAX_EMPTY_RETRIES) {
                        log.error("第{}层连续{}次没有就绪任务，但仍有未完成任务，可能存在死锁或任务执行时间过长",
                                currentLayer, MAX_EMPTY_RETRIES);
                        printTaskDiagnostics(); // 打印诊断信息
                        return false;
                    }

                    long remainingTime = maxExecutionTime - elapsedTime;
                    log.warn("第{}层没有就绪任务，但还有未完成任务，等待{}ms后重试 ({}/{}) [剩余时间: {}ms]",
                            currentLayer, EMPTY_LAYER_WAIT_MS, emptyLayerRetries, MAX_EMPTY_RETRIES, remainingTime);

                    try {
                        Thread.sleep(EMPTY_LAYER_WAIT_MS); // 增加等待时间
                        continue; // 重新检查，不增加layer计数
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        log.warn("等待被中断，停止执行");
                        return false;
                    }
                } else {
                    log.info("第{}层没有可执行任务，所有任务执行完成", currentLayer);
                    break;
                }
            }

            // 重置重试计数
            emptyLayerRetries = 0;
            log.info("开始执行第{}层任务，共{}个任务: {}", currentLayer, currentLayerTasks.size(), currentLayerTasks);

            // 并行执行当前层级的所有任务
            boolean layerSuccess = executeCurrentLayerInParallel(currentLayerTasks, currentLayer);
            if (!layerSuccess) {
                log.error("第{}层任务执行失败", currentLayer);
                return false;
            }

            log.info("第{}层任务全部完成", currentLayer);

            // 等待状态更新完成，但时间缩短
            try {
                Thread.sleep(LAYER_COMPLETION_WAIT_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("层完成等待被中断");
                return false;
            }

            currentLayer++;
        }

        if (currentLayer >= MAX_LAYERS) {
            log.error("执行层数超过最大限制{}，可能存在循环依赖", MAX_LAYERS);
            return false;
        }

        long totalTime = System.currentTimeMillis() - startTime;
        log.info("所有层级任务执行完成，共{}层，总耗时: {}ms", currentLayer, totalTime);
        return true;
    }

    /**
     * 并行执行当前层级的所有任务
     */
    private boolean executeCurrentLayerInParallel(List<String> layerTasks, int layerIndex) {
        List<CompletableFuture<Void>> layerFutures = new ArrayList<>();

        // 为当前层级的每个任务创建异步执行
        for (String task : layerTasks) {
            CompletableFuture<Void> taskFuture = executeTaskAsync(task, layerIndex);
            layerFutures.add(taskFuture);
        }

        try {
            // 等待当前层级的所有任务完成
            CompletableFuture<Void> allLayerTasks = CompletableFuture.allOf(
                    layerFutures.toArray(new CompletableFuture[0])
            );

            // 增加层级任务超时时间
            allLayerTasks.get(LAYER_TASK_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            log.info("第{}层的{}个任务全部并行完成", layerIndex, layerTasks.size());

            // 缩短状态更新等待时间
            Thread.sleep(STATE_UPDATE_WAIT_MS);
            return true;

        } catch (TimeoutException e) {
            log.error("第{}层任务执行超时，超时时间: {}秒", layerIndex, LAYER_TASK_TIMEOUT_SECONDS, e);

            // 取消未完成的任务
            int cancelledCount = 0;
            for (CompletableFuture<Void> future : layerFutures) {
                if (!future.isDone()) {
                    future.cancel(true);
                    cancelledCount++;
                }
            }
            log.warn("已取消{}个未完成的任务", cancelledCount);
            return false;

        } catch (Exception e) {
            log.error("第{}层任务执行异常", layerIndex, e);
            return false;
        }
    }

    /**
     * 异步执行单个任务
     */
    private CompletableFuture<Void> executeTaskAsync(String task, int layerIndex) {
        return CompletableFuture.runAsync(() -> {
            try {
                log.info("第{}层 - 开始执行任务: {}", layerIndex, task);

                // 获取前置任务结果
                List<CompletableFuture<String>> preTaskResults = resultCilent.getPreTask(task);

                // 检查限流
                if (!rateLimiter.isAllow(task)) {
                    log.info("任务{}被限流，进入延迟队列", task);
                    TaskAction action = TaskAction.statusUpdate(task, "RATE_LIMITED");
                    statusCilent.push(action);
                    taskExecutor.enqueueDelayed(task, preTaskResults);
                } else {
                    log.info("任务{}开始正常执行", task);
                    TaskAction action = TaskAction.statusUpdate(task, "RUNNING");
                    statusCilent.push(action);

                    // 等待任务真正完成，增加超时时间
                    CompletableFuture<String> taskResult = taskExecutor.executeAsync(task, preTaskResults);

                    // 同步等待任务完成，增加超时时间
                    String result = taskResult.get(SINGLE_TASK_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                    log.info("任务[{}]执行完成,结果长度: {}", task, result != null ? result.length() : 0);
                }
                log.info("第{}层 - 任务{}处理完成", layerIndex, task);

            } catch (TimeoutException e) {
                log.error("第{}层 - 任务{}执行超时，超时时间: {}秒", layerIndex, task, SINGLE_TASK_TIMEOUT_SECONDS, e);
                TaskAction timeoutAction = TaskAction.statusUpdate(task, "TIMEOUT");
                statusCilent.push(timeoutAction);
                throw new RuntimeException("任务执行超时: " + task, e);

            } catch (Exception e) {
                log.error("第{}层 - 任务{}执行失败", layerIndex, task, e);
                TaskAction failAction = TaskAction.statusUpdate(task, "FAILED");
                statusCilent.push(failAction);
                throw new RuntimeException("任务执行失败: " + task, e);
            }
        });
    }

    /**
     * 打印任务诊断信息 - 详细版本
     */
    private void printTaskDiagnostics() {
        try {
            log.error("=== 任务诊断信息 ===");

            if (createDiagram != null) {
                // 1. 基本统计信息
                List<String> allTasks = createDiagram.getAllTasks();
                List<String> readyTasks = createDiagram.getReadyList();
                boolean hasUnfinished = createDiagram.hasUnfinishedTasks();

                log.error("总任务数量: {}", allTasks.size());
                log.error("就绪任务数量: {}", readyTasks != null ? readyTasks.size() : 0);
                log.error("是否存在未完成任务: {}", hasUnfinished);

                // 2. 就绪任务详情
                if (readyTasks != null && !readyTasks.isEmpty()) {
                    log.error("就绪任务列表: {}", readyTasks);
                } else {
                    log.error("当前没有就绪任务");
                }

                // 3. 分析每个任务的状态和入度
                log.error("--- 所有任务状态分析 ---");
                Map<String, Integer> statusCount = new HashMap<>();

                for (String task : allTasks) {
                    try {
                        int currentInDegree = createDiagram.getCurrentInDegree(task);
                        int originalInDegree = createDiagram.getInDegree(task);
                        String status = statusCilent != null ?
                                getTaskStatus(task) : "UNKNOWN";

                        // 统计状态
                        statusCount.merge(status, 1, Integer::sum);

                        // 只打印有问题的任务
                        if (currentInDegree > 0 || !"DONE".equals(status)) {
                            log.error("任务: {}, 当前入度: {}, 原始入度: {}, 状态: {}",
                                    task, currentInDegree, originalInDegree, status);

                            // 如果入度大于0，显示其父任务
                            if (currentInDegree > 0) {
                                List<String> parents = createDiagram.getParetents(task);
                                log.error("  -> 父任务: {}", parents);

                                // 检查父任务状态
                                for (String parent : parents) {
                                    String parentStatus = getTaskStatus(parent);
                                    log.error("     父任务 {} 状态: {}", parent, parentStatus);
                                }
                            }
                        }
                    } catch (Exception e) {
                        log.error("分析任务 {} 时出错", task, e);
                    }
                }

                // 4. 状态统计
                log.error("--- 任务状态统计 ---");
                for (Map.Entry<String, Integer> entry : statusCount.entrySet()) {
                    log.error("状态 {}: {} 个任务", entry.getKey(), entry.getValue());
                }

                // 5. 图结构验证
                boolean isValid = createDiagram.validateGraph();
                log.error("图结构是否有效: {}", isValid);

                // 6. 打印图的调试信息
                log.error("--- 图结构调试信息 ---");
                createDiagram.printDebugInfo();
            }

            log.error("=== 诊断信息结束 ===");

        } catch (Exception e) {
            log.error("打印诊断信息时发生异常", e);
        }
    }

    /**
     * 获取任务状态的辅助方法
     */
    private String getTaskStatus(String task) {
        try {
            // 这里需要根据你的实际StatusClient实现来获取状态
            // 如果StatusClient有直接获取状态的方法，使用它
            // 否则可能需要通过其他方式获取
            return "UNKNOWN"; // 占位符，需要根据实际情况实现
        } catch (Exception e) {
            log.debug("获取任务 {} 状态失败", task, e);
            return "ERROR";
        }
    }

    /**
     * 计算任务的层级（基于入度和依赖关系）
     */
    private int calculateTaskLayer(String task) {
        // 如果入度为0，则为第0层
        if (createDiagram.getInDegree(task) == 0) {
            return 0;
        }
        // 否则，层级 = 所有前置任务的最大层级 + 1
        List<String> parents = createDiagram.getParetents(task);
        int maxParentLayer = parents.stream()
                .mapToInt(this::calculateTaskLayer)
                .max()
                .orElse(-1);

        return maxParentLayer + 1;
    }
}