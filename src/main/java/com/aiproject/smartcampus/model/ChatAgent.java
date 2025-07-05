package com.aiproject.smartcampus.model;

import com.aiproject.smartcampus.commons.utils.UserLocalThreadUtils;
import com.aiproject.smartcampus.model.intent.Intent;

import com.aiproject.smartcampus.model.store.LocalStore;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.Arrays;
import java.util.List;

/**
 * @program: SmartCampus
 * @description: 智能体实现，支持会话记忆存储
 * @author: lk
 * @create: 2025-05-27 23:05
 **/

@Component
@RequiredArgsConstructor
@Slf4j
public class ChatAgent {

    private final Intent intentHandler;
    private final LocalStore localStore;

    // 注入意图处理器
    public String start(String userMessage) {

        UserLocalThreadUtils.setUserMemory(userMessage);

        // 处理用户意图
        String answer = intentHandler.handlerIntent(userMessage);

        // 存储会话记录
        storeConversation(userMessage, answer);

        return answer;
    }

    /**
     * 存储会话记录到本地存储
     */
    private void storeConversation(String userMessage, String aiResponse) {
        try {
            // 获取当前用户ID作为存储键
           // String userId = UserLocalThreadUtils.getUserInfo().getUserId().toString();
            //todo 测试编写2
            String userId = "1";

            if (userId == null) {
                log.warn("用户ID为空，无法存储会话记录");
                return;
            }

            // 创建用户消息和AI回复消息
            List<ChatMessage> newMessages = Arrays.asList(
                    UserMessage.from(userMessage),
                    AiMessage.from(aiResponse)
            );

            // 更新存储
            localStore.updateMessages(userId, newMessages);

            log.debug("会话记录已存储，userId: {}, 消息数: {}", userId, newMessages.size());

        } catch (Exception e) {
            log.error("存储会话记录失败，userMessage: {}", userMessage, e);
        }
    }
}