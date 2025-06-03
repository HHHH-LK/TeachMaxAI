package com.aiproject.smartcampus.pojo.bo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskAction {

    public enum ActionType {
        STATUS_UPDATE,
        INDEGREE_DECREASE
    }

    private String intent;  // 任务标识

    private ActionType actionType; // 事件类型

    // 任务状态更新时使用，改成 String 表示状态名（比如 "RUNNING", "DONE"）
    private String newStatus;

    // 入度减少时用到的依赖结果
    private List<CompletableFuture<String>> dependencies;

    // 方便构造不同类型事件
    public static TaskAction statusUpdate(String intent, String newStatus) {
        return new TaskAction(intent, ActionType.STATUS_UPDATE, newStatus, null);
    }

    public static TaskAction indegreeDecrease(String intent) {
        return new TaskAction(intent, ActionType.INDEGREE_DECREASE, null, null);
    }

    public static TaskAction indegreeDecreaseWithDeps(String intent, List<CompletableFuture<String>> dependencies) {
        return new TaskAction(intent, ActionType.INDEGREE_DECREASE, null, dependencies);
    }
}

