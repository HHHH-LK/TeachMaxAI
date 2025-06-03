package com.aiproject.smartcampus.commons.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class TaskMonitor {
    
    private final Map<String, Long> taskStartTime = new ConcurrentHashMap<>();
    private final Map<String, Integer> retryCount = new ConcurrentHashMap<>();
    
    public void recordTaskStart(String intent) {
        taskStartTime.put(intent, System.currentTimeMillis());
    }
    
    public void recordTaskEnd(String intent) {
        Long startTime = taskStartTime.remove(intent);
        if (startTime != null) {
            long duration = System.currentTimeMillis() - startTime;
            log.info("任务 {} 执行耗时: {}ms", intent, duration);
        }
    }
    
    public boolean shouldRetry(String intent, int maxRetries) {
        int count = retryCount.getOrDefault(intent, 0);
        if (count < maxRetries) {
            retryCount.put(intent, count + 1);
            return true;
        }
        return false;
    }
}