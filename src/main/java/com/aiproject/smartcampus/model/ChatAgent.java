package com.aiproject.smartcampus.model;

import com.aiproject.smartcampus.commons.utils.UserLocalThreadUtils;
import com.aiproject.smartcampus.model.intent.Intent;
import com.aiproject.smartcampus.model.store.UnifiedMemoryManager;
import dev.langchain4j.data.message.ChatMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChatAgent {

    private final Intent intentHandler;
    private final UnifiedMemoryManager memoryManager;

    /**
     * 智能体入口方法（修复版）
     */
    public String start(String userMessage) {
        String userId = getCurrentUserId();
        log.info("=== 智能体处理开始 ===");
        log.info("用户ID: {}, 用户消息: {}", userId, userMessage);

        try {
            // 1. 获取历史记忆
            List<ChatMessage> memoryMessages = memoryManager.getMemoryMessages(userId);
            log.info("获取到历史记忆: {}条消息", memoryMessages.size());

            // 调试：打印记忆内容
            if (log.isDebugEnabled() && !memoryMessages.isEmpty()) {
                log.debug("历史记忆内容:");
                for (int i = 0; i < Math.min(4, memoryMessages.size()); i++) {
                    ChatMessage msg = memoryMessages.get(i);
                    log.debug("  [{}] {}: {}", i, msg.type(),
                            msg.text().length() > 50 ? msg.text().substring(0, 50) + "..." : msg.text());
                }
            }

            // 2. **关键修复：正确设置线程上下文**
            UserLocalThreadUtils.setUserId(userId);
            UserLocalThreadUtils.setUserMemory(memoryMessages);  // 传递记忆列表而不是字符串

            // 3. 处理用户意图
            String answer = processIntentWithMemory(userMessage, memoryMessages);

            // 4. 统一存储会话记录
            log.info("准备存储会话记录到记忆");
            memoryManager.storeConversation(userId, userMessage, answer);

            // 5. 验证存储结果
            List<ChatMessage> updatedMemory = memoryManager.getMemoryMessages(userId);
            log.info("存储后记忆数量: {} -> {}", memoryMessages.size(), updatedMemory.size());

            log.info("=== 智能体处理完成 ===");
            return answer;

        } catch (Exception e) {
            log.error("=== 智能体处理失败 ===", e);
            String errorResponse = "抱歉，我遇到了一些问题，请稍后再试。";

            // 即使出错也尝试存储
            try {
                memoryManager.storeConversation(userId, userMessage, errorResponse);
            } catch (Exception memoryError) {
                log.error("错误场景下记忆存储也失败", memoryError);
            }

            return errorResponse;
        } finally {
            // **重要：清理线程上下文**
            UserLocalThreadUtils.clear();
        }
    }

    /**
     * 带记忆的意图处理（修复版）
     */
    private String processIntentWithMemory(String userMessage, List<ChatMessage> memoryMessages) {
        try {
            // 方法1：检查是否支持记忆的意图处理器
            if (intentHandler instanceof MemoryAwareIntent) {
                log.info("使用支持记忆的意图处理器");
                return ((MemoryAwareIntent) intentHandler).handlerIntentWithMemory(userMessage, memoryMessages);
            }

            // 方法2：构建包含记忆上下文的增强消息
            String enhancedMessage = buildEnhancedMessage(userMessage, memoryMessages);
            log.info("构建增强消息，原长度: {}, 增强后长度: {}", userMessage.length(), enhancedMessage.length());

            return intentHandler.handlerIntent(enhancedMessage);

        } catch (Exception e) {
            log.warn("带记忆的意图处理失败，回退到普通处理", e);
            return intentHandler.handlerIntent(userMessage);
        }
    }

    /**
     * 构建包含记忆上下文的增强消息（优化版）
     */
    private String buildEnhancedMessage(String userMessage, List<ChatMessage> memoryMessages) {
        if (memoryMessages == null || memoryMessages.isEmpty()) {
            log.debug("无历史记忆，直接返回原消息");
            return userMessage;
        }

        StringBuilder enhanced = new StringBuilder();

        // 添加记忆上下文标识
        enhanced.append("【历史对话上下文】\n");

        // 获取最近的对话作为上下文（避免token过多）
        int contextSize = Math.min(6, memoryMessages.size()); // 最多3轮对话
        List<ChatMessage> recentMessages = memoryMessages.subList(
                Math.max(0, memoryMessages.size() - contextSize), memoryMessages.size()
        );

        // 按用户-AI对的形式组织对话
        for (int i = 0; i < recentMessages.size(); i += 2) {
            if (i + 1 < recentMessages.size()) {
                ChatMessage userMsg = recentMessages.get(i);
                ChatMessage aiMsg = recentMessages.get(i + 1);

                enhanced.append("用户: ").append(userMsg.text()).append("\n");

                // 限制AI回复长度，避免上下文过长
                String aiReply = aiMsg.text();
                if (aiReply.length() > 200) {
                    aiReply = aiReply.substring(0, 200) + "...";
                }
                enhanced.append("助手: ").append(aiReply).append("\n");
                enhanced.append("---\n");
            }
        }

        // 添加当前用户消息
        enhanced.append("\n【当前用户问题】\n");
        enhanced.append(userMessage);
        enhanced.append("\n\n指令：请基于上述历史对话上下文，为用户提供连贯且个性化的回复。");

        return enhanced.toString();
    }

    /**
     * 获取当前用户ID（修复版）
     */
    private String getCurrentUserId() {
        try {
            // 优先从UserLocalThreadUtils获取
            if (UserLocalThreadUtils.getUserInfo() != null &&
                    UserLocalThreadUtils.getUserInfo().getUserId() != null) {
                String userId = UserLocalThreadUtils.getUserInfo().getUserId().toString();
                log.debug("从UserLocalThreadUtils获取用户ID: {}", userId);
                return userId;
            }
        } catch (Exception e) {
            log.debug("从UserLocalThreadUtils获取用户ID失败: {}", e.getMessage());
        }

        // 降级处理：使用默认用户ID
        String defaultUserId = "default_user_1";
        log.debug("使用默认用户ID: {}", defaultUserId);
        return defaultUserId;
    }

    // === 其他方法保持不变 ===

    public UnifiedMemoryManager.MemoryStats getUserMemoryStats(String userId) {
        String targetUserId = userId != null ? userId : getCurrentUserId();
        return memoryManager.getMemoryStats(targetUserId);
    }

    public void clearUserMemory(String userId) {
        String targetUserId = userId != null ? userId : getCurrentUserId();
        memoryManager.clearUserMemory(targetUserId);
    }

    public List<ChatMessage> getFullMemoryForDebug(String userId) {
        String targetUserId = userId != null ? userId : getCurrentUserId();
        return memoryManager.getMemoryMessages(targetUserId);
    }

    /**
     * 接口：支持记忆的意图处理器
     */
    public interface MemoryAwareIntent {
        String handlerIntentWithMemory(String userMessage, List<ChatMessage> memoryMessages);
    }
}