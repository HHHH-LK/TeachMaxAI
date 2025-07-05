package com.aiproject.smartcampus.commons.delayedtask;

import com.aiproject.smartcampus.commons.client.EnhancedHandlerRegisterClient;
import com.aiproject.smartcampus.commons.client.ResultCilent;
import com.aiproject.smartcampus.commons.client.StatusCilent;
import com.aiproject.smartcampus.commons.utils.CreateDiagram;
import com.aiproject.smartcampus.model.handler.Handler;
import com.aiproject.smartcampus.pojo.bo.TaskAction;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.concurrent.*;

/**
 * @program: SmartCampus
 * @description: 延时队列客户端 - 完善版
 * @author: lk
 * @create: 2025-05-28 15:37
 **/

@Component
@RequiredArgsConstructor
@Slf4j
public class IntentDelayedQueueClien {

    private final EnhancedHandlerRegisterClient handlerRegiserCilent;
    private final DelayQueue<IntentBatchTask> intentBatchTaskQueue = new DelayQueue<>();
    private final ExecutorService EXECUTOR_SERVICE = Executors.newFixedThreadPool(20);
    private volatile boolean running = true;
    private final StatusCilent statusCilent;
    private final ResultCilent resultCilent;
    private final CreateDiagram createDiagram;

    // 用于跟踪正在处理的任务，避免重复提交
    private final Set<String> processingTasks = ConcurrentHashMap.newKeySet();

    // 超时配置
    private static final int DEPENDENCY_WAIT_TIMEOUT = 30; // 依赖等待超时（秒）
    private static final int TASK_EXECUTION_TIMEOUT = 180; // 任务执行超时（秒）

    /**
     * 添加延时任务
     */
    public boolean addTask(IntentBatchTask intentBatchTask) {
        String intent = intentBatchTask.getIntents();

        if (intent == null || intent.trim().isEmpty()) {
            log.warn("延时任务intent为空，跳过添加");
            return false;
        }

        // 检查任务是否已在处理中
        if (processingTasks.contains(intent)) {
            log.info("任务[{}]已在延时队列中，跳过重复添加", intent);
            return false;
        }

        processingTasks.add(intent);
        boolean added = intentBatchTaskQueue.offer(intentBatchTask);

        if (added) {
            log.info("延时任务已添加：{}，当前队列大小: {}", intent, intentBatchTaskQueue.size());
        } else {
            log.error("延时任务添加失败：{}", intent);
            processingTasks.remove(intent);
        }

        return added;
    }

    /**
     * 检查任务是否在延时队列中
     */
    public boolean containsTask(String intent) {
        return processingTasks.contains(intent);
    }

    @PostConstruct
    public void start() {
        Thread consumerThread = new Thread(() -> {
            log.info("延时队列处理线程启动");
            while (running && !Thread.currentThread().isInterrupted()) {
                try {
                    IntentBatchTask task = intentBatchTaskQueue.take();
                    processDelayedTask(task);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.info("延时队列处理线程被中断");
                    break;
                } catch (Exception e) {
                    log.error("延时队列消费任务失败", e);
                    // 继续处理下一个任务，不中断线程
                }
            }
            log.info("延时队列处理线程结束");
        }, "DelayedQueue-Consumer");

        consumerThread.setDaemon(false);
        consumerThread.start();
    }

    /**
     * 处理延时任务
     */
    private void processDelayedTask(IntentBatchTask task) {
        String intent = task.getIntents();
        List<CompletableFuture<String>> futureList = task.getFutureList();

        try {
            log.info("开始处理延时任务：{}，依赖数量: {}", intent,
                    futureList != null ? futureList.size() : 0);

            // 更新任务状态为处理中
            updateTaskStatus(intent, "DELAYED_PROCESSING");

            // 等待前置任务完成
            if (futureList != null && !futureList.isEmpty()) {
                waitForDependencies(intent, futureList);
            }

            // 执行任务
            executeTask(intent, futureList);

        } catch (Exception e) {
            log.error("延时任务[{}]处理失败", intent, e);
            updateTaskStatus(intent, "DELAYED_ERROR");

            // 更新图状态，即使任务失败也要清理状态
            updateGraphStateOnFailure(intent);

        } finally {
            // 从处理中的任务集合移除
            processingTasks.remove(intent);
            log.debug("任务[{}]已从处理队列中移除，当前处理数量: {}", intent, processingTasks.size());
        }
    }

