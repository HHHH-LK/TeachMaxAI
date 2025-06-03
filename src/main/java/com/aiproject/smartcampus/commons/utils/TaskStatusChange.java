package com.aiproject.smartcampus.commons.utils;

import com.aiproject.smartcampus.commons.TaskClient;
import com.aiproject.smartcampus.pojo.bo.TaskAction;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @program: SmartCampus
 * @description: 转变任务状态
 * @author: lk
 * @create: 2025-05-29 18:33
 **/

@Component
@RequiredArgsConstructor
public class TaskStatusChange {

    private final TaskClient taskClient;
    // 这里只存状态字符串，比如 "UNDO", "RUNNING", "DONE"
    private Map<String, String> taskStatusMap = new ConcurrentHashMap<>();

    private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();

    public void init() {
        Map<String, TaskAction> sourceMap = taskClient.getTaskStatusMap();
        if (sourceMap != null) {
            readWriteLock.writeLock().lock();
            try {
                taskStatusMap.clear();
                for (Map.Entry<String, TaskAction> entry : sourceMap.entrySet()) {
                    TaskAction taskAction = entry.getValue();
                    // 这里假设 TaskAction 有 getNewStatus() 或类似方法获取状态字符串
                    // 如果没有，请用对应字段替代
                    taskStatusMap.put(entry.getKey(), taskAction.getNewStatus());
                }
            } finally {
                readWriteLock.writeLock().unlock();
            }
        }
    }

    /**
     * 更新任务状态
     * @param intent 任务标识
     * @param newStatus 新状态字符串
     */
    public void changeStatus(String intent, String newStatus) {
        readWriteLock.writeLock().lock();
        try {
            if (taskStatusMap.containsKey(intent)) {
                taskStatusMap.put(intent, newStatus);
            }
        } finally {
            readWriteLock.writeLock().unlock();
        }
    }

    /**
     * 获取任务状态
     * @param intent 任务标识
     * @return 状态字符串，如果没有则返回 null
     */
    public String getStatus(String intent) {
        readWriteLock.readLock().lock();
        try {
            return taskStatusMap.getOrDefault(intent, null);
        } finally {
            readWriteLock.readLock().unlock();
        }
    }

    /**
     * 删除任务状态
     * @param intent 任务标识
     */
    public void deleteStatus(String intent) {
        readWriteLock.writeLock().lock();
        try {
            taskStatusMap.remove(intent);
        } finally {
            readWriteLock.writeLock().unlock();
        }
    }
}
