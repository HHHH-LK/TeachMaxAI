package com.aiproject.smartcampus.commons.easyuse;

import com.aiproject.smartcampus.commons.client.ResultCilent;
import com.aiproject.smartcampus.commons.client.StatusCilent;
import com.aiproject.smartcampus.commons.delayedtask.IntentBatchTask;
import com.aiproject.smartcampus.commons.delayedtask.IntentDelayedQueueClien;
import com.aiproject.smartcampus.commons.utils.CreateDiagram;
import com.aiproject.smartcampus.model.intent.handler.Handler;
import com.aiproject.smartcampus.pojo.bo.TaskAction;
import com.github.xiaoymin.knife4j.core.util.StrUtil;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

@Component("intentTaskExecutor")
@Slf4j
@RequiredArgsConstructor
public class TaskExecutor {

    private final IntentDelayedQueueClien delayedQueue;
    private final IntentDispatcher intentDispatcher;
    private final ResultCilent resultCilent;
    private ExecutorService executor = Executors.newFixedThreadPool(20);
    private final StatusCilent statusCilent;

    /**
     * 关键修改：返回CompletableFuture以便调用方等待任务完成
     */
    public CompletableFuture<String> executeAsync(String intent, List<CompletableFuture<String>> futureList) {
        log.info("开始异步执行任务: {}", intent);

        CompletableFuture<String> taskFuture = CompletableFuture.supplyAsync(() -> {
            try {
                log.info("任务 {} 开始执行Handler", intent);

                // 根据intent获取对应的Handler
                Handler handler = intentDispatcher.getHandler(intent);
                if (handler == null) {
                    throw new RuntimeException("未找到对应的Handler: " + intent);
                }
                String result = intentDispatcher.dispatch(handler, intent, futureList);
                log.info("任务 {} 执行完成，结果长度: {}", intent, result != null ? result.length() : 0);
                return result;

            } catch (Exception e) {
                log.error("任务 {} 执行失败", intent, e);
                TaskAction failAction = TaskAction.statusUpdate(intent, "FAILED");
                statusCilent.push(failAction);
                try {
                    // 加入延迟队列重试
                    enqueueDelayed(intent, futureList);
                } catch (Exception ex) {
                    log.error("加入延迟队列失败", ex);
                }
                throw new RuntimeException("任务执行失败: " + intent, e);
            }
        }, executor);
        taskFuture.whenComplete((result, throwable) -> {
            if (throwable == null) {
                // 任务成功完成
                log.info("任务 {} 成功完成，开始保存结果", intent);

                // 保存结果到resultMap
                List<CompletableFuture<String>> allFutures = new ArrayList<>();
                allFutures.add(CompletableFuture.completedFuture(result));
                resultCilent.addResult(intent, allFutures);

                // 标记任务为成功
                TaskAction successAction = TaskAction.statusUpdate(intent, "SUCCESS");
                statusCilent.push(successAction);
                TaskAction decreaseAction = TaskAction.indegreeDecrease(intent);
                statusCilent.push(decreaseAction);
                log.info("任务 {} 的所有后续处理已完成", intent);

            } else {
                // 任务执行失败
                log.error("任务 {} 执行失败", intent, throwable);
                TaskAction failAction = TaskAction.statusUpdate(intent, "FAILED");
                statusCilent.push(failAction);
            }
        });

        return taskFuture;
    }

    public void enqueueDelayed(String intent, List<CompletableFuture<String>> futureList) {
        log.info("将任务 {} 加入延迟队列", intent);
        IntentBatchTask task = new IntentBatchTask();
        task.setIntents(intent);
        task.setDelayTime(System.currentTimeMillis() + 2000);
        task.setFutureList(futureList);
        delayedQueue.addTask(task);
    }

    @PreDestroy
    public void shutdown() {
        log.info("关闭TaskExecutor线程池");
        executor.shutdown();
        try {
            if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}