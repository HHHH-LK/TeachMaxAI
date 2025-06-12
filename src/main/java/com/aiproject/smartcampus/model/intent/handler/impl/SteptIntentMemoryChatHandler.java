package com.aiproject.smartcampus.model.intent.handler.impl;

import com.aiproject.smartcampus.commons.client.ResultCilent;
import com.aiproject.smartcampus.commons.client.StatusCilent;
import com.aiproject.smartcampus.commons.delayedtask.IntentBatchTask;
import com.aiproject.smartcampus.commons.delayedtask.IntentDelayedQueueClien;
import com.aiproject.smartcampus.commons.utils.CreateDiagram;
import com.aiproject.smartcampus.model.intent.handler.AutoRegisterHandler;
import com.aiproject.smartcampus.pojo.bo.TaskAction;
import com.github.xiaoymin.knife4j.core.util.StrUtil;
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

/**
 * @program: SmartCampus
 * @description: 记忆处理处理器
 * @author: lk
 * @create: 2025-05-28 13:33
 **/

@Service
@Slf4j
@RequiredArgsConstructor
public class SteptIntentMemoryChatHandler extends AutoRegisterHandler {

    private final CreateDiagram createDiagram;
    private final ChatLanguageModel chatLanguageModel;
    private final ChatMemoryStore chatMemoryStore;
    private final IntentDelayedQueueClien intentDelayedQueueClien;
    private final ResultCilent resultCilent;
    private final StatusCilent statusCilent;

    private final String functionDescription = "基于对话记忆回答用户问题，处理上下文相关的查询";

    // 默认记忆ID，可以根据用户会话或任务分组 todo 后续修改成为user指定id
    private static final String DEFAULT_MEMORY_ID = "smart_campus_memory";
    // 系统提示词
    private static final String MEMORY_SYSTEM_PROMPT = "你是一个智能助手，能够基于之前的对话记忆为用户提供准确的回答。请根据历史对话上下文和当前问题，给出恰当的回复。";

    @Override
    public String run(String intent, List<CompletableFuture<String>> result) {
        log.info("基于记忆的对话处理器执行[{}]中", intent);
        try {
            // 更新任务状态为执行中
            TaskAction runningAction = TaskAction.statusUpdate(intent, "RUNNING");
            statusCilent.push(runningAction);

            String finalResult = null;
            int inDegree = createDiagram.getInDegree(intent);

            if (inDegree == 0) {
                // 无依赖任务，直接执行记忆对话
                finalResult = executeDirectMemoryTask(intent);
            } else {
                // 有依赖任务，需要等待前置条件
                finalResult = executeMemoryWithDependencies(intent, result);
            }

            log.info("记忆对话执行结果为{}", finalResult);
            return finalResult != null ? finalResult : null;
        } catch (Exception e) {
            log.error("执行记忆任务[{}]时发生异常", intent, e);
            // 更新任务状态为失败
            TaskAction failedAction = TaskAction.statusUpdate(intent, "FAILED");
            statusCilent.push(failedAction);
            throw new RuntimeException("记忆任务执行失败: " + intent, e);
        }
    }

    /**
     * 执行无依赖的直接记忆任务
     */
    private String executeDirectMemoryTask(String intent) {
        try {
            // 获取历史对话记忆
            List<ChatMessage> memoryMessages = getMemoryMessages();

            // 构建完整的对话消息列表
            List<ChatMessage> messages = new ArrayList<>();
            messages.add(SystemMessage.from(MEMORY_SYSTEM_PROMPT));
            messages.addAll(memoryMessages);
            messages.add(UserMessage.from(intent));

            // 执行对话
            ChatResponse chatResponse = chatLanguageModel.chat(messages);
            String result = chatResponse.aiMessage().text();

            // 将用户问题和AI回答添加到记忆中
            updateMemory(intent, result);

            log.info("记忆对话执行[{}]成功,结果为[{}]", intent, result);

            // 更新状态为成功
            TaskAction successAction = TaskAction.statusUpdate(intent, "SUCCESS");
            statusCilent.push(successAction);

            // 减少相关任务的入度
            TaskAction decreaseAction = TaskAction.indegreeDecrease(intent);
            statusCilent.push(decreaseAction);

            return result;
        } catch (Exception e) {
            log.error("执行直接记忆任务失败: {}", intent, e);
            throw new RuntimeException("记忆对话执行失败", e);
        }
    }

