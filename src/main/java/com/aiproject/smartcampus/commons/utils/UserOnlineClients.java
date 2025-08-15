package com.aiproject.smartcampus.commons.utils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class UserOnlineClients {

    private static final Map<String, SseEmitter> USER_EMITTERS = new ConcurrentHashMap<>();
    private static final Logger log = LoggerFactory.getLogger(UserOnlineClients.class);
    private final StringRedisTemplate stringRedisTemplate;

    public static void addUserEmitter(String userId, SseEmitter emitter) {
        log.error("用户 {} 已登陆", userId);
        USER_EMITTERS.put(userId, emitter);
    }

    public static void removeUserEmitter(String userId) {
        USER_EMITTERS.remove(userId);
    }

    public static SseEmitter getUserEmitter(String userId) {
        return USER_EMITTERS.get(userId);
    }

    public static Integer getUserEmitterCount() {
        return USER_EMITTERS.size();
    }

    public static boolean isContainsUserId(StringRedisTemplate stringRedisTemplate, String userId) {

        return stringRedisTemplate.hasKey("user:online:" + userId);
    }

    public void addUserOnlineToRedis(String userId) {

        stringRedisTemplate.opsForValue().set("user:online:" + userId, userId + ":online", 3, TimeUnit.HOURS);

    }

    public void removeUserOnlineFromRedis(String userId) {

        Boolean delete = stringRedisTemplate.delete("user:online:" + userId);
        if (!delete) {
            log.error("user: {}  退出失败", userId);
            throw new RuntimeException("退出登录失败");
        }
    }

}
