package com.aiproject.smartcampus.model.intent.handler.impl;

import com.aiproject.smartcampus.commons.client.ResultCilent;
import com.aiproject.smartcampus.commons.client.StatusCilent;
import com.aiproject.smartcampus.commons.delayedtask.IntentBatchTask;
import com.aiproject.smartcampus.commons.delayedtask.IntentDelayedQueueClien;
import com.aiproject.smartcampus.commons.utils.CreateDiagram;
import com.aiproject.smartcampus.model.intent.handler.EnhancedAutoRegisterHandler;
import com.aiproject.smartcampus.model.prompts.UserPrompts;
import com.aiproject.smartcampus.pojo.bo.TaskAction;
import com.github.xiaoymin.knife4j.core.util.StrUtil;
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
 * @program: SmartCampus
 * @description: LLM基础意图处理器
 * @author: lk
 * @create: 2025-05-29 20:05
 **/

@Service
@Slf4j
@RequiredArgsConstructor
public class StepIntentLLMChatResponseHandler extends EnhancedAutoRegisterHandler {

    private final CreateDiagram createDiagram;
    private final ChatLanguageModel chatLanguageModel;
    private final IntentDelayedQueueClien intentDelayedQueueClien;
    private final ResultCilent resultCilent;
    private final StatusCilent statusCilent;


    @Override
    public String run(String intent, List<CompletableFuture<String>> result) {
        log.info("基础LLM执行[{}]中", intent);
        try {
            // 更新任务状态为执行中
            TaskAction runningAction = TaskAction.statusUpdate(intent, "RUNNING");
            statusCilent.push(runningAction);
            String finalResult = null;
            int inDegree = createDiagram.getInDegree(intent);
            if (inDegree == 0) {
                // 无依赖任务，直接执行
                finalResult = executeDirectTask(intent);
            } else {
                // 有依赖任务，需要等待前置条件
                finalResult = executeWithDependencies(intent, result);
            }
            log.info("执行结果为{}", finalResult);
            return finalResult!=null?finalResult:null;
        } catch (Exception e) {
            log.error("执行任务[{}]时发生异常", intent, e);
            // 更新任务状态为失败
            TaskAction failedAction = TaskAction.statusUpdate(intent, "FAILED");
            statusCilent.push(failedAction);
            throw new RuntimeException("任务执行失败: " + intent, e);
        }
    }

    /**
     * 执行无依赖的直接任务
     */
    private String executeDirectTask(String intent) {
        ChatResponse chatResponse = chatLanguageModel.chat(UserMessage.from(intent));
        String result = chatResponse.aiMessage().text();
        log.info("执行[{}]成功,结果为[{}]", intent, result);
        // 更新状态为成功
        TaskAction successAction = TaskAction.statusUpdate(intent, "SUCCESS");
        statusCilent.push(successAction);
        // 减少相关任务的入度
        TaskAction decreaseAction = TaskAction.indegreeDecrease(intent);
        statusCilent.push(decreaseAction);

        return result;
    }

    /**
     * 执行有依赖的任务
     */
    private String executeWithDependencies(String intent, List<CompletableFuture<String>> result) {
        List<String> parentTasks = createDiagram.getParetents(intent);
        int expectedSize = parentTasks.size();
        // 检查是否有足够的前置结果
        if (result != null && result.size() == expectedSize && isOKTask(result)) {
            return executeTaskWithResults(intent, result);
        } else {
            handleMissingDependencies(intent, expectedSize);
            return null;
        }
    }

    /**
     * 判断依赖任务不为null
     * */
    private Boolean isOKTask(List<CompletableFuture<String>> result){
        for (CompletableFuture<String> future : result) {
            if (future == null) {
                return false;
            }
        }
        return true;
    }