    /**
     * 执行有依赖的记忆任务
     */
    private String executeMemoryWithDependencies(String intent, List<CompletableFuture<String>> result) {
        List<String> parentTasks = createDiagram.getParetents(intent);
        int expectedSize = parentTasks.size();

        // 检查是否有足够的前置结果
        if (result != null && result.size() == expectedSize && isOKTask(result)) {
            return executeMemoryTaskWithResults(intent, result);
        } else {
            handleMissingDependencies(intent, expectedSize);
            return null;
        }
    }

    /**
     * 判断依赖任务不为null
     */
    private Boolean isOKTask(List<CompletableFuture<String>> result) {
        for (CompletableFuture<String> future : result) {
            if (future == null) {
                return false;
            }
        }
        return true;
    }

    /**
     * 使用前置结果执行记忆任务
     */
    private String executeMemoryTaskWithResults(String intent, List<CompletableFuture<String>> futures) {
        List<String> dependencyResults = new ArrayList<>();

        // 获取所有前置任务结果
        for (int i = 0; i < futures.size(); i++) {
            try {
                String result = futures.get(i).get(5, TimeUnit.SECONDS);
                dependencyResults.add(result);
            } catch (TimeoutException te) {
                log.warn("前置任务{}等待超时，记忆任务[{}]将重试", i, intent);
                TaskAction retryAction = TaskAction.statusUpdate(intent, "WAITING_RETRY");
                statusCilent.push(retryAction);
                throw new RuntimeException("前置任务执行超时", te);
            } catch (ExecutionException | InterruptedException e) {
                log.error("获取前置任务{}结果异常，记忆任务[{}]", i, intent, e);
                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
                throw new RuntimeException("获取前置结果异常", e);
            }
        }

        // 格式化前置结果
        StringBuilder contextBuilder = new StringBuilder();
        for (int i = 0; i < dependencyResults.size(); i++) {
            contextBuilder.append("相关信息").append(i + 1).append(": ")
                    .append(dependencyResults.get(i)).append("\n");
        }

        // 获取历史对话记忆
        List<ChatMessage> memoryMessages = getMemoryMessages();

        // 构建增强的prompt
        String enhancedPrompt = buildEnhancedPrompt(intent, contextBuilder.toString());

        // 构建完整的对话消息列表
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(SystemMessage.from(MEMORY_SYSTEM_PROMPT));
        messages.addAll(memoryMessages);
        messages.add(UserMessage.from(enhancedPrompt));

        // 执行对话
        ChatResponse chatResponse = chatLanguageModel.chat(messages);
        String finalResult = chatResponse.aiMessage().text();

        // 将增强的问题和AI回答添加到记忆中
        updateMemory(enhancedPrompt, finalResult);

        log.info("记忆对话执行[{}]成功,结果为[{}]", intent, finalResult);

        // 更新状态
        TaskAction successAction = TaskAction.statusUpdate(intent, "SUCCESS");
        statusCilent.push(successAction);

        // 减少入度
        TaskAction decreaseAction = TaskAction.indegreeDecrease(intent);
        statusCilent.push(decreaseAction);

        return finalResult;
    }

    /**
     * 处理缺失依赖的情况
     */
    private void handleMissingDependencies(String intent, int expectedSize) {
        log.info("记忆任务[{}]执行失败,前置条件未满足,将推迟处理", intent);

        // 更新状态为等待依赖
        TaskAction waitingAction = TaskAction.statusUpdate(intent, "WAITING_DEPENDENCIES");
        statusCilent.push(waitingAction);

        List<CompletableFuture<String>> dependencyResults = waitForDependencies(intent, expectedSize);

        if (dependencyResults != null && !dependencyResults.isEmpty() && isOKTask(dependencyResults)) {
            // 检查是否任务已经在延时队列中，避免重复提交
            if (!isTaskInDelayedQueue(intent)) {
                intentDelayedQueueClien.addTask(new IntentBatchTask(intent, dependencyResults, 500));
                log.info("记忆任务[{}]已加入到延时队列中，即将重新执行", intent);
            } else {
                log.info("记忆任务[{}]已在延时队列中，跳过重复提交", intent);
            }
        } else {
            log.warn("获取记忆任务[{}]依赖结果失败，前置任务可能丢失或异常", intent);
            TaskAction failedAction = TaskAction.statusUpdate(intent, "DEPENDENCIES_FAILED");
            statusCilent.push(failedAction);
            throw new RuntimeException("记忆任务依赖缺失: " + intent);
        }
    }

