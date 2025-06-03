package com.aiproject.smartcampus.commons.easyuse;

import com.aiproject.smartcampus.commons.ResultCilent;
import com.aiproject.smartcampus.commons.StatusCilent;
import com.aiproject.smartcampus.commons.delayedtask.IntentBatchTask;
import com.aiproject.smartcampus.commons.delayedtask.IntentDelayedQueueClien;
import com.aiproject.smartcampus.commons.utils.CreateDiagram;
import com.aiproject.smartcampus.commons.utils.TaskStatusChange;
import com.aiproject.smartcampus.model.intent.handler.Handler;
import com.aiproject.smartcampus.pojo.bo.TaskAction;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * @program: SmartCampus
 * @description: 业务处理，并发执行
 * @author: lk
 * @create: 2025-05-28 20:45
 **/

@Component("intentTaskExecutor")
@Slf4j
@RequiredArgsConstructor
public class TaskExecutor {

    private final IntentDelayedQueueClien delayedQueue;
    private final IntentDispatcher intentDispatcher;
    private final ResultCilent resultCilent;
    private ExecutorService executor = Executors.newFixedThreadPool(20);
    private final StatusCilent statusCilent;

    public void executeAsync(String intent, List<CompletableFuture<String>> futureList) {
        // 创建一个新的CompletableFuture，异步执行任务
        CompletableFuture<String> submit = CompletableFuture.supplyAsync(() -> {
            try {
                // 根据intent获取对应的Handler
                Handler handler = intentDispatcher.getHandler(intent);
                if (handler == null) {
                    throw new RuntimeException("未找到对应的Handler: " + intent);
                }
                return intentDispatcher.dispatch(handler, intent, futureList);
            } catch (Exception e) {
                log.error("任务执行失败", e);
                try {
                    // 加入延迟队列
                    enqueueDelayed(intent, futureList);
                } catch (Exception ex) {
                    throw new RuntimeException("任务执行失败", ex);
                }
                throw new RuntimeException("任务执行失败", e);
            }
        }, executor);

        //将结果存入结果队列中
        List<CompletableFuture<String>> allFutures = new ArrayList<>();
        if (futureList != null) {
            allFutures.addAll(futureList);
        }
        allFutures.add(submit);
        resultCilent.addResult(intent, allFutures);
        TaskAction action = TaskAction.statusUpdate(intent, "SUCCESS");
        statusCilent.push(action);

    }

    public void enqueueDelayed(String intent, List<CompletableFuture<String>> futureList) throws Exception {
        //todo 对其进行依赖控制处理
        IntentBatchTask task = new IntentBatchTask();
        task.setIntents(intent);
        task.setDelayTime(System.currentTimeMillis() + 2000);
        task.setFutureList(futureList);
        delayedQueue.addTask(task);
    }

    //最后释放线程池
    @PreDestroy
    public void shutdown() {
        executor.shutdown();
    }


}
