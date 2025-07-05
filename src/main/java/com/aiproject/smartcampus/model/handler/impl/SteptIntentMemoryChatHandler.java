package com.aiproject.smartcampus.model.handler.impl;

import com.aiproject.smartcampus.commons.client.ResultCilent;
import com.aiproject.smartcampus.commons.delayedtask.IntentBatchTask;
import com.aiproject.smartcampus.commons.delayedtask.IntentDelayedQueueClien;
import com.aiproject.smartcampus.commons.utils.CreateDiagram;
import com.aiproject.smartcampus.commons.utils.UserLocalThreadUtils;
import com.aiproject.smartcampus.model.handler.BaseEnhancedHandler;
import com.github.xiaoymin.knife4j.core.util.StrUtil;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
@Slf4j
@RequiredArgsConstructor
public class SteptIntentMemoryChatHandler extends BaseEnhancedHandler {

    private final CreateDiagram createDiagram;
    private final ChatLanguageModel chatLanguageModel;
    private final ChatMemoryStore chatMemoryStore;
    private final IntentDelayedQueueClien intentDelayedQueueClien;
    private final ResultCilent resultCilent;

    private static String DEFAULT_MEMORY_ID = "smart_campus_summary_memory:";
    private static final String MEMORY_SYSTEM_PROMPT = "你是一个智能助手，能够基于之前的对话记忆为用户提供准确的回答。请根据历史对话上下文和当前问题，给出恰当的回复。";
    private static final String FALLBACK_SYSTEM_PROMPT = "你是一个智能助手，请根据用户的问题直接给出准确、有用的回答。";

    @Override
    protected String executeBusinessLogic(String intent, List<CompletableFuture<String>> result) {
        log.info("基于记忆的对话处理器执行[{}]中", intent);

        DEFAULT_MEMORY_ID = DEFAULT_MEMORY_ID + UserLocalThreadUtils.test();
        int inDegree = createDiagram.getInDegree(intent);

        if (inDegree == 0) {
            return executeDirectMemoryTask(intent);
        } else {
            return executeMemoryWithDependencies(intent, result);
        }
    }

    private String executeDirectMemoryTask(String intent) {
        try {
            log.info("开始执行直接记忆任务: {}", intent);

            List<ChatMessage> memoryMessages = getMemoryMessages();
            String result;

            if (memoryMessages.isEmpty()) {
                log.info("未找到历史记忆，降级使用基础LLM处理任务: {}", intent);
                result = executeWithBasicLLM(intent);
            } else {
                log.info("找到{}条历史记忆，使用记忆增强对话", memoryMessages.size());
                result = executeWithMemoryEnhanced(intent, memoryMessages);
            }

            try {
                updateMemory(intent, result);
                log.debug("已更新对话记忆");
            } catch (Exception e) {
                log.warn("更新记忆失败，但不影响任务执行: {}", e.getMessage());
            }

            log.info("记忆对话执行[{}]成功,结果长度: {}", intent, result.length());
            return result;

        } catch (Exception e) {
            log.error("执行直接记忆任务失败: {}", intent, e);
            throw new RuntimeException("记忆对话执行失败", e);
        }
    }

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

    private String executeWithMemoryEnhanced(String intent, List<ChatMessage> memoryMessages) {
        try {
            List<ChatMessage> messages = new ArrayList<>();
            messages.add(SystemMessage.from(MEMORY_SYSTEM_PROMPT));
            messages.addAll(memoryMessages);
            messages.add(UserMessage.from(intent));

            ChatResponse chatResponse = chatLanguageModel.chat(messages);
            String result = chatResponse.aiMessage().text();

            log.info("记忆增强对话处理完成，结果长度: {}", result.length());

            return result != null && !result.trim().isEmpty() ? result : "任务已完成";
        } catch (Exception e) {
            log.warn("记忆增强对话失败，降级使用基础LLM: {}", e.getMessage());
            return executeWithBasicLLM(intent);
        }
    }

    private String executeMemoryWithDependencies(String intent, List<CompletableFuture<String>> result) {
        List<String> parentTasks = createDiagram.getParetents(intent);
        int expectedSize = parentTasks.size();

        // 修复1: 更严格的依赖检查
        if (result != null && result.size() == expectedSize && isAllTasksCompleted(result)) {
            log.info("所有依赖任务已完成，开始执行记忆任务: {}", intent);
            return executeMemoryTaskWithResults(intent, result);
        } else {
            // 修复2: 不抛异常，而是通过延时队列处理
            return handleMissingDependenciesWithoutException(intent, expectedSize);
        }
    }

