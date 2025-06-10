package com.aiproject.smartcampus.commons.utils;

import com.aiproject.smartcampus.commons.TaskClient;
import com.aiproject.smartcampus.commons.delayedtask.IntentDelayedQueueClien;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;

/**
 * @program: SmartCampus
 * @description: 任务清除工具
 * @author: lk_hhh
 * @create: 2025-06-10 10:07
 **/
@Component
@RequiredArgsConstructor
@Slf4j
public class TaskClearUtils {

    private final CreateDiagram createDiagram;
    private final TaskStatusChange taskStatusChange;
    private final TaskClient taskClient;
    private final IntentDelayedQueueClien intentDelayedQueueClien;
    private  ReadWriteLock readWriteLock;

    private void  clear(){
        log.info("开始执行清除任务工作");
        Lock lock = readWriteLock.writeLock();
        try {
            lock.lock();
            createDiagram.clear();
            intentDelayedQueueClien.clearProcessingTasks();
            taskClient.deleteAllTask();
            taskStatusChange.deleteAllStatus();
            log.info("清除成功");
        } catch (Exception e) {
            log.error("执行清除任务失败");
        } finally {
            lock.unlock();
        }

    }


}