package com.aiproject.smartcampus.commons.utils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserOnlineClients {

    private static final Map<String, SseEmitter> USER_EMITTERS = new ConcurrentHashMap<>();
    private final StringRedisTemplate stringRedisTemplate;

    // Redis key前缀
    private static final String ONLINE_KEY_PREFIX = "user:online:";
    private static final String SSE_NODE_KEY_PREFIX = "user:sse:node:";

    // 获取当前服务器节点标识（可以是IP+端口或其他唯一标识）
    private String getNodeId() {
        // 这里简单使用时间戳+随机数，实际应该使用服务器IP+端口
        return System.getProperty("server.port", "8080") + "_" + System.currentTimeMillis();
    }

    public void addUserEmitter(String userId, SseEmitter emitter) {
        log.info("用户 {} 建立SSE连接", userId);

        // 1. 存储到内存
        USER_EMITTERS.put(userId, emitter);

        // 2. 在Redis中记录用户在线状态
        addUserOnlineToRedis(userId);

        // 3. 记录SSE连接所在的节点（用于多实例部署）
        String nodeId = getNodeId();
        stringRedisTemplate.opsForValue().set(
                SSE_NODE_KEY_PREFIX + userId,
                nodeId,
                3,
                TimeUnit.HOURS
        );
    }

    public void removeUserEmitter(String userId) {
        log.info("移除用户 {} 的SSE连接", userId);

        // 1. 从内存中移除
        SseEmitter emitter = USER_EMITTERS.remove(userId);
        if (emitter != null) {
            try {
                emitter.complete();
            } catch (Exception e) {
                log.debug("关闭SSE连接时出现异常", e);
            }
        }

        // 2. 从Redis中移除
        removeUserOnlineFromRedis(userId);

        // 3. 移除SSE节点信息
        stringRedisTemplate.delete(SSE_NODE_KEY_PREFIX + userId);
    }

    public static SseEmitter getUserEmitter(String userId) {
        return USER_EMITTERS.get(userId);
    }

    public static Integer getUserEmitterCount() {
        return USER_EMITTERS.size();
    }

    public boolean isUserOnline(String userId) {
        // 检查Redis中的在线状态
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(ONLINE_KEY_PREFIX + userId));
    }

    public boolean hasLocalSseConnection(String userId) {
        // 检查本地是否有SSE连接
        return USER_EMITTERS.containsKey(userId);
    }

    public void addUserOnlineToRedis(String userId) {
        stringRedisTemplate.opsForValue().set(
                ONLINE_KEY_PREFIX + userId,
                userId + ":online",
                3,
                TimeUnit.HOURS
        );
        log.info("用户 {} 在线状态已记录到Redis", userId);
    }

    public void removeUserOnlineFromRedis(String userId) {
        Boolean deleted = stringRedisTemplate.delete(ONLINE_KEY_PREFIX + userId);
        if (Boolean.TRUE.equals(deleted)) {
            log.info("用户 {} 在线状态已从Redis移除", userId);
        } else {
            log.warn("用户 {} 在线状态移除失败或不存在", userId);
        }
    }

    // 刷新用户在线状态的过期时间
    public void refreshUserOnlineStatus(String userId) {
        if (isUserOnline(userId)) {
            stringRedisTemplate.expire(ONLINE_KEY_PREFIX + userId, 3, TimeUnit.HOURS);
            stringRedisTemplate.expire(SSE_NODE_KEY_PREFIX + userId, 3, TimeUnit.HOURS);
        }
    }

    // 清理所有无效的SSE连接
    public void cleanupInvalidConnections() {
        USER_EMITTERS.entrySet().removeIf(entry -> {
            String userId = entry.getKey();
            SseEmitter emitter = entry.getValue();

            try {
                // 发送心跳测试连接是否有效
                emitter.send(SseEmitter.event()
                        .name("heartbeat")
                        .data("ping"));
                return false;
            } catch (Exception e) {
                log.info("清理无效的SSE连接: 用户 {}", userId);
                removeUserOnlineFromRedis(userId);
                return true;
            }
        });
    }
}