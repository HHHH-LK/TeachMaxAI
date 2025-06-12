package com.aiproject.smartcampus.commons.client;

import com.aiproject.smartcampus.commons.delayedtask.IntentBatchTask;
import com.aiproject.smartcampus.commons.delayedtask.IntentDelayedQueueClien;
import com.aiproject.smartcampus.commons.utils.CreateDiagram;
import com.aiproject.smartcampus.commons.utils.TaskStatusChange;
import com.aiproject.smartcampus.pojo.bo.TaskAction;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;


//状态转换客户端

@Component
@RequiredArgsConstructor
public class StatusCilent {

    private final TaskStatusChange taskStatusChange;
    private final CreateDiagram createDiagram;
    //解决循环依赖问题（不能加final）
    @Lazy
    @Autowired
    private  IntentDelayedQueueClien intentDelayedQueueClien;
    @Lazy
    @Autowired
    private  ResultCilent resultCilent;

    private final BlockingQueue<TaskAction> actionQueue = new LinkedBlockingQueue<>();

    @PostConstruct
    public void start() {
        Thread thread = new Thread(() -> {
            while (true) {
                try {
                    TaskAction action = actionQueue.take();
                    handleTaskAction(action);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    public void push(TaskAction taskAction) {
        actionQueue.offer(taskAction);
    }

    private void handleTaskAction(TaskAction action) {
        String intent = action.getIntent();
        TaskAction.ActionType actionType = action.getActionType();

        switch (actionType) {
            case STATUS_UPDATE:
                // newStatus 是 String 类型
                taskStatusChange.changeStatus(intent, action.getNewStatus());
                break;

            case INDEGREE_DECREASE:
                createDiagram.updateInDegree(intent);
                List<String> readyTasks = createDiagram.getReadyList();
                for (String readyIntent : readyTasks) {
                    List<CompletableFuture<String>> dependencies = resultCilent.getResult(readyIntent);
                    intentDelayedQueueClien.addTask(new IntentBatchTask(readyIntent, dependencies, 200));
                }
                break;
        }
    }
}