    /**
     * 使用前置结果执行任务
     */
    private String executeTaskWithResults(String intent, List<CompletableFuture<String>> futures) {
        List<String> dependencyResults = new ArrayList<>();
        // 获取所有前置任务结果
        for (int i = 0; i < futures.size(); i++) {
            try {
                String result = futures.get(i).get(5, TimeUnit.SECONDS);
                dependencyResults.add(result);
            } catch (TimeoutException te) {
                log.warn("前置任务{}等待超时，任务[{}]将重试", i, intent);
                // 更新状态为等待重试
                TaskAction retryAction = TaskAction.statusUpdate(intent, "WAITING_RETRY");
                statusCilent.push(retryAction);
                throw new RuntimeException("前置任务执行超时", te);
            } catch (ExecutionException | InterruptedException e) {
                log.error("获取前置任务{}结果异常，任务[{}]", i, intent, e);
                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
                throw new RuntimeException("获取前置结果异常", e);
            }
        }
        // 格式化前置结果
        StringBuilder resultBuilder = new StringBuilder();
        for (int i = 0; i < dependencyResults.size(); i++) {
            resultBuilder.append("前置任务").append(i).append("的结果为")
                    .append(dependencyResults.get(i)).append("\n");
        }
        // 构建prompt并执行
        String prompt = UserPrompts.getTaskPrompt(intent, resultBuilder.toString());
        ChatResponse chatResponse = chatLanguageModel.chat(UserMessage.from(prompt));
        String finalResult = chatResponse.aiMessage().text();
        log.info("执行[{}]成功,结果为[{}]", intent, finalResult);
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
        log.info("执行[{}]失败,前置条件未满足,将推迟处理", intent);
        // 更新状态为等待依赖
        TaskAction waitingAction = TaskAction.statusUpdate(intent, "WAITING_DEPENDENCIES");
        statusCilent.push(waitingAction);
        List<CompletableFuture<String>> dependencyResults = waitForDependencies(intent, expectedSize);
        if (dependencyResults != null && !dependencyResults.isEmpty() && isOKTask(dependencyResults)) {
            // 检查是否任务已经在延时队列中，避免重复提交
            if (!isTaskInDelayedQueue(intent)) {
                intentDelayedQueueClien.addTask(new IntentBatchTask(intent, dependencyResults, 500));
                log.info("[{}]任务已加入到延时队列中，即将重新执行", intent);
            } else {
                log.info("[{}]任务已在延时队列中，跳过重复提交", intent);
            }
        } else {
            log.warn("获取任务[{}]依赖结果失败，前置任务可能丢失或异常", intent);
            TaskAction failedAction = TaskAction.statusUpdate(intent, "DEPENDENCIES_FAILED");
            statusCilent.push(failedAction);
            throw new RuntimeException("任务依赖缺失: " + intent);
        }
    }

    /**
     * 等待依赖任务完成
     */
    private List<CompletableFuture<String>> waitForDependencies(String intent, int expectedSize) {
        List<CompletableFuture<String>> dependencyResults = null;
        int retryCount = 0;
        final int maxRetries = 10;
        final long sleepInterval = 200;

        while (retryCount < maxRetries) {
            try {
                //获取父依赖
                List<String> paretents = createDiagram.getParetents(intent);
                paretents.stream().map(parent -> resultCilent.getResult(parent))
                        .forEach(s-> dependencyResults.addAll(s));
                // 检查结果是否完整
                if (dependencyResults != null && dependencyResults.size() == expectedSize && isOKTask(dependencyResults)) {
                    log.info("任务[{}]的依赖结果已准备完成", intent);
                    break;
                }
                // 等待一段时间后重试
                Thread.sleep(sleepInterval);
                retryCount++;
                // 每2次重试记录一次日志
                if (retryCount % 2== 0) {
                    log.info("任务[{}]等待依赖结果中，重试次数: {}/{}", intent, retryCount, maxRetries);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("等待依赖结果被中断: {}", intent, e);
                break;
            } catch (Exception e) {
                log.error("获取依赖结果时发生异常: {}", intent, e);
                retryCount++;
            }
        }

        return dependencyResults;
    }

    /**
     * 检查任务是否已在延时队列中（需要根据实际的延时队列实现）
     */
    private boolean isTaskInDelayedQueue(String intent) {
        if(intent!=null&& !StrUtil.isBlank(intent)){
            return intentDelayedQueueClien.containsTask(intent);
        }else {
            log.error("任务intent为null");
            throw new RuntimeException("任务id为空");
        }
    }
}