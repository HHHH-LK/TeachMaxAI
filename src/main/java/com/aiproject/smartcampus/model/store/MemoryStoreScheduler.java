package com.aiproject.smartcampus.model.store;

import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * MemoryStore快速同步调度器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MemoryStoreScheduler {

    private final LocalStore memoryStore;

    /**
     * 每30秒清理一次过期缓存（提高频率）
     */
    @Scheduled(fixedRate = 30000) // 30秒
    public void cleanupExpiredCache() {
        try {
            memoryStore.cleanupExpiredCache();
        } catch (Exception e) {
            log.error("清理过期缓存失败", e);
        }
    }

    /**
     * 每2分钟强制写回脏数据（大幅提高频率）
     */
    @Scheduled(fixedRate = 1000) // 2分钟
    public void flushDirtyData() {
        try {
            memoryStore.flushDirtyData();
        } catch (Exception e) {
            log.error("强制写回脏数据失败", e);
        }
    }

    /**
     * 每1分钟检查Redis健康状态
     */
    @Scheduled(fixedRate = 60000) // 1分钟
    public void checkRedisHealth() {
        try {
            memoryStore.forceRedisHealthCheck();
        } catch (Exception e) {
            log.error("Redis健康检查失败", e);
        }
    }

    /**
     * 每5分钟输出缓存统计信息
     */
    @Scheduled(fixedRate = 300000) // 5分钟
    public void logCacheStats() {
        try {
            Map<String, Object> stats = memoryStore.getCacheStats();
            log.info("缓存统计 - 大小: {}/{}, Redis状态: {}, 脏数据: {}, 过期数据: {}",
                    stats.get("cacheSize"),
                    stats.get("maxCacheSize"),
                    stats.get("redisAvailable"),
                    stats.get("dirtyEntries"),
                    stats.get("expiredEntries"));
        } catch (Exception e) {
            log.error("获取缓存统计失败", e);
        }
    }

    /**
     * 应用关闭时清理
     */
    @PreDestroy
    public void shutdown() {
        log.info("关闭MemoryStore，正在写回所有数据...");
        try {
            // 强制写回所有脏数据
            memoryStore.flushDirtyData();

            // 等待一点时间确保异步操作完成
            Thread.sleep(1000);

            log.info("MemoryStore关闭完成");
        } catch (Exception e) {
            log.error("关闭时写回数据失败", e);
        }
    }
}