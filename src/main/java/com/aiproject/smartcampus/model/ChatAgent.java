package com.aiproject.smartcampus.model;

import com.aiproject.smartcampus.commons.utils.UserLocalThreadUtils;
import com.aiproject.smartcampus.model.intent.Intent;
import com.aiproject.smartcampus.model.store.UnifiedMemoryManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 优化后的智能体实现，使用统一记忆管理
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ChatAgent {

    private final Intent intentHandler;
    private final UnifiedMemoryManager memoryManager;

    /**
     * 智能体入口方法
     */
    public String start(String userMessage) {
        String userId = getCurrentUserId();

        try {
            // 设置用户上下文
            UserLocalThreadUtils.setUserMemory(userMessage);

            // 处理用户意图
            String answer = intentHandler.handlerIntent(userMessage);

            // 统一存储会话记录
            memoryManager.storeConversation(userId, userMessage, answer);

            log.debug("智能体处理完成，userId: {}, 输入长度: {}, 输出长度: {}",
                    userId, userMessage.length(), answer.length());

            return answer;

        } catch (Exception e) {
            log.error("智能体处理失败，userId: {}, userMessage: {}", userId, userMessage, e);

            // 降级处理：返回友好的错误信息
            String errorResponse = "抱歉，我遇到了一些问题，请稍后再试。";

            // 即使出错也尝试存储，用于问题分析
            try {
                memoryManager.storeConversation(userId, userMessage, errorResponse);
            } catch (Exception memoryError) {
                log.error("错误场景下记忆存储也失败", memoryError);
            }

            return errorResponse;
        }
    }

    /**
     * 获取用户记忆统计
     */
    public UnifiedMemoryManager.MemoryStats getUserMemoryStats(String userId) {
        return memoryManager.getMemoryStats(userId != null ? userId : getCurrentUserId());
    }

    /**
     * 清除用户记忆
     */
    public void clearUserMemory(String userId) {
        memoryManager.clearUserMemory(userId != null ? userId : getCurrentUserId());
    }

    /**
     * 获取当前用户ID
     */
    private String getCurrentUserId() {
        try {
            // 优先从UserLocalThreadUtils获取
            if (UserLocalThreadUtils.getUserInfo() != null &&
                    UserLocalThreadUtils.getUserInfo().getUserId() != null) {
                return UserLocalThreadUtils.getUserInfo().getUserId().toString();
            }
        } catch (Exception e) {
            log.debug("从UserLocalThreadUtils获取用户ID失败: {}", e.getMessage());
        }

        // 降级处理：使用默认用户ID（测试环境）
        String defaultUserId = "default_user_1";
        log.debug("使用默认用户ID: {}", defaultUserId);
        return defaultUserId;
    }
}