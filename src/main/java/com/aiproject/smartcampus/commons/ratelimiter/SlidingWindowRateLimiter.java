package com.aiproject.smartcampus.commons.ratelimiter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @program: SmartCampus
 * @description: 滑窗限流算法
 * @author: lk
 * @create: 2025-05-28 14:20
 **/


@Component
@Slf4j
public class SlidingWindowRateLimiter implements RateLimiter {

    //定义窗口
    private Map<Long, AtomicInteger> windowMap = new ConcurrentHashMap<>();
    //定义窗口大小
    private long windowSize;
    //定义最大请求数
    private int maxRequest;
    //定义子桶大小
    private int bucketSize;

    public SlidingWindowRateLimiter builder(long windowSize, int maxRequest, int bucketSize) {
        this.windowSize = windowSize;
        this.maxRequest = maxRequest;
        this.bucketSize = bucketSize;
        return this;
    }

    @Override
    public boolean isAllow() {
        long currentTime = System.currentTimeMillis();
        long bucketInterval = windowSize / bucketSize;
        long bucketId = currentTime / bucketInterval;
        // 加入当前桶
        windowMap.computeIfAbsent(bucketId, k -> new AtomicInteger(0)).incrementAndGet();
        // 清理过期桶
        long earliestBucketId = (currentTime - windowSize) / bucketInterval;
        windowMap.keySet().removeIf(k -> k < earliestBucketId);
        // 累加当前时间窗口内的请求总数
        long totalCount = windowMap.entrySet().stream()
                .filter(e -> e.getKey() >= earliestBucketId)
                .mapToInt(e -> e.getValue().get())
                .sum();

        log.info("当前时间：{}，当前请求数：{}", currentTime, totalCount);

        return totalCount < maxRequest;
    }


}
