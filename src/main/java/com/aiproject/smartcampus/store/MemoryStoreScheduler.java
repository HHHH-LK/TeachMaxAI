package com.aiproject.smartcampus.store;

import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * MemoryStore定时清理任务
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MemoryStoreScheduler {

    private final LocalStore memoryStore;

    /**
     * 每5分钟清理一次过期缓存
     */
    @Scheduled(fixedRate = 300000) // 5分钟
    public void cleanupExpiredCache() {
        try {
            memoryStore.cleanupExpiredCache();
        } catch (Exception e) {
            log.error("Failed to cleanup expired cache", e);
        }
    }

    /**
     * 每30分钟强制写回脏数据
     */
    @Scheduled(fixedRate = 1800000) // 30分钟
    public void flushDirtyData() {
        try {
            memoryStore.flushDirtyData();
        } catch (Exception e) {
            log.error("Failed to flush dirty data", e);
        }
    }

    /**
     * 应用关闭时清理
     */
    @PreDestroy
    public void shutdown() {
        log.info("Shutting down MemoryStore, flushing all data...");
        try {
            memoryStore.flushDirtyData();
        } catch (Exception e) {
            log.error("Failed to flush data during shutdown", e);
        }
    }
}