package com.aiproject.smartcampus.model.intent.router;

import com.aiproject.smartcampus.commons.client.ResultCilent;
import com.aiproject.smartcampus.commons.client.StatusCilent;
import com.aiproject.smartcampus.commons.easyuse.RateLimiterWrapper;
import com.aiproject.smartcampus.commons.easyuse.TaskExecutor;
import com.aiproject.smartcampus.commons.utils.CreateDiagram;
import com.aiproject.smartcampus.handler.contenthandler.ContentCheckClient;
import com.aiproject.smartcampus.model.intent.summer.ModelSummer;
import com.aiproject.smartcampus.pojo.bo.TaskAction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

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
        long maxExecutionTime = 300_000; // 5分钟超时
        int currentLayer = 0;
        int maxLayers = 50; // 防止无限循环
        int emptyLayerRetries = 0; //空层重试计数
        final int MAX_EMPTY_RETRIES = 5; //最大重试次数

        while (currentLayer < maxLayers) {
            // 检查超时
            if (System.currentTimeMillis() - startTime > maxExecutionTime) {
                log.error("整体执行超时");
                return false;
            }

            // 获取当前层级可执行的任务（入度为0的任务）
            List<String> currentLayerTasks = createDiagram.getReadyList();
            if (currentLayerTasks == null || currentLayerTasks.isEmpty()) {
                //检查是否还有未完成的任务
                if (createDiagram.hasUnfinishedTasks()) {
                    emptyLayerRetries++;
                    if (emptyLayerRetries >= MAX_EMPTY_RETRIES) {
                        log.error("第{}层连续{}次没有就绪任务，但仍有未完成任务，可能存在死锁", currentLayer, MAX_EMPTY_RETRIES);
                        return false;
                    }
                    log.warn("第{}层没有就绪任务，但还有未完成任务，等待500ms后重试 ({}/{})",
                            currentLayer, emptyLayerRetries, MAX_EMPTY_RETRIES);
                    try {
                        Thread.sleep(500); // 等待异步状态更新
                        continue; // 重新检查，不增加layer计数
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return false;
                    }
                } else {
                    log.info("第{}层没有可执行任务，执行完成", currentLayer);
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
            // 等待一小段时间确保状态更新完成
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }

            currentLayer++;
        }
        if (currentLayer >= maxLayers) {
            log.error("执行层数超过最大限制，可能存在循环依赖");
            return false;
        }
        log.info("所有层级任务执行完成，共{}层", currentLayer);
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
            //增加等待时间
            allLayerTasks.get(120, TimeUnit.SECONDS); // 从100秒增加到120秒
            log.info("第{}层的{}个任务全部并行完成", layerIndex, layerTasks.size());
            Thread.sleep(1000); // 等待1秒确保入度更新完成

            return true;

        } catch (TimeoutException e) {
            log.error("第{}层任务执行超时", layerIndex, e);
            layerFutures.forEach(future -> {
                if (!future.isDone()) {
                    future.cancel(true);
                }
            });
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
                    //等待任务真正完成
                    CompletableFuture<String> taskResult = taskExecutor.executeAsync(task, preTaskResults);

                    // 同步等待任务完成
                    String result = taskResult.get(120, TimeUnit.SECONDS);
                    log.info("任务[{}]执行完成,结果为{}", task,result);
                }
                log.info("第{}层 - 任务{}处理完成", layerIndex, task);
            } catch (Exception e) {
                log.error("第{}层 - 任务{}执行失败", layerIndex, task, e);
                TaskAction failAction = TaskAction.statusUpdate(task, "FAILED");
                statusCilent.push(failAction);
                throw new RuntimeException("任务执行失败: " + task, e);
            }
        });
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