    /**
     * 等待依赖任务完成
     */
    private void waitForDependencies(String intent, List<CompletableFuture<String>> futureList) {
        try {
            log.info("任务[{}]等待前置依赖完成", intent);

            // 设置合理的超时时间，避免无限等待
            CompletableFuture<Void> allDependencies = CompletableFuture.allOf(
                    futureList.toArray(new CompletableFuture[0])
            );

            allDependencies.get(DEPENDENCY_WAIT_TIMEOUT, TimeUnit.SECONDS);
            log.info("任务[{}]的前置依赖已完成", intent);

        } catch (TimeoutException e) {
            log.error("任务[{}]等待前置依赖超时", intent);
            throw new RuntimeException("等待前置依赖超时", e);
        } catch (ExecutionException e) {
            log.error("任务[{}]的前置依赖执行失败", intent, e);
            throw new RuntimeException("前置依赖执行失败", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("等待前置依赖被中断：{}", intent);
            throw new RuntimeException("等待被中断", e);
        }
    }

    /**
     * 执行具体任务
     */
    private void executeTask(String intent, List<CompletableFuture<String>> futureList) {
        Handler handler = handlerRegiserCilent.getHandler(intent);
        if (handler == null) {
            log.error("未找到对应的Handler：{}", intent);
            updateTaskStatus(intent, "HANDLER_NOT_FOUND");
            throw new RuntimeException("Handler not found: " + intent);
        }

        log.info("获取到的处理器为【{}】", handler);

        // 异步执行任务，设置超时
        CompletableFuture<String> taskFuture = CompletableFuture.supplyAsync(() -> {
            try {
                log.info("延时任务开始执行：{}", intent);
                String result = handler.run(intent, futureList);
                log.info("延时任务执行完成：{}，结果长度: {}", intent,
                        result != null ? result.length() : 0);
                return result;
            } catch (Exception e) {
                log.error("延时任务执行失败：{}", intent, e);
                throw new RuntimeException("任务执行失败: " + e.getMessage(), e);
            }
        }, EXECUTOR_SERVICE);

        // 处理任务完成情况
        handleTaskCompletion(intent, taskFuture, futureList);
    }

    /**
     * 处理任务完成情况 - 关键修复点
     */
    private void handleTaskCompletion(String intent, CompletableFuture<String> taskFuture,
                                      List<CompletableFuture<String>> futureList) {
        taskFuture.whenComplete((result, throwable) -> {
            try {
                if (throwable != null) {
                    log.error("任务[{}]执行异常", intent, throwable);
                    updateTaskStatus(intent, "DELAYED_FAILED");

                    // 即使失败也要更新图状态
                    updateGraphStateOnFailure(intent);

                } else {
                    log.info("任务[{}]执行成功，结果：{}", intent,
                            result != null ? result : "null");

                    // 1. 更新任务状态
                    updateTaskStatus(intent, "DELAYED_SUCCESS");

                    // 2. 将任务结果添加到结果队列
                    addTaskResult(intent, taskFuture, futureList);

                    // 3. 关键修复：更新图状态
                    updateGraphStateOnSuccess(intent);
                }
            } catch (Exception e) {
                log.error("处理任务完成回调时发生异常：{}", intent, e);
            }
        });
    }

    /**
     * 任务成功时更新图状态 - 新增方法
     */
    private void updateGraphStateOnSuccess(String intent) {
        try {
            log.info("延时任务[{}]成功完成，开始更新图状态", intent);

            // 1. 减少依赖任务的入度
            createDiagram.updateInDegree(intent);

            // 2. 发送入度减少的状态动作
            TaskAction decreaseAction = TaskAction.indegreeDecrease(intent);
            statusCilent.push(decreaseAction);

            log.info("延时任务[{}]的图状态更新完成", intent);

        } catch (Exception e) {
            log.error("更新图状态失败，任务: {}", intent, e);
        }
    }

    /**
     * 任务失败时更新图状态
     */
    private void updateGraphStateOnFailure(String intent) {
        try {
            log.warn("延时任务[{}]失败，进行图状态清理", intent);

            // 失败时也要减少入度，避免阻塞后续任务
            // 这里可以根据业务需求决定是否减少入度
            createDiagram.updateInDegree(intent);

            // 发送状态动作
            TaskAction decreaseAction = TaskAction.indegreeDecrease(intent);
            statusCilent.push(decreaseAction);

            log.info("延时任务[{}]失败后的图状态清理完成", intent);

        } catch (Exception e) {
            log.error("失败任务图状态清理失败: {}", intent, e);
        }
    }

    /**
     * 添加任务结果到结果队列
     */
    private void addTaskResult(String intent, CompletableFuture<String> taskFuture,
                               List<CompletableFuture<String>> futureList) {
        try {
            List<CompletableFuture<String>> allResults = new CopyOnWriteArrayList<>();

            // 添加前置任务结果
            if (futureList != null && !futureList.isEmpty()) {
                allResults.addAll(futureList);
            }

            // 添加当前任务结果
            allResults.add(taskFuture);

            // 存储到结果客户端
            resultCilent.addResult(intent, allResults);
            log.info("任务[{}]结果已添加到结果队列，总结果数: {}", intent, allResults.size());

        } catch (Exception e) {
            log.error("添加任务结果时发生异常：{}", intent, e);
        }
    }

    /**
     * 更新任务状态
     */
    private void updateTaskStatus(String intent, String status) {
        try {
            TaskAction action = TaskAction.statusUpdate(intent, status);
            statusCilent.push(action);
            log.debug("任务[{}]状态更新为：{}", intent, status);
        } catch (Exception e) {
            log.error("更新任务状态失败：{} -> {}", intent, status, e);
        }
    }

    /**
     * 获取队列大小
     */
    public int getQueueSize() {
        return intentBatchTaskQueue.size();
    }

    /**
     * 获取正在处理的任务数量
     */
    public int getProcessingTaskCount() {
        return processingTasks.size();
    }

    /**
     * 获取正在处理的任务列表
     */
    public Set<String> getProcessingTasks() {
        return Set.copyOf(processingTasks);
    }

    /**
     * 清除延时任务列表
     */
    public void clearProcessingTasks() {
        intentBatchTaskQueue.clear();
        processingTasks.clear();
        log.info("延时任务队列已清空");
    }

    /**
     * 强制移除指定任务
     */
    public boolean forceRemoveTask(String intent) {
        boolean removed = processingTasks.remove(intent);
        if (removed) {
            log.info("强制移除任务: {}", intent);
        }
        return removed;
    }

    /**
     * 获取延时队列统计信息
     */
    public String getQueueStats() {
        return String.format("队列大小: %d, 处理中任务: %d, 线程池活跃数: %d",
                getQueueSize(),
                getProcessingTaskCount(),
                ((ThreadPoolExecutor) EXECUTOR_SERVICE).getActiveCount());
    }

    @PreDestroy
    public void stop() {
        log.info("开始关闭延时队列客户端");
        running = false;

        // 关闭线程池
        EXECUTOR_SERVICE.shutdown();
        try {
            if (!EXECUTOR_SERVICE.awaitTermination(120, TimeUnit.SECONDS)) {
                log.warn("线程池未在120秒内关闭，强制关闭");
                EXECUTOR_SERVICE.shutdownNow();
                // 再等待一段时间
                if (!EXECUTOR_SERVICE.awaitTermination(60, TimeUnit.SECONDS)) {
                    log.error("线程池强制关闭失败");
                }
            } else {
                log.info("线程池正常关闭");
            }
        } catch (InterruptedException e) {
            log.warn("等待线程池关闭被中断");
            EXECUTOR_SERVICE.shutdownNow();
            Thread.currentThread().interrupt();
        }

        // 清理处理中的任务集合
        int remainingTasks = processingTasks.size();
        processingTasks.clear();

        log.info("延时队列客户端已关闭，剩余队列任务：{}，清理处理中任务：{}",
                intentBatchTaskQueue.size(), remainingTasks);
    }
}