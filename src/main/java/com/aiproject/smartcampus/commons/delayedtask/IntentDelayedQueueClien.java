package com.aiproject.smartcampus.commons.delayedtask;

import com.aiproject.smartcampus.commons.HandlerRegiserCilent;
import com.aiproject.smartcampus.commons.ResultCilent;
import com.aiproject.smartcampus.commons.StatusCilent;
import com.aiproject.smartcampus.commons.TaskClient;
import com.aiproject.smartcampus.model.intent.handler.Handler;
import com.aiproject.smartcampus.pojo.bo.TaskAction;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.*;

/**
 * @program: SmartCampus
 * @description: 延时队列客户端
 * @author: lk
 * @create: 2025-05-28 15:37
 **/

@Component
@RequiredArgsConstructor
@Slf4j
public class IntentDelayedQueueClien {

    private final HandlerRegiserCilent handlerRegiserCilent;
    private final DelayQueue<IntentBatchTask> intentBatchTaskQueue = new DelayQueue<>();
    private final ExecutorService EXECUTOR_SERVICE = Executors.newFixedThreadPool(20);
    private volatile boolean running = true;
    private final StatusCilent statusCilent;
    private final ResultCilent resultCilent;

    public void addTask(IntentBatchTask intentBatchTask) {
        intentBatchTaskQueue.add(intentBatchTask);
        log.info("延时任务已添加：{}", intentBatchTask.getIntents());
    }

    @PostConstruct
    public void start() {
        new Thread(() -> {
            log.info("延时队列处理线程启动");
            while (running && !Thread.currentThread().isInterrupted()) {
                try {
                    IntentBatchTask task = intentBatchTaskQueue.take();
                    String intent = task.getIntents();
                    if (task != null) {
                        List<CompletableFuture<String>> futureList = task.getFutureList();
                        if (futureList == null) {
                        log.warn("{}延时任务未找到对应的前置条件", intent);
                        }
                        //等待前置任务执行完
                        CompletableFuture.allOf(futureList.toArray(new CompletableFuture[0])).join();
                        //执行任务处理
                        Handler handler = handlerRegiserCilent.getHandler(intent);
                        if (handler != null) {
                            CompletableFuture<String> submit = CompletableFuture.supplyAsync(() -> {
                                try {
                                    log.info("延时任务开始执行：{}", intent);
                                    //将处理结果加入结果队列
                                    return handler.run(intent, futureList);
                                } catch (Exception e) {
                                    log.error("延时任务执行失败：{}", intent, e);
                                    TaskAction action = TaskAction.statusUpdate(intent, "ERROR");
                                    statusCilent.push(action);
                                    throw e;
                                }
                                },EXECUTOR_SERVICE);
                            //将结果加入结果队列中
                            List<CompletableFuture<String>> futures = new CopyOnWriteArrayList<>();
                            if(futureList!= null){
                                futures.addAll(futureList);
                            }
                            futures.add(submit);
                            resultCilent.addResult(intent,futures );
                            TaskAction action = TaskAction.statusUpdate(intent, "SUCCESS");
                            statusCilent.push(action);
                        } else {
                            log.error("未找到对应的Handler：{}", intent);
                            TaskAction action = TaskAction.statusUpdate(intent, "ERROR");
                            statusCilent.push(action);
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.info("延时队列处理线程被中断");
                    break;
                } catch (Exception e) {
                    log.error("延时队列消费任务失败", e);
                }
            }
        }, "DelayedQueue-Consumer").start();
    }

    @PreDestroy
    public void stop() {
        running = false;
        EXECUTOR_SERVICE.shutdown();
        try {
            if (!EXECUTOR_SERVICE.awaitTermination(60, TimeUnit.SECONDS)) {
                EXECUTOR_SERVICE.shutdownNow();
            }
        } catch (InterruptedException e) {
            EXECUTOR_SERVICE.shutdownNow();
            Thread.currentThread().interrupt();
        }
        log.info("延时队列客户端已关闭");
    }
}
