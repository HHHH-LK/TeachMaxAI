package com.aiproject.smartcampus.controller;

import com.aiproject.smartcampus.commons.client.Result;
import com.aiproject.smartcampus.pojo.dto.ChatStreamMessage;
import com.aiproject.smartcampus.pojo.dto.ChatMessagePushDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/chat")
@Slf4j
public class ChatSSEController {

    // 存储用户的SSE连接
    private static final Map<String, SseEmitter> USER_EMITTERS = new ConcurrentHashMap<>();

    /**
     * 建立SSE连接
     */
    @GetMapping(value = "/events/{userId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamEvents(@PathVariable String userId) {
        SseEmitter emitter = new SseEmitter(0L); // 永不超时

        USER_EMITTERS.put(userId, emitter);

        emitter.onCompletion(() -> {
            USER_EMITTERS.remove(userId);
            log.info("用户 {} SSE连接关闭", userId);
        });

        emitter.onTimeout(() -> {
            USER_EMITTERS.remove(userId);
            log.info("用户 {} SSE连接超时", userId);
        });

        emitter.onError((e) -> {
            USER_EMITTERS.remove(userId);
            log.error("用户 {} SSE连接异常", userId, e);
        });

        log.info("用户 {} 建立SSE连接", userId);

        // 发送连接成功消息
        try {
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data("连接成功"));
        } catch (IOException e) {
            log.error("发送连接确认失败", e);
            USER_EMITTERS.remove(userId);
        }

        return emitter;
    }

    /**
     * 推送AI流式消息给指定用户（用于AI聊天流式输出）
     */
    public void pushStreamMessageToUser(String userId, ChatStreamMessage message) {
        SseEmitter emitter = USER_EMITTERS.get(userId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event()
                        .name("ai_stream_message")
                        .data(message));
                log.info("SSE推送AI流式消息成功 - 用户: {}", userId);
            } catch (IOException e) {
                log.error("SSE推送AI流式消息失败 - 用户: {}", userId, e);
                USER_EMITTERS.remove(userId);
            }
        } else {
            log.debug("用户 {} 不在线，无法推送AI流式消息", userId);
        }
    }

    // 师生聊天不需要离线消息队列，因为：

    public void pushMessageToUser(String userId, ChatMessagePushDto message) {
        SseEmitter emitter = USER_EMITTERS.get(userId);
        if (emitter != null) {
            // 在线用户：实时推送
            try {
                emitter.send(SseEmitter.event()
                        .name("chat_message")
                        .data(message));
                log.info("实时推送成功 - 用户: {}", userId);
            } catch (IOException e) {
                log.error("实时推送失败 - 用户: {}", userId, e);
                USER_EMITTERS.remove(userId);
            }
        } else {
            // 离线用户：不做处理，因为消息已在数据库中
            log.debug("用户 {} 离线，消息已存储在数据库中，等待用户上线时主动拉取", userId);
        }
    }

    /**
     * 获取在线用户数
     */
    @GetMapping("/online-count")
    public Result<Integer> getOnlineCount() {
        return Result.success(USER_EMITTERS.size());
    }

    /**
     * 检查用户是否在线
     */
    @GetMapping("/is-online/{userId}")
    public Result<Boolean> isUserOnline(@PathVariable String userId) {
        boolean isOnline = USER_EMITTERS.containsKey(userId);
        return Result.success(isOnline);
    }
}