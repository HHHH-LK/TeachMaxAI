package com.aiproject.smartcampus.commons.easyuse;

import com.aiproject.smartcampus.commons.ratelimiter.SlidingWindowRateLimiter;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @program: SmartCampus
 * @description: 限流处理
 * @author: lk
 * @create: 2025-05-28 23:35
 **/


@Component
@RequiredArgsConstructor
@Slf4j
public class RateLimiterWrapper {

    private final SlidingWindowRateLimiter rateLimiter;

    @PostConstruct
    public void init() {
        rateLimiter.builder(100L, 10, 10);
    }

    public boolean isAllow(String intent) {
        boolean allowed = rateLimiter.isAllow();
        if (!allowed) {
            log.warn("限流拒绝了 Intent: {},将放入延时队列中执行", intent);
        }
        return allowed;
    }
}
