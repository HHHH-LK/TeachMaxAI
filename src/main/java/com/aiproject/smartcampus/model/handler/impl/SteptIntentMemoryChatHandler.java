package com.aiproject.smartcampus.model.handler.impl;

import com.aiproject.smartcampus.commons.client.ResultCilent;
import com.aiproject.smartcampus.commons.delayedtask.IntentBatchTask;
import com.aiproject.smartcampus.commons.delayedtask.IntentDelayedQueueClien;
import com.aiproject.smartcampus.commons.utils.CreateDiagram;
import com.aiproject.smartcampus.commons.utils.UserLocalThreadUtils;
import com.aiproject.smartcampus.model.handler.BaseEnhancedHandler;
import com.aiproject.smartcampus.model.store.UnifiedMemoryManager;
import com.github.xiaoymin.knife4j.core.util.StrUtil;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 完整的记忆对话处理器 - 使用统一记忆管理，支持用户隔离
 * 修复版本：正确使用线程上下文中的记忆信息
 *
 * @author: lk
 * @description: 基于记忆增强的智能对话处理器，支持依赖任务管理和用户级记忆隔离
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SteptIntentMemoryChatHandler extends BaseEnhancedHandler {

    // =============================================================================
    // 依赖注入
    // =============================================================================

    private final CreateDiagram createDiagram;
    private final ChatLanguageModel chatLanguageModel;
    private final UnifiedMemoryManager memoryManager;
    private final IntentDelayedQueueClien intentDelayedQueueClien;
    private final ResultCilent resultCilent;

    // =============================================================================
    // 常量定义
    // =============================================================================

    private static final String MEMORY_SYSTEM_PROMPT =
            "你是一个具备记忆能力的智能助手。你能够：\n" +
                    "1. 记住之前的对话内容和用户偏好\n" +
                    "2. 基于历史上下文提供连贯的回复\n" +
                    "3. 识别用户问题的关联性和发展趋势\n" +
                    "请基于对话历史为用户提供个性化和连贯的回复。";

    private static final String FALLBACK_SYSTEM_PROMPT =
            "你是一个智能助手，请根据用户的问题直接给出准确、有用的回答。";

    private static final int DEPENDENCY_TIMEOUT_SECONDS = 10;
    private static final int MAX_RETRIES = 5;
    private static final long SLEEP_INTERVAL_MS = 500;
    private static final long DELAYED_QUEUE_DELAY_MS = 1000;
    private static final int MAX_HISTORY_MESSAGES = 10; // 最多保留5轮对话

    // =============================================================================
    // 主要业务逻辑入口
    // =============================================================================

    @Override
    protected String executeBusinessLogic(String intent, List<CompletableFuture<String>> result) {
        log.info("记忆对话处理器执行[{}]中", intent);

        // **关键修复1：从线程上下文获取记忆和用户ID**
        List<ChatMessage> threadMemoryMessages = UserLocalThreadUtils.getChatMemory();
        String threadUserId = UserLocalThreadUtils.getCurrentUserId();

        log.info("从线程上下文获取 - 记忆: {}条, 用户ID: {}",
                threadMemoryMessages != null ? threadMemoryMessages.size() : 0, threadUserId);

        // **关键修复2：如果线程上下文没有记忆，尝试直接从记忆管理器获取**
        List<ChatMessage> memoryMessages = threadMemoryMessages;
        String userId = threadUserId;

        if ((memoryMessages == null || memoryMessages.isEmpty()) && userId != null) {
            log.info("线程上下文无记忆，尝试从记忆管理器获取，userId: {}", userId);
            memoryMessages = memoryManager.getMemoryMessages(userId);
            log.info("从记忆管理器获取到{}条记忆", memoryMessages.size());
        }

        // 如果还是没有用户ID，使用默认值
        if (userId == null) {
            userId = getCurrentUserId();
            log.info("使用默认用户ID: {}", userId);
            if (memoryMessages == null || memoryMessages.isEmpty()) {
                memoryMessages = memoryManager.getMemoryMessages(userId);
                log.info("使用默认用户ID获取到{}条记忆", memoryMessages.size());
            }
        }

        // 获取任务的入度（依赖数量）
        int inDegree = createDiagram.getInDegree(intent);

        if (inDegree == 0) {
            // 无依赖任务，直接执行
            return executeDirectMemoryTask(intent, memoryMessages, userId);
        } else {
            // 有依赖任务，需要等待依赖完成
            return executeMemoryWithDependencies(intent, result, memoryMessages, userId);
        }
    }

    // =============================================================================
    // 直接任务执行（无依赖）
    // =============================================================================

    /**
     * 执行无依赖的记忆任务（修复版）
     */
    private String executeDirectMemoryTask(String intent, List<ChatMessage> memoryMessages, String userId) {
        try {
            log.info("开始执行直接记忆任务: {}, 用户ID: {}", intent, userId);

            String result;
            if (memoryMessages == null || memoryMessages.isEmpty()) {
                log.info("未找到历史记忆，使用基础LLM处理任务: {}", intent);
                result = executeWithBasicLLM(intent);
            } else {
                log.info("找到{}条历史记忆，使用记忆增强对话", memoryMessages.size());
                result = executeWithMemoryEnhanced(intent, memoryMessages);
            }

            log.info("记忆对话执行[{}]成功,结果长度: {}", intent, result.length());
            return result;

        } catch (Exception e) {
            log.error("执行直接记忆任务失败: {}", intent, e);
            // 降级到基础LLM，不抛异常
            log.warn("降级使用基础LLM处理");
            return executeWithBasicLLM(intent);
        }
    }

    /**
     * 使用基础LLM处理（无记忆增强）
     */
    private String executeWithBasicLLM(String intent) {
        try {
            ChatResponse chatResponse = chatLanguageModel.chat(
                    SystemMessage.from(FALLBACK_SYSTEM_PROMPT),
                    UserMessage.from(intent)
            );

            String result = chatResponse.aiMessage().text();
            log.info("基础LLM处理完成，结果长度: {}", result.length());

            return result != null && !result.trim().isEmpty() ? result : "任务已完成";

        } catch (Exception e) {
            log.error("基础LLM处理失败: {}", e.getMessage(), e);
            return "抱歉，处理失败，请稍后重试。";
        }
    }

    /**
     * 使用记忆增强的LLM处理（关键修复）
     */
    private String executeWithMemoryEnhanced(String intent, List<ChatMessage> memoryMessages) {
        try {
            // **关键修复3：正确构建包含记忆的对话上下文**
            List<ChatMessage> messages = new ArrayList<>();

            // 添加系统消息
            messages.add(SystemMessage.from(MEMORY_SYSTEM_PROMPT));

            // **重要：添加历史记忆到对话上下文**
            if (memoryMessages != null && !memoryMessages.isEmpty()) {
                // 限制历史记忆长度，避免token过多
                List<ChatMessage> limitedHistory = memoryMessages.size() > MAX_HISTORY_MESSAGES
                        ? memoryMessages.subList(memoryMessages.size() - MAX_HISTORY_MESSAGES, memoryMessages.size())
                        : memoryMessages;

                messages.addAll(limitedHistory);
                log.info("已添加{}条历史记忆到对话上下文，原始记忆{}条",
                        limitedHistory.size(), memoryMessages.size());

                // 调试：打印部分记忆内容
                if (log.isDebugEnabled()) {
                    for (int i = 0; i < Math.min(4, limitedHistory.size()); i++) {
                        ChatMessage msg = limitedHistory.get(i);
                        log.debug("记忆[{}]: {} - {}", i, msg.type(),
                                msg.text().length() > 50 ? msg.text().substring(0, 50) + "..." : msg.text());
                    }
                }
            }

            // 添加当前用户消息
            messages.add(UserMessage.from(intent));

            // 调用LLM
            ChatResponse chatResponse = chatLanguageModel.chat(messages);
            String result = chatResponse.aiMessage().text();

            log.info("记忆增强对话处理完成，输入消息数: {}, 结果长度: {}",
                    messages.size(), result.length());

            return result != null && !result.trim().isEmpty() ? result : "任务已完成";

        } catch (Exception e) {
            log.warn("记忆增强对话失败，降级使用基础LLM: {}", e.getMessage());
            return executeWithBasicLLM(intent);
        }
    }

    // =============================================================================
    // 依赖任务执行
    // =============================================================================

    /**
     * 执行有依赖的记忆任务（修复版）
     */
    private String executeMemoryWithDependencies(String intent, List<CompletableFuture<String>> result,
                                                 List<ChatMessage> memoryMessages, String userId) {
        List<String> parentTasks = createDiagram.getParetents(intent);
        int expectedSize = parentTasks.size();

        if (result != null && result.size() == expectedSize && isAllTasksCompleted(result)) {
            log.info("所有依赖任务已完成，开始执行记忆任务: {}", intent);
            return executeMemoryTaskWithResults(intent, result, memoryMessages, userId);
        } else {
            return handleMissingDependenciesWithoutException(intent, expectedSize);
        }
    }

    /**
     * 执行有依赖结果的记忆任务（修复版）
     */
    private String executeMemoryTaskWithResults(String intent, List<CompletableFuture<String>> futures,
                                                List<ChatMessage> memoryMessages, String userId) {
        try {
            log.info("开始执行带依赖的记忆任务: {}, 用户ID: {}", intent, userId);

            // 收集所有依赖任务的结果
            List<String> dependencyResults = collectDependencyResults(intent, futures);

            // 构建上下文信息
            StringBuilder contextBuilder = new StringBuilder();
            contextBuilder.append("【前置任务结果】\n");
            for (int i = 0; i < dependencyResults.size(); i++) {
                contextBuilder.append("任务").append(i + 1).append(": ")
                        .append(dependencyResults.get(i)).append("\n");
            }
            contextBuilder.append("\n【当前任务】\n").append(intent);

            String result;
            if (memoryMessages == null || memoryMessages.isEmpty()) {
                log.info("未找到历史记忆，使用基础LLM处理带依赖的任务: {}", intent);
                result = executeWithBasicLLMAndContext(contextBuilder.toString());
            } else {
                log.info("找到{}条历史记忆，使用记忆增强处理带依赖的任务", memoryMessages.size());
                result = executeWithMemoryAndContext(contextBuilder.toString(), memoryMessages);
            }

            log.info("带依赖的记忆对话执行[{}]成功,结果长度: {}", intent, result.length());
            return result;

        } catch (Exception e) {
            log.error("执行带依赖的记忆任务失败: {}", intent, e);
            // 降级处理
            return executeDirectMemoryTask(intent, memoryMessages, userId);
        }
    }

    /**
     * 收集依赖任务结果
     */
    private List<String> collectDependencyResults(String intent, List<CompletableFuture<String>> futures)
            throws InterruptedException, ExecutionException, TimeoutException {

        List<String> dependencyResults = new ArrayList<>();

        for (int i = 0; i < futures.size(); i++) {
            try {
                String result = futures.get(i).get(DEPENDENCY_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                dependencyResults.add(result != null ? result : "");

            } catch (TimeoutException te) {
                log.warn("前置任务{}等待超时，记忆任务[{}]将重试", i, intent);
                throw te;

            } catch (ExecutionException | InterruptedException e) {
                log.error("获取前置任务{}结果异常，记忆任务[{}]", i, intent, e);
                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
                throw e;
            }
        }

        return dependencyResults;
    }

    /**
     * 使用基础LLM处理带上下文的任务
     */
    private String executeWithBasicLLMAndContext(String contextInfo) {
        try {
            ChatResponse chatResponse = chatLanguageModel.chat(
                    SystemMessage.from(FALLBACK_SYSTEM_PROMPT),
                    UserMessage.from(contextInfo)
            );

            String result = chatResponse.aiMessage().text();
            log.info("基础LLM处理带上下文任务完成，结果长度: {}", result.length());

            return result != null && !result.trim().isEmpty() ? result : "任务已完成";

        } catch (Exception e) {
            log.error("基础LLM处理带上下文任务失败: {}", e.getMessage(), e);
            return "处理失败，请稍后重试。";
        }
    }

    /**
     * 使用记忆增强处理带上下文的任务（修复版）
     */
    private String executeWithMemoryAndContext(String contextInfo, List<ChatMessage> memoryMessages) {
        try {
            List<ChatMessage> messages = new ArrayList<>();
            messages.add(SystemMessage.from(MEMORY_SYSTEM_PROMPT));

            // **重要：添加历史记忆**
            if (memoryMessages != null && !memoryMessages.isEmpty()) {
                List<ChatMessage> limitedHistory = memoryMessages.size() > MAX_HISTORY_MESSAGES
                        ? memoryMessages.subList(memoryMessages.size() - MAX_HISTORY_MESSAGES, memoryMessages.size())
                        : memoryMessages;
                messages.addAll(limitedHistory);
                log.info("带依赖任务已添加{}条历史记忆", limitedHistory.size());
            }

            messages.add(UserMessage.from(contextInfo));

            ChatResponse chatResponse = chatLanguageModel.chat(messages);
            String result = chatResponse.aiMessage().text();

            log.info("记忆增强带上下文处理完成，结果长度: {}", result.length());
            return result != null && !result.trim().isEmpty() ? result : "任务已完成";

        } catch (Exception e) {
            log.warn("记忆增强带上下文处理失败，降级使用基础LLM: {}", e.getMessage());
            return executeWithBasicLLMAndContext(contextInfo);
        }
    }

    // =============================================================================
    // 依赖管理相关方法
    // =============================================================================

    /**
     * 检查所有任务是否已完成
     */
    private boolean isAllTasksCompleted(List<CompletableFuture<String>> result) {
        if (result == null || result.isEmpty()) {
            return false;
        }

        for (CompletableFuture<String> future : result) {
            if (future == null || !future.isDone() || future.isCancelled()) {
                log.debug("发现未完成的依赖任务");
                return false;
            }

            // 检查是否有异常完成
            try {
                future.get(1, TimeUnit.SECONDS);
            } catch (Exception e) {
                log.debug("依赖任务异常完成: {}", e.getMessage());
                return false;
            }
        }
        return true;
    }

    /**
     * 处理依赖缺失的情况（不抛异常版本）
     */
    private String handleMissingDependenciesWithoutException(String intent, int expectedSize) {
        log.info("记忆任务[{}]依赖未就绪，尝试等待依赖完成", intent);

        // 尝试等待依赖完成
        List<CompletableFuture<String>> dependencyResults = waitForDependencies(intent, expectedSize);

        if (dependencyResults != null && !dependencyResults.isEmpty() && isAllTasksCompleted(dependencyResults)) {
            log.info("等待后依赖已完成，直接执行任务: {}", intent);
            // 获取记忆信息
            List<ChatMessage> memoryMessages = UserLocalThreadUtils.getChatMemory();
            String userId = UserLocalThreadUtils.getCurrentUserId();
            return executeMemoryTaskWithResults(intent, dependencyResults, memoryMessages, userId);
        } else {
            // 检查是否已在队列中，避免重复加入
            if (!isTaskInDelayedQueue(intent)) {
                intentDelayedQueueClien.addTask(new IntentBatchTask(intent, dependencyResults, DELAYED_QUEUE_DELAY_MS));
                log.info("记忆任务[{}]已加入到延时队列中，等待依赖完成", intent);
            } else {
                log.info("记忆任务[{}]已在延时队列中，跳过重复提交", intent);
            }

            // 返回占位符结果，不抛异常
            return "任务等待依赖完成，已加入延时队列";
        }
    }

    /**
     * 等待依赖任务完成
     */
    private List<CompletableFuture<String>> waitForDependencies(String intent, int expectedSize) {
        List<CompletableFuture<String>> dependencyResults = new ArrayList<>();
        int retryCount = 0;

        while (retryCount < MAX_RETRIES) {
            try {
                List<String> parents = createDiagram.getParetents(intent);
                dependencyResults.clear();

                boolean allDependenciesReady = true;
                for (String parent : parents) {
                    List<CompletableFuture<String>> parentResults = resultCilent.getResult(parent);
                    if (parentResults == null || parentResults.isEmpty()) {
                        allDependenciesReady = false;
                        break;
                    }
                    dependencyResults.addAll(parentResults);
                }

                if (allDependenciesReady && dependencyResults.size() == expectedSize && isAllTasksCompleted(dependencyResults)) {
                    log.info("记忆任务[{}]的依赖结果已准备完成", intent);
                    break;
                }

                Thread.sleep(SLEEP_INTERVAL_MS);
                retryCount++;
                log.debug("记忆任务[{}]等待依赖结果中，重试次数: {}/{}", intent, retryCount, MAX_RETRIES);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("等待记忆任务依赖结果被中断: {}", intent, e);
                break;
            } catch (Exception e) {
                log.error("获取记忆任务依赖结果时发生异常: {}", intent, e);
                retryCount++;
            }
        }

        return dependencyResults;
    }

    /**
     * 检查任务是否已在延时队列中
     */
    private boolean isTaskInDelayedQueue(String intent) {
        if (intent != null && !StrUtil.isBlank(intent)) {
            return intentDelayedQueueClien.containsTask(intent);
        } else {
            log.error("记忆任务intent为null");
            return false;
        }
    }

    // =============================================================================
    // 工具方法
    // =============================================================================

    /**
     * 获取当前用户ID - 与ChatAgent保持一致
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

        // 降级处理：使用默认用户ID
        String defaultUserId = "default_user_1";
        log.debug("使用默认用户ID: {}", defaultUserId);
        return defaultUserId;
    }

    /**
     * 任务失败时是否更新图状态
     */
    @Override
    protected boolean shouldUpdateGraphOnFailure() {
        return true; // 记忆任务失败时也要更新图状态，避免任务阻塞
    }

    // =============================================================================
    // 公共方法 - 提供给外部调用的接口
    // =============================================================================

    /**
     * 获取当前处理器的记忆统计信息
     */
    public UnifiedMemoryManager.MemoryStats getHandlerMemoryStats() {
        String userId = getCurrentUserId();
        return memoryManager.getMemoryStats(userId);
    }

    /**
     * 清除当前用户的记忆
     */
    public void clearCurrentUserMemory() {
        String userId = getCurrentUserId();
        memoryManager.clearUserMemory(userId);
        log.info("已清除当前用户记忆，userId: {}", userId);
    }

    /**
     * 检查当前用户是否有记忆
     */
    public boolean hasUserMemory() {
        String userId = getCurrentUserId();
        List<ChatMessage> memories = memoryManager.getMemoryMessages(userId);
        return memories != null && !memories.isEmpty();
    }

    /**
     * 获取当前用户的记忆消息数量
     */
    public int getUserMemoryCount() {
        String userId = getCurrentUserId();
        List<ChatMessage> memories = memoryManager.getMemoryMessages(userId);
        return memories != null ? memories.size() : 0;
    }

    /**
     * 调试方法：打印当前记忆状态
     */
    public void debugMemoryStatus() {
        String userId = getCurrentUserId();
        List<ChatMessage> threadMemory = UserLocalThreadUtils.getChatMemory();
        List<ChatMessage> managerMemory = memoryManager.getMemoryMessages(userId);

        log.info("=== 记忆状态调试 ===");
        log.info("用户ID: {}", userId);
        log.info("线程上下文记忆: {}条", threadMemory != null ? threadMemory.size() : 0);
        log.info("记忆管理器记忆: {}条", managerMemory != null ? managerMemory.size() : 0);

        if (threadMemory != null && !threadMemory.isEmpty()) {
            log.info("线程记忆示例: {}", threadMemory.get(0).text().substring(0,
                    Math.min(50, threadMemory.get(0).text().length())));
        }

        if (managerMemory != null && !managerMemory.isEmpty()) {
            log.info("管理器记忆示例: {}", managerMemory.get(0).text().substring(0,
                    Math.min(50, managerMemory.get(0).text().length())));
        }
        log.info("=== 调试结束 ===");
    }
}