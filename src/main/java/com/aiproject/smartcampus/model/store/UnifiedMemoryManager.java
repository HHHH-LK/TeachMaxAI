package com.aiproject.smartcampus.model.store;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 统一记忆管理器 - 解决记忆存储隔离和一致性问题
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class UnifiedMemoryManager {

    private final LocalStore localStore;
    private final ChatMemoryStore chatMemoryStore;

    private static final String MEMORY_PREFIX = "smart_campus_summary_memory:";
    private static final int MAX_MEMORY_SIZE = 10; // 最多保留10轮对话

    /**
     * 统一存储会话记录
     *
     * @param userId      用户ID
     * @param userMessage 用户消息
     * @param aiResponse  AI回复
     */
    public void storeConversation(String userId, String userMessage, String aiResponse) {
        if (userId == null || userMessage == null || aiResponse == null) {
            log.warn("存储参数不完整，跳过记忆存储");
            return;
        }

        try {
            String memoryId = buildMemoryId(userId);

            List<ChatMessage> newMessages = Arrays.asList(
                    UserMessage.from(userMessage),
                    AiMessage.from(aiResponse)
            );

            // 同步更新两个存储系统，确保数据一致性
            updateBothStores(userId, memoryId, newMessages);

            log.debug("统一记忆存储成功，userId: {}, memoryId: {}", userId, memoryId);

        } catch (Exception e) {
            log.error("统一记忆存储失败，userId: {}", userId, e);
            // 不抛出异常，避免影响主业务流程
        }
    }

    /**
     * 获取用户记忆消息
     *
     * @param userId 用户ID
     * @return 记忆消息列表
     */
    public List<ChatMessage> getMemoryMessages(String userId) {
        if (userId == null) {
            log.warn("用户ID为空，返回空记忆");
            return new ArrayList<>();
        }

        try {
            String memoryId = buildMemoryId(userId);

            // 优先从ChatMemoryStore获取（用于任务处理）
            List<ChatMessage> messages = chatMemoryStore.getMessages(memoryId);

            if (messages == null || messages.isEmpty()) {
                log.debug("ChatMemoryStore中无记忆，尝试从LocalStore恢复");
                messages = recoverFromLocalStore(userId, memoryId);
            }

            // 限制记忆大小
            List<ChatMessage> limitedMessages = limitMemorySize(messages);

            log.debug("成功获取用户记忆，userId: {}, 消息数: {}", userId, limitedMessages.size());
            return limitedMessages;

        } catch (Exception e) {
            log.warn("获取用户记忆失败，userId: {}, 返回空记忆", userId, e);
            return new ArrayList<>();
        }
    }

    /**
     * 清除用户记忆
     *
     * @param userId 用户ID
     */
    public void clearUserMemory(String userId) {
        if (userId == null) {
            log.warn("用户ID为空，无法清除记忆");
            return;
        }

        try {
            String memoryId = buildMemoryId(userId);

            // 同时清除两个存储
            chatMemoryStore.deleteMessages(memoryId);
            localStore.deleteMessages(userId);

            log.info("成功清除用户记忆，userId: {}", userId);

        } catch (Exception e) {
            log.error("清除用户记忆失败，userId: {}", userId, e);
        }
    }

    /**
     * 获取记忆统计信息
     *
     * @param userId 用户ID
     * @return 记忆统计
     */
    public MemoryStats getMemoryStats(String userId) {
        try {
            List<ChatMessage> messages = getMemoryMessages(userId);
            int conversationRounds = messages.size() / 2;

            return MemoryStats.builder()
                    .userId(userId)
                    .totalMessages(messages.size())
                    .conversationRounds(conversationRounds)
                    .memoryId(buildMemoryId(userId))
                    .build();

        } catch (Exception e) {
            log.error("获取记忆统计失败，userId: {}", userId, e);
            return MemoryStats.empty(userId);
        }
    }

    // =============================================================================
    // 私有方法
    // =============================================================================

    /**
     * 构建统一的记忆ID
     */
    private String buildMemoryId(String userId) {
        return MEMORY_PREFIX + userId;
    }

    /**
     * 同时更新两个存储系统
     */
    private void updateBothStores(String userId, String memoryId, List<ChatMessage> newMessages) {
        Exception localStoreException = null;
        Exception chatMemoryException = null;

        // 尝试更新LocalStore
        try {
            localStore.updateMessages(userId, newMessages);
        } catch (Exception e) {
            localStoreException = e;
            log.warn("LocalStore更新失败，userId: {}", userId, e);
        }

        // 尝试更新ChatMemoryStore
        try {
            chatMemoryStore.updateMessages(memoryId, newMessages);
        } catch (Exception e) {
            chatMemoryException = e;
            log.warn("ChatMemoryStore更新失败，memoryId: {}", memoryId, e);
        }

        // 如果两个都失败，记录错误但不抛异常
        if (localStoreException != null && chatMemoryException != null) {
            log.error("两个记忆存储系统都更新失败，userId: {}, memoryId: {}", userId, memoryId);
        }
    }

    /**
     * 从LocalStore恢复记忆到ChatMemoryStore
     */
    private List<ChatMessage> recoverFromLocalStore(String userId, String memoryId) {
        try {
            List<ChatMessage> localMessages = localStore.getMessages(userId);

            if (localMessages != null && !localMessages.isEmpty()) {
                // 恢复到ChatMemoryStore
                chatMemoryStore.updateMessages(memoryId, localMessages);
                log.info("从LocalStore恢复记忆成功，userId: {}, 消息数: {}", userId, localMessages.size());
                return localMessages;
            }

        } catch (Exception e) {
            log.warn("从LocalStore恢复记忆失败，userId: {}", userId, e);
        }

        return new ArrayList<>();
    }

    /**
     * 限制记忆大小
     */
    private List<ChatMessage> limitMemorySize(List<ChatMessage> messages) {
        if (messages == null) {
            return new ArrayList<>();
        }

        int maxMessages = MAX_MEMORY_SIZE * 2; // 每轮对话包含用户消息和AI回复

        if (messages.size() <= maxMessages) {
            return new ArrayList<>(messages);
        }

        List<ChatMessage> limited = new ArrayList<>(
                messages.subList(messages.size() - maxMessages, messages.size())
        );

        log.debug("记忆消息过多，从{}条截取为{}条", messages.size(), limited.size());
        return limited;
    }

    @Data
    @Builder
    @AllArgsConstructor
    public static class MemoryStats {
        private String userId;
        private String memoryId;
        private int totalMessages;
        private int conversationRounds;

        public static MemoryStats empty(String userId) {
            return MemoryStats.builder()
                    .userId(userId)
                    .memoryId("")
                    .totalMessages(0)
                    .conversationRounds(0)
                    .build();
        }
    }
}

