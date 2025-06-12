package com.aiproject.smartcampus.commons.websocket;

import com.aiproject.smartcampus.commons.client.NotificateClient;
import com.aiproject.smartcampus.pojo.bo.NotificationMessage;
import com.aiproject.smartcampus.pojo.po.Notificate;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class NotificationWebSocketHandler implements WebSocketHandler {

    private static final ConcurrentHashMap<String, WebSocketSession> sessionMap = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(5);

    @Autowired
    private NotificateClient notificateClient;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String userId = getUserIdFromSession(session);
        if (userId != null) {
            sessionMap.put(userId, session);
            log.info("用户 {} 建立WebSocket连接", userId);
            startNotificationPushTask(userId);
        } else {
            log.warn("无法获取用户ID，关闭连接");
            session.close();
        }
    }

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        String userId = getUserIdFromSession(session);
        log.info("收到用户 {} 的消息: {}", userId, message.getPayload());
        if ("ping".equals(message.getPayload().toString())) {
            session.sendMessage(new TextMessage("pong"));
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        String userId = getUserIdFromSession(session);
        log.error("用户 {} WebSocket连接出现错误", userId, exception);
        cleanupSession(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
        String userId = getUserIdFromSession(session);
        log.info("用户 {} WebSocket连接关闭，状态: {}", userId, closeStatus);
        cleanupSession(session);
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }

    private String getUserIdFromSession(WebSocketSession session) {
        String query = session.getUri().getQuery();
        if (query != null && query.startsWith("userId=")) {
            return query.substring(7);
        }
        return null;
    }

    private void startNotificationPushTask(String userId) {
        scheduler.scheduleWithFixedDelay(() -> {
            WebSocketSession session = sessionMap.get(userId);
            if (session != null && session.isOpen()) {
                try {
                    Notificate notificate = notificateClient.getNotificate(userId);
                    if (notificate != null) {
                        NotificationMessage message = NotificationMessage.builder()
                                .id(notificate.getId())
                                .content(notificate.getContent())
                                .type(String.valueOf(notificate.getType()))
                                .createTime(notificate.getCreateTime())
                                .receiverId(notificate.getReceiverId())
                                .build();
                        String jsonMessage = objectMapper.writeValueAsString(message);
                        session.sendMessage(new TextMessage(jsonMessage));
                        log.info("向用户 {} 推送通知: {}", userId, message.getContent());
                    }
                } catch (Exception e) {
                    log.error("向用户 {} 推送通知失败", userId, e);
                    sessionMap.remove(userId);
                }
            } else {
                sessionMap.remove(userId);
                log.info("用户 {} 会话已关闭，停止推送任务", userId);
            }
        }, 1, 2, TimeUnit.SECONDS);
    }

    private void cleanupSession(WebSocketSession session) {
        String userId = getUserIdFromSession(session);
        if (userId != null) {
            sessionMap.remove(userId);
        }
    }

    public static void sendNotificationToUser(String userId, NotificationMessage message) {
        WebSocketSession session = sessionMap.get(userId);
        if (session != null && session.isOpen()) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                String jsonMessage = mapper.writeValueAsString(message);
                session.sendMessage(new TextMessage(jsonMessage));
                log.info("主动向用户 {} 发送通知: {}", userId, message.getContent());
            } catch (IOException e) {
                log.error("向用户 {} 发送通知失败", userId, e);
                sessionMap.remove(userId);
            }
        }
    }

    public static void broadcastNotification(NotificationMessage message) {
        sessionMap.forEach((userId, session) -> {
            if (session.isOpen()) {
                try {
                    ObjectMapper mapper = new ObjectMapper();
                    String jsonMessage = mapper.writeValueAsString(message);
                    session.sendMessage(new TextMessage(jsonMessage));
                    log.info("向用户 {} 广播通知: {}", userId, message.getContent());
                } catch (IOException e) {
                    log.error("向用户 {} 广播通知失败", userId, e);
                    sessionMap.remove(userId);
                }
            }
        });
    }
}