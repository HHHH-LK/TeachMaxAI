package com.aiproject.smartcampus.commons.utils;

import com.aiproject.smartcampus.controller.ChatSSEController;
import com.aiproject.smartcampus.pojo.dto.ChatStreamMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class StreamingOutputUtils {

    @Autowired
    private ChatSSEController chatSseController;

    /**
     * 推送流式消息
     */
    public void pushStreamMessage(String userId, String content , String messageType, Boolean isComplete) {
        if (userId == null || userId.trim().isEmpty()) {
            log.warn("用户ID为空，跳过流式输出");
            return;
        }

        try {
            ChatStreamMessage chatStreamMessage = new ChatStreamMessage();
            chatStreamMessage.setUserId(userId);
            chatStreamMessage.setContent(content);
            chatStreamMessage.setMessageType(messageType);
            chatStreamMessage.setIsComplete(isComplete);
            chatStreamMessage.setTimestamp(System.currentTimeMillis());
            chatSseController.pushStreamMessageToUser(userId, chatStreamMessage);
            log.debug("推送流式消息成功 - 任务: {}, 用户: {}, 完成: {}", messageType, userId, isComplete);

        } catch (Exception e) {
            log.error("推送流式消息失败 - 任务: {}, 用户: {}", messageType, userId, e);
        }
    }


}