    /**
     * 等待依赖任务完成
     */
    private List<CompletableFuture<String>> waitForDependencies(String intent, int expectedSize) {
        List<CompletableFuture<String>> dependencyResults = new ArrayList<>();
        int retryCount = 0;
        final int maxRetries = 10;
        final long sleepInterval = 200;

        while (retryCount < maxRetries) {
            try {
                // 获取父依赖
                List<String> parents = createDiagram.getParetents(intent);
                dependencyResults.clear();

                for (String parent : parents) {
                    List<CompletableFuture<String>> parentResults = resultCilent.getResult(parent);
                    if (parentResults != null) {
                        dependencyResults.addAll(parentResults);
                    }
                }

                // 检查结果是否完整
                if (dependencyResults.size() == expectedSize && isOKTask(dependencyResults)) {
                    log.info("记忆任务[{}]的依赖结果已准备完成", intent);
                    break;
                }

                // 等待一段时间后重试
                Thread.sleep(sleepInterval);
                retryCount++;

                // 每2次重试记录一次日志
                if (retryCount % 2 == 0) {
                    log.info("记忆任务[{}]等待依赖结果中，重试次数: {}/{}", intent, retryCount, maxRetries);
                }
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
            throw new RuntimeException("记忆任务id为空");
        }
    }

    /**
     * 获取历史对话记忆
     */
    private List<ChatMessage> getMemoryMessages() {
        try {
            List<ChatMessage> messages = chatMemoryStore.getMessages(DEFAULT_MEMORY_ID);
            if (messages == null) {
                messages = new ArrayList<>();
            }

            // 限制记忆条数，避免上下文过长
            int maxMemorySize = 10; // 最多保留10轮对话
            if (messages.size() > maxMemorySize * 2) { // 每轮对话包含用户消息和AI消息
                messages = messages.subList(messages.size() - maxMemorySize * 2, messages.size());
            }

            log.debug("获取到{}条历史记忆消息", messages.size());
            return messages;
        } catch (Exception e) {
            log.warn("获取历史记忆失败，将使用空记忆: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * 更新对话记忆
     */
    private void updateMemory(String userMessage, String aiResponse) {
        try {
            // 添加用户消息
            chatMemoryStore.updateMessages(DEFAULT_MEMORY_ID, List.of(
                    UserMessage.from(userMessage),
                    dev.langchain4j.data.message.AiMessage.from(aiResponse)
            ));

            log.debug("已更新对话记忆");
        } catch (Exception e) {
            log.warn("更新对话记忆失败: {}", e.getMessage());
        }
    }

    /**
     * 构建增强的prompt
     */
    private String buildEnhancedPrompt(String originalIntent, String contextInfo) {
        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append("基于以下相关信息回答问题：\n\n");
        promptBuilder.append("相关信息：\n").append(contextInfo).append("\n");
        promptBuilder.append("用户问题：").append(originalIntent).append("\n\n");
        promptBuilder.append("请结合上述相关信息和历史对话记忆，给出准确、有用的回答。");

        return promptBuilder.toString();
    }

    /**
     * 清除指定记忆ID的对话记忆
     */
    public void clearMemory(String memoryId) {
        try {
            chatMemoryStore.deleteMessages(memoryId != null ? memoryId : DEFAULT_MEMORY_ID);
            log.info("已清除记忆ID[{}]的对话记忆", memoryId);
        } catch (Exception e) {
            log.error("清除对话记忆失败: {}", e.getMessage());
        }
    }

    /**
     * 清除默认记忆
     */
    public void clearDefaultMemory() {
        clearMemory(DEFAULT_MEMORY_ID);
    }
}