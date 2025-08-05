package com.aiproject.smartcampus.commons.utils;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class UserOnlineClients {

    private static final Map<String, SseEmitter> USER_EMITTERS = new ConcurrentHashMap<>();
    private static final Logger log = LoggerFactory.getLogger(UserOnlineClients.class);

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
        return  USER_EMITTERS.size();
    }

    public static boolean isContainsUserId(String userId) {
        return USER_EMITTERS.containsKey(userId);
    }

}
