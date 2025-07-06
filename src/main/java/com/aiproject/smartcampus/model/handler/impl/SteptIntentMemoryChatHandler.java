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
    private final UnifiedMemoryManager memoryManager; // 使用统一记忆管理器
    private final IntentDelayedQueueClien intentDelayedQueueClien;
    private final ResultCilent resultCilent;

    // =============================================================================
    // 常量定义
    // =============================================================================

    private static final String MEMORY_SYSTEM_PROMPT =
            "你是一个智能助手，能够基于之前的对话记忆为用户提供准确的回答。请根据历史对话上下文和当前问题，给出恰当的回复。";

    private static final String FALLBACK_SYSTEM_PROMPT =
            "你是一个智能助手，请根据用户的问题直接给出准确、有用的回答。";

    private static final int DEPENDENCY_TIMEOUT_SECONDS = 10;
    private static final int STATUS_CHECK_TIMEOUT_SECONDS = 1;
    private static final int MAX_RETRIES = 5;
    private static final long SLEEP_INTERVAL_MS = 500;
    private static final long DELAYED_QUEUE_DELAY_MS = 1000;

    // =============================================================================
    // 主要业务逻辑入口
    // =============================================================================

    @Override
    protected String executeBusinessLogic(String intent, List<CompletableFuture<String>> result) {
        log.info("基于记忆的对话处理器执行[{}]中", intent);

        // 获取任务的入度（依赖数量）
        int inDegree = createDiagram.getInDegree(intent);

        if (inDegree == 0) {
            // 无依赖任务，直接执行
            return executeDirectMemoryTask(intent);
        } else {
            // 有依赖任务，需要等待依赖完成
            return executeMemoryWithDependencies(intent, result);
        }
    }

    // =============================================================================
    // 直接任务执行（无依赖）
    // =============================================================================

    /**
     * 执行无依赖的记忆任务
     */
    private String executeDirectMemoryTask(String intent) {
        try {
            log.info("开始执行直接记忆任务: {}", intent);

            // 获取当前用户的记忆
            String userId = getCurrentUserId();
            List<ChatMessage> memoryMessages = memoryManager.getMemoryMessages(userId);

            String result;
            if (memoryMessages.isEmpty()) {
                log.info("未找到历史记忆，降级使用基础LLM处理任务: {}", intent);
                result = executeWithBasicLLM(intent);
            } else {
                log.info("找到{}条历史记忆，使用记忆增强对话", memoryMessages.size());
                result = executeWithMemoryEnhanced(intent, memoryMessages);
            }

            // 注意：不在这里存储记忆，由ChatAgent统一处理，避免重复存储
            log.info("记忆对话执行[{}]成功,结果长度: {}", intent, result.length());
            return result;

        } catch (Exception e) {
            log.error("执行直接记忆任务失败: {}", intent, e);
            throw new RuntimeException("记忆对话执行失败: " + intent, e);
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
            throw new RuntimeException("基础LLM处理失败", e);
        }
    }

    /**
     * 使用记忆增强的LLM处理
     */
    private String executeWithMemoryEnhanced(String intent, List<ChatMessage> memoryMessages) {
        try {
            // 构建包含记忆的消息序列
            List<ChatMessage> messages = new ArrayList<>();
            messages.add(SystemMessage.from(MEMORY_SYSTEM_PROMPT));
            messages.addAll(memoryMessages);
            messages.add(UserMessage.from(buildEnhancedPrompt(intent, "")));

            ChatResponse chatResponse = chatLanguageModel.chat(messages);
            String result = chatResponse.aiMessage().text();

            log.info("记忆增强对话处理完成，结果长度: {}", result.length());
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
     * 执行有依赖的记忆任务
     */
    private String executeMemoryWithDependencies(String intent, List<CompletableFuture<String>> result) {
        List<String> parentTasks = createDiagram.getParetents(intent);
        int expectedSize = parentTasks.size();

        if (result != null && result.size() == expectedSize && isAllTasksCompleted(result)) {
            log.info("所有依赖任务已完成，开始执行记忆任务: {}", intent);
            return executeMemoryTaskWithResults(intent, result);
        } else {
            return handleMissingDependenciesWithoutException(intent, expectedSize);
        }
    }

    /**
     * 执行有依赖结果的记忆任务
     */
    private String executeMemoryTaskWithResults(String intent, List<CompletableFuture<String>> futures) {
        try {
            log.info("开始执行带依赖的记忆任务: {}", intent);

            // 收集所有依赖任务的结果
            List<String> dependencyResults = collectDependencyResults(intent, futures);

            // 构建上下文信息
            StringBuilder contextBuilder = new StringBuilder();
            for (int i = 0; i < dependencyResults.size(); i++) {
                contextBuilder.append("相关信息").append(i + 1).append(": ")
                        .append(dependencyResults.get(i)).append("\n");
            }

            // 获取用户记忆并执行任务
            String userId = getCurrentUserId();
            List<ChatMessage> memoryMessages = memoryManager.getMemoryMessages(userId);

            String result;
            if (memoryMessages.isEmpty()) {
                log.info("未找到历史记忆，使用基础LLM处理带依赖的任务: {}", intent);
                result = executeWithBasicLLMAndContext(intent, contextBuilder.toString());
            } else {
                log.info("找到{}条历史记忆，使用记忆增强处理带依赖的任务", memoryMessages.size());
                result = executeWithMemoryAndContext(intent, contextBuilder.toString(), memoryMessages);
            }

            log.info("带依赖的记忆对话执行[{}]成功,结果长度: {}", intent, result.length());
            return result;

        } catch (Exception e) {
            log.error("执行带依赖的记忆任务失败: {}", intent, e);
            throw new RuntimeException("带依赖的记忆任务执行失败: " + intent, e);
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
    private String executeWithBasicLLMAndContext(String intent, String contextInfo) {
        try {
            String enhancedPrompt = buildEnhancedPrompt(intent, contextInfo);

            ChatResponse chatResponse = chatLanguageModel.chat(
                    SystemMessage.from(FALLBACK_SYSTEM_PROMPT),
                    UserMessage.from(enhancedPrompt)
            );

            String result = chatResponse.aiMessage().text();
            log.info("基础LLM处理带上下文任务完成，结果长度: {}", result.length());

            return result != null && !result.trim().isEmpty() ? result : "任务已完成";

        } catch (Exception e) {
            log.error("基础LLM处理带上下文任务失败: {}", e.getMessage(), e);
            throw new RuntimeException("基础LLM处理失败", e);
        }
    }

    /**
     * 使用记忆增强处理带上下文的任务
     */
    private String executeWithMemoryAndContext(String intent, String contextInfo, List<ChatMessage> memoryMessages) {
        try {
            String enhancedPrompt = buildEnhancedPrompt(intent, contextInfo);

            List<ChatMessage> messages = new ArrayList<>();
            messages.add(SystemMessage.from(MEMORY_SYSTEM_PROMPT));
            messages.addAll(memoryMessages);
            messages.add(UserMessage.from(enhancedPrompt));

            ChatResponse chatResponse = chatLanguageModel.chat(messages);
            String result = chatResponse.aiMessage().text();

            log.info("记忆增强带上下文处理完成，结果长度: {}", result.length());
            return result != null && !result.trim().isEmpty() ? result : "任务已完成";

        } catch (Exception e) {
            log.warn("记忆增强带上下文处理失败，降级使用基础LLM: {}", e.getMessage());
            return executeWithBasicLLMAndContext(intent, contextInfo);
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
                future.get(STATUS_CHECK_TIMEOUT_SECONDS, TimeUnit.SECONDS);
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
            return executeMemoryTaskWithResults(intent, dependencyResults);
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
    // 提示词构建方法
    // =============================================================================

    /**
     * 构建增强的提示词
     */
    private String buildEnhancedPrompt(String originalIntent, String contextInfo) {
        StringBuilder promptBuilder = new StringBuilder();

        // 系统角色和能力声明
        promptBuilder.append("你是一个具备高级记忆能力的智能助手，能够：\n");
        promptBuilder.append("- 从历史对话中学习用户偏好和行为模式\n");
        promptBuilder.append("- 识别问题间的关联性和演进趋势\n");
        promptBuilder.append("- 提供基于个人历史的个性化建议\n\n");

        // 当前问题分析
        promptBuilder.append("【当前问题分析】\n");
        promptBuilder.append("用户问题：").append(originalIntent).append("\n");
        promptBuilder.append("问题类型：请首先分析这是什么类型的问题（咨询、操作、学习等）\n");
        promptBuilder.append("关键词提取：请识别问题中的核心关键词\n\n");

        // 记忆检索和分析
        if (contextInfo != null && !contextInfo.trim().isEmpty()) {
            promptBuilder.append("【记忆分析】\n");
            promptBuilder.append("历史记忆信息：\n").append(contextInfo).append("\n\n");
            promptBuilder.append("记忆检索任务：\n");
            promptBuilder.append("1. 从上述历史信息中搜索与当前问题相关的内容\n");
            promptBuilder.append("2. 识别用户的提问模式和偏好\n");
            promptBuilder.append("3. 分析问题的关联性和发展脉络\n");
            promptBuilder.append("4. 提取有助于当前回答的关键信息\n\n");

            promptBuilder.append("【信息融合要求】\n");
            promptBuilder.append("- 深度分析历史记忆中的关键信息\n");
            promptBuilder.append("- 识别信息间的关联性和一致性\n");
            promptBuilder.append("- 构建完整的知识图谱来回答问题\n\n");
        } else {
            promptBuilder.append("【记忆状态】\n");
            promptBuilder.append("当前暂无相关历史记忆，这将是建立用户记忆档案的起点。\n\n");
        }

        // 个性化回答策略
        promptBuilder.append("【个性化回答策略】\n");
        promptBuilder.append("1. 记忆驱动回答：\n");
        promptBuilder.append("   - 如果历史中有相似问题，说明：\"根据我们之前的讨论...\"\n");
        promptBuilder.append("   - 如果发现新需求，说明：\"这是一个新的需求，让我为你...\"\n");
        promptBuilder.append("   - 如果问题有演进，说明：\"我注意到你的需求在深化...\"\n\n");

        promptBuilder.append("2. 连贯性保持：\n");
        promptBuilder.append("   - 保持与之前回答风格的一致性\n");
        promptBuilder.append("   - 体现对用户学习/工作进度的了解\n");
        promptBuilder.append("   - 提供符合用户认知水平的解答\n\n");

        promptBuilder.append("3. 主动关怀：\n");
        promptBuilder.append("   - 基于历史互动主动提供相关建议\n");
        promptBuilder.append("   - 预测用户可能的后续需求\n");
        promptBuilder.append("   - 体现长期陪伴的智能助手价值\n\n");

        // 输出格式要求
        promptBuilder.append("【回答格式】\n");
        promptBuilder.append("请按以下结构组织你的回答：\n\n");
        promptBuilder.append("**直接回答**\n");
        promptBuilder.append("[对用户问题的直接、准确回答]\n\n");

        if (contextInfo != null && !contextInfo.trim().isEmpty()) {
            promptBuilder.append("**记忆关联**\n");
            promptBuilder.append("[基于历史记忆的补充信息或个性化见解]\n\n");

            promptBuilder.append("**深度分析**（如适用）\n");
            promptBuilder.append("[结合历史上下文的深入分析]\n\n");
        }

        promptBuilder.append("**个性化建议**\n");
        promptBuilder.append("[基于用户历史的后续建议或指导]\n\n");

        promptBuilder.append("现在请开始回答：");

        return promptBuilder.toString();
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
}