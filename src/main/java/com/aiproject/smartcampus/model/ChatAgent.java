package com.aiproject.smartcampus.model;

import com.aiproject.smartcampus.commons.utils.UserLocalThreadUtils;
import com.aiproject.smartcampus.model.intent.Intent;
import com.aiproject.smartcampus.model.store.UnifiedMemoryManager;
import dev.langchain4j.data.message.ChatMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 增强版智能体实现，带记忆调试功能
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ChatAgent {

    private final Intent intentHandler;
    private final UnifiedMemoryManager memoryManager;

    /**
     * 智能体入口方法（增强版）
     */
    public String start(String userMessage) {
        String userId = "1";
        log.info("=== 智能体处理开始 ===");
        log.info("用户ID: {}, 用户消息: {}", userId, userMessage);

        try {
            // 1. 获取历史记忆
            List<ChatMessage> memoryMessages = memoryManager.getMemoryMessages(userId);
            log.info("获取到历史记忆: {}条消息", memoryMessages.size());

            // 调试：打印记忆内容（可选）
            if (log.isDebugEnabled() && !memoryMessages.isEmpty()) {
                log.debug("历史记忆内容:");
                for (int i = 0; i < Math.min(4, memoryMessages.size()); i++) {
                    ChatMessage msg = memoryMessages.get(i);
                    log.debug("  [{}] {}: {}", i, msg.type(),
                            msg.text().length() > 50 ? msg.text().substring(0, 50) + "..." : msg.text());
                }
            }

            // 2. 设置用户上下文（可能需要包含记忆）
            UserLocalThreadUtils.setUserMemory(userMessage);

            // 3. 处理用户意图（这里是关键！需要确保Intent处理器使用了记忆）
            String answer = processIntentWithMemory(userMessage, memoryMessages);

            // 4. 统一存储会话记录
            log.info("准备存储会话记录到记忆");
            memoryManager.storeConversation(userId, userMessage, answer);

            // 5. 验证存储结果
            List<ChatMessage> updatedMemory = memoryManager.getMemoryMessages(userId);
            log.info("存储后记忆数量: {} -> {}", memoryMessages.size(), updatedMemory.size());

            log.info("=== 智能体处理完成 ===");
            log.info("用户ID: {}, 输入长度: {}, 输出长度: {}", userId, userMessage.length(), answer.length());

            return answer;

        } catch (Exception e) {
            log.error("=== 智能体处理失败 ===");
            log.error("用户ID: {}, 用户消息: {}", userId, userMessage, e);

            // 降级处理：返回友好的错误信息
            String errorResponse = "抱歉，我遇到了一些问题，请稍后再试。如果问题持续，请联系管理员。";

            // 即使出错也尝试存储，用于问题分析
            try {
                memoryManager.storeConversation(userId, userMessage, errorResponse);
                log.info("错误场景下记忆存储成功");
            } catch (Exception memoryError) {
                log.error("错误场景下记忆存储也失败", memoryError);
            }

            return errorResponse;
        }
    }

    /**
     * 带记忆的意图处理（关键方法）
     */
    private String processIntentWithMemory(String userMessage, List<ChatMessage> memoryMessages) {
        try {
            // 方法1：如果Intent处理器支持记忆，直接传递
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
     * 构建包含记忆上下文的增强消息
     */
    private String buildEnhancedMessage(String userMessage, List<ChatMessage> memoryMessages) {
        if (memoryMessages == null || memoryMessages.isEmpty()) {
            log.debug("无历史记忆，直接返回原消息");
            return userMessage;
        }

        StringBuilder enhanced = new StringBuilder();

        // 添加记忆上下文
        enhanced.append("=== 对话历史上下文 ===\n");

        // 只取最近几轮对话作为上下文（避免token过多）
        int contextSize = Math.min(6, memoryMessages.size()); // 最多3轮对话
        List<ChatMessage> recentMessages = memoryMessages.subList(
                Math.max(0, memoryMessages.size() - contextSize), memoryMessages.size()
        );

        for (int i = 0; i < recentMessages.size(); i += 2) {
            if (i + 1 < recentMessages.size()) {
                ChatMessage userMsg = recentMessages.get(i);
                ChatMessage aiMsg = recentMessages.get(i + 1);

                enhanced.append("用户: ").append(userMsg.text()).append("\n");
                // 限制AI回复长度
                String aiReply = aiMsg.text();
                if (aiReply.length() > 200) {
                    aiReply = aiReply.substring(0, 200) + "...";
                }
                enhanced.append("AI: ").append(aiReply).append("\n");
                enhanced.append("---\n");
            }
        }

        // 添加当前用户消息
        enhanced.append("=== 当前用户消息 ===\n");
        enhanced.append(userMessage);
        enhanced.append("\n\n请基于上述对话历史，为用户提供连贯且个性化的回复。");

        return enhanced.toString();
    }

    /**
     * 获取用户记忆统计（增强版）
     */
    public UnifiedMemoryManager.MemoryStats getUserMemoryStats(String userId) {
        String targetUserId = userId != null ? userId : getCurrentUserId();
        log.info("获取用户记忆统计, userId: {}", targetUserId);

        UnifiedMemoryManager.MemoryStats stats = memoryManager.getMemoryStats(targetUserId);
        log.info("记忆统计结果: 总消息数={}, 对话轮数={}", stats.getTotalMessages(), stats.getConversationRounds());

        return stats;
    }

    /**
     * 清除用户记忆（增强版）
     */
    public void clearUserMemory(String userId) {
        String targetUserId = userId != null ? userId : getCurrentUserId();
        log.info("准备清除用户记忆, userId: {}", targetUserId);

        // 获取清除前的统计
        UnifiedMemoryManager.MemoryStats beforeStats = memoryManager.getMemoryStats(targetUserId);

        memoryManager.clearUserMemory(targetUserId);

        // 验证清除结果
        UnifiedMemoryManager.MemoryStats afterStats = memoryManager.getMemoryStats(targetUserId);
        log.info("记忆清除完成, 清除前: {}条消息, 清除后: {}条消息",
                beforeStats.getTotalMessages(), afterStats.getTotalMessages());
    }

    /**
     * 调试方法：获取完整记忆内容
     */
    public List<ChatMessage> getFullMemoryForDebug(String userId) {
        String targetUserId = userId != null ? userId : getCurrentUserId();
        List<ChatMessage> messages = memoryManager.getMemoryMessages(targetUserId);

        log.info("调试 - 获取完整记忆, userId: {}, 消息数量: {}", targetUserId, messages.size());

        if (log.isDebugEnabled()) {
            for (int i = 0; i < messages.size(); i++) {
                ChatMessage msg = messages.get(i);
                log.debug("消息[{}]: 类型={}, 内容={}", i, msg.type(),
                        msg.text().length() > 100 ? msg.text().substring(0, 100) + "..." : msg.text());
            }
        }

        return messages;
    }

    /**
     * 测试记忆功能
     */
    public String testMemoryFunction(String userId) {
        String targetUserId = userId != null ? userId : getCurrentUserId();

        try {
            // 1. 测试存储
            String testUser = "测试用户消息: " + System.currentTimeMillis();
            String testAI = "测试AI回复: " + System.currentTimeMillis();

            memoryManager.storeConversation(targetUserId, testUser, testAI);
            log.info("测试消息存储成功");

            // 2. 测试获取
            List<ChatMessage> messages = memoryManager.getMemoryMessages(targetUserId);
            log.info("测试消息获取成功，数量: {}", messages.size());

            // 3. 测试统计
            UnifiedMemoryManager.MemoryStats stats = memoryManager.getMemoryStats(targetUserId);
            log.info("测试统计获取成功: {}", stats);

            return String.format("记忆功能测试成功！当前记忆: %d条消息, %d轮对话",
                    stats.getTotalMessages(), stats.getConversationRounds());

        } catch (Exception e) {
            log.error("记忆功能测试失败", e);
            return "记忆功能测试失败: " + e.getMessage();
        }
    }

    /**
     * 获取当前用户ID（增强版）
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

        // 降级处理：使用默认用户ID（测试环境）
        String defaultUserId = "default_user_1";
        log.debug("使用默认用户ID: {}", defaultUserId);
        return defaultUserId;
    }

    /**
     * 接口：支持记忆的意图处理器
     */
    public interface MemoryAwareIntent {
        String handlerIntentWithMemory(String userMessage, List<ChatMessage> memoryMessages);
    }
}