    // 修复3: 改进的任务完成状态检查
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
                future.get(1, TimeUnit.SECONDS); // 短时间检查，避免阻塞
            } catch (Exception e) {
                log.debug("依赖任务异常完成: {}", e.getMessage());
                return false;
            }
        }
        return true;
    }

    // 修复4: 不抛异常的依赖处理方法
    private String handleMissingDependenciesWithoutException(String intent, int expectedSize) {
        log.info("记忆任务[{}]依赖未就绪，尝试等待依赖完成", intent);

        // 尝试等待依赖完成
        List<CompletableFuture<String>> dependencyResults = waitForDependencies(intent, expectedSize);

        if (dependencyResults != null && !dependencyResults.isEmpty() && isAllTasksCompleted(dependencyResults)) {
            log.info("等待后依赖已完成，直接执行任务: {}", intent);
            return executeMemoryTaskWithResults(intent, dependencyResults);
        } else {
            // 修复5: 检查是否已在队列中，避免重复加入
            if (!isTaskInDelayedQueue(intent)) {
                intentDelayedQueueClien.addTask(new IntentBatchTask(intent, dependencyResults, 1000)); // 增加延时
                log.info("记忆任务[{}]已加入到延时队列中，等待依赖完成", intent);
            } else {
                log.info("记忆任务[{}]已在延时队列中，跳过重复提交", intent);
            }

            // 修复6: 返回占位符结果，不抛异常
            return "任务等待依赖完成，已加入延时队列";
        }
    }

    private String executeMemoryTaskWithResults(String intent, List<CompletableFuture<String>> futures) {
        try {
            log.info("开始执行带依赖的记忆任务: {}", intent);

            List<String> dependencyResults = new ArrayList<>();

            for (int i = 0; i < futures.size(); i++) {
                try {
                    String result = futures.get(i).get(10, TimeUnit.SECONDS); // 增加超时时间
                    dependencyResults.add(result != null ? result : "");
                } catch (TimeoutException te) {
                    log.warn("前置任务{}等待超时，记忆任务[{}]将重试", i, intent);
                    throw new RuntimeException("前置任务执行超时", te);
                } catch (ExecutionException | InterruptedException e) {
                    log.error("获取前置任务{}结果异常，记忆任务[{}]", i, intent, e);
                    if (e instanceof InterruptedException) {
                        Thread.currentThread().interrupt();
                    }
                    throw new RuntimeException("获取前置结果异常", e);
                }
            }

            StringBuilder contextBuilder = new StringBuilder();
            for (int i = 0; i < dependencyResults.size(); i++) {
                contextBuilder.append("相关信息").append(i + 1).append(": ")
                        .append(dependencyResults.get(i)).append("\n");
            }

            List<ChatMessage> memoryMessages = getMemoryMessages();
            String result;

            if (memoryMessages.isEmpty()) {
                log.info("未找到历史记忆，使用基础LLM处理带依赖的任务: {}", intent);
                result = executeWithBasicLLMAndContext(intent, contextBuilder.toString());
            } else {
                log.info("找到{}条历史记忆，使用记忆增强处理带依赖的任务", memoryMessages.size());
                result = executeWithMemoryAndContext(intent, contextBuilder.toString(), memoryMessages);
            }

            try {
                updateMemory(buildEnhancedPrompt(intent, contextBuilder.toString()), result);
                log.debug("已更新对话记忆");
            } catch (Exception e) {
                log.warn("更新记忆失败，但不影响任务执行: {}", e.getMessage());
            }

            log.info("带依赖的记忆对话执行[{}]成功,结果长度: {}", intent, result.length());
            return result;

        } catch (Exception e) {
            log.error("执行带依赖的记忆任务失败: {}", intent, e);
            throw new RuntimeException("带依赖的记忆任务执行失败", e);
        }
    }

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

    private List<CompletableFuture<String>> waitForDependencies(String intent, int expectedSize) {
        List<CompletableFuture<String>> dependencyResults = new ArrayList<>();
        int retryCount = 0;
        final int maxRetries = 5; // 减少重试次数
        final long sleepInterval = 500; // 增加等待间隔

        while (retryCount < maxRetries) {
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

                Thread.sleep(sleepInterval);
                retryCount++;
                log.debug("记忆任务[{}]等待依赖结果中，重试次数: {}/{}", intent, retryCount, maxRetries);

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

    private boolean isTaskInDelayedQueue(String intent) {
        if (intent != null && !StrUtil.isBlank(intent)) {
            return intentDelayedQueueClien.containsTask(intent);
        } else {
            log.error("记忆任务intent为null");
            return false; // 不抛异常，返回false
        }
    }

    private List<ChatMessage> getMemoryMessages() {
        try {
            List<ChatMessage> messages = chatMemoryStore.getMessages(DEFAULT_MEMORY_ID);
            if (messages == null) {
                log.info("记忆存储返回null，使用空记忆");
                return new ArrayList<>();
            }

            int maxMemorySize = 10;
            if (messages.size() > maxMemorySize * 2) {
                messages = messages.subList(messages.size() - maxMemorySize * 2, messages.size());
                log.debug("记忆消息过多，截取最近{}条", maxMemorySize * 2);
            }

            log.debug("成功获取到{}条历史记忆消息", messages.size());
            return messages;
        } catch (Exception e) {
            log.warn("获取历史记忆失败，将使用空记忆作为降级方案: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    private void updateMemory(String userMessage, String aiResponse) {
        try {
            if (userMessage == null || aiResponse == null) {
                log.warn("用户消息或AI回复为null，跳过记忆更新");
                return;
            }

            chatMemoryStore.updateMessages(DEFAULT_MEMORY_ID, List.of(
                    UserMessage.from(userMessage)
                    , AiMessage.from(aiResponse)
            ));
            log.debug("成功更新对话记忆");
        } catch (Exception e) {
            log.warn("更新对话记忆失败: {}", e.getMessage());
        }
    }

    private String buildEnhancedPrompt(String originalIntent, String contextInfo) {
        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append("基于以下相关信息回答问题：\n\n");
        promptBuilder.append("相关信息：\n").append(contextInfo).append("\n");
        promptBuilder.append("用户问题：").append(originalIntent).append("\n\n");
        promptBuilder.append("请结合上述相关信息，给出准确、有用的回答。");
        return promptBuilder.toString();
    }

    public void clearMemory(String memoryId) {
        try {
            chatMemoryStore.deleteMessages(memoryId != null ? memoryId : DEFAULT_MEMORY_ID);
            log.info("已清除记忆ID[{}]的对话记忆", memoryId);
        } catch (Exception e) {
            log.error("清除对话记忆失败: {}", e.getMessage());
        }
    }

    public void clearDefaultMemory() {
        clearMemory(DEFAULT_MEMORY_ID);
    }

    @Override
    protected boolean shouldUpdateGraphOnFailure() {
        return true;
    }
}