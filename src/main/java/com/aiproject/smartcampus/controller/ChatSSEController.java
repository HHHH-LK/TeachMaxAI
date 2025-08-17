package com.aiproject.smartcampus.controller;

import com.aiproject.smartcampus.commons.client.Result;
import com.aiproject.smartcampus.commons.utils.UserOnlineClients;
import com.aiproject.smartcampus.pojo.dto.ChatStreamMessage;
import com.aiproject.smartcampus.pojo.dto.ChatMessagePushDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/chat")
@Slf4j
@RequiredArgsConstructor
public class ChatSSEController {

    private final UserOnlineClients userOnlineClients;
    private final ScheduledExecutorService heartbeatExecutor = Executors.newScheduledThreadPool(1);

    /**
     * 建立SSE连接
     */
    @GetMapping(value = "/events/{userId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamEvents(@PathVariable String userId) {
        // 创建SSE连接，设置30分钟超时
        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L);

        // 先检查是否已有连接，如果有则先关闭
        SseEmitter existingEmitter = UserOnlineClients.getUserEmitter(userId);
        if (existingEmitter != null) {
            log.info("用户 {} 已有SSE连接，先关闭旧连接", userId);
            userOnlineClients.removeUserEmitter(userId);
        }

        // 添加新连接
        userOnlineClients.addUserEmitter(userId, emitter);

        // 设置连接关闭时的处理
        emitter.onCompletion(() -> {
            log.info("用户 {} SSE连接正常关闭", userId);
            userOnlineClients.removeUserEmitter(userId);
        });

        emitter.onTimeout(() -> {
            log.info("用户 {} SSE连接超时", userId);
            userOnlineClients.removeUserEmitter(userId);
        });

        emitter.onError((e) -> {
            log.error("用户 {} SSE连接异常: {}", userId, e.getMessage());
            userOnlineClients.removeUserEmitter(userId);
        });

        // 发送连接成功消息
        try {
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data("连接成功")
                    .id(String.valueOf(System.currentTimeMillis())));

            // 启动心跳保持连接
            startHeartbeat(userId, emitter);

        } catch (IOException e) {
            log.error("发送连接确认失败", e);
            userOnlineClients.removeUserEmitter(userId);
            throw new RuntimeException("建立SSE连接失败");
        }

        log.info("用户 {} SSE连接建立成功", userId);
        return emitter;
    }

    /**
     * 启动心跳机制
     */
    private void startHeartbeat(String userId, SseEmitter emitter) {
        heartbeatExecutor.scheduleWithFixedDelay(() -> {
            try {
                if (UserOnlineClients.getUserEmitter(userId) == emitter) {
                    emitter.send(SseEmitter.event()
                            .name("heartbeat")
                            .data("ping")
                            .id(String.valueOf(System.currentTimeMillis())));

                    // 刷新Redis中的在线状态
                    userOnlineClients.refreshUserOnlineStatus(userId);
                }
            } catch (Exception e) {
                log.debug("心跳发送失败，移除用户 {} 的连接", userId);
                userOnlineClients.removeUserEmitter(userId);
            }
        }, 30, 30, TimeUnit.SECONDS);
    }

    /**
     * 推送AI流式消息给指定用户
     */
    public void pushStreamMessageToUser(String userId, ChatStreamMessage message) {
        if (!userOnlineClients.hasLocalSseConnection(userId)) {
            log.debug("用户 {} 在本节点无SSE连接", userId);
            return;
        }

        SseEmitter emitter = UserOnlineClients.getUserEmitter(userId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event()
                        .name("ai_stream_message")
                        .data(message)
                        .id(String.valueOf(System.currentTimeMillis())));
                log.debug("SSE推送AI流式消息成功 - 用户: {}", userId);
            } catch (Exception e) {
                log.error("SSE推送AI流式消息失败 - 用户: {}", userId, e);
                userOnlineClients.removeUserEmitter(userId);
            }
        }
    }

    /**
     * 推送师生聊天消息给指定用户
     */
    public void pushMessageToUser(String userId, ChatMessagePushDto message) {
        log.info("🔄 尝试推送消息给用户: {}", userId);

        // 先检查用户是否在线（Redis中的状态）
        if (!userOnlineClients.isUserOnline(userId)) {
            log.info("📴 用户 {} 不在线，消息已存储在数据库中", userId);
            return;
        }

        // 检查本地是否有SSE连接
        if (!userOnlineClients.hasLocalSseConnection(userId)) {
            log.debug("用户 {} 在线但SSE连接不在本节点", userId);
            // 如果是多实例部署，这里可以通过消息队列转发到正确的节点
            return;
        }

        SseEmitter emitter = UserOnlineClients.getUserEmitter(userId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event()
                        .name("chat_message")
                        .data(message)
                        .id(String.valueOf(System.currentTimeMillis()))
                        .reconnectTime(3000L)); // 设置重连时间

                log.info("✅ 实时推送成功 - 用户: {}, 消息ID: {}", userId, message.getMessageId());
            } catch (Exception e) {
                log.error("❌ 实时推送失败 - 用户: {}", userId, e);
                userOnlineClients.removeUserEmitter(userId);
            }
        }
    }

    /**
     * 获取在线用户数
     */
    @GetMapping("/online-count")
    public Result<Integer> getOnlineCount() {
        return Result.success(UserOnlineClients.getUserEmitterCount());
    }

    /**
     * 检查用户是否在线
     */
    @GetMapping("/is-online/{userId}")
    public Result<Boolean> isUserOnline(@PathVariable String userId) {
        boolean isOnline = userOnlineClients.isUserOnline(userId);
        log.info("用户 {} 在线状态: {}", userId, isOnline ? "在线" : "离线");
        return Result.success(isOnline);
    }

    /**
     * 定期清理无效连接
     */
    @Scheduled(fixedDelay = 60000) // 每分钟执行一次
    public void cleanupConnections() {
        log.debug("开始清理无效的SSE连接");
        userOnlineClients.cleanupInvalidConnections();
    }
}