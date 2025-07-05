// =============================================================================
// 3. StepIntentLLMChatResponseHandler.java - LLM基础处理器
// =============================================================================
package com.aiproject.smartcampus.model.handler.impl;

import com.aiproject.smartcampus.commons.client.ResultCilent;
import com.aiproject.smartcampus.commons.delayedtask.IntentBatchTask;
import com.aiproject.smartcampus.commons.delayedtask.IntentDelayedQueueClien;
import com.aiproject.smartcampus.commons.utils.CreateDiagram;
import com.aiproject.smartcampus.model.handler.BaseEnhancedHandler;
import com.aiproject.smartcampus.model.prompts.UserPrompts;
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

@Service
@Slf4j
@RequiredArgsConstructor
public class StepIntentLLMChatResponseHandler extends BaseEnhancedHandler {

    private final CreateDiagram createDiagram;
    private final ChatLanguageModel chatLanguageModel;
    private final IntentDelayedQueueClien intentDelayedQueueClien;
    private final ResultCilent resultCilent;

    @Override
    protected String executeBusinessLogic(String intent, List<CompletableFuture<String>> result) {
        log.info("基础LLM执行[{}]中", intent);

        int inDegree = createDiagram.getInDegree(intent);
        if (inDegree == 0) {
            return executeDirectTask(intent);
        } else {
            return executeWithDependencies(intent, result);
        }
    }

    private String executeDirectTask(String intent) {
        ChatResponse chatResponse = chatLanguageModel.chat(UserMessage.from(intent));
        String result = chatResponse.aiMessage().text();
        log.info("执行[{}]成功,结果为[{}]", intent, result);
        return result;
    }

    private String executeWithDependencies(String intent, List<CompletableFuture<String>> result) {
        List<String> parentTasks = createDiagram.getParetents(intent);
        int expectedSize = parentTasks.size();

        if (result != null && result.size() == expectedSize && isOKTask(result)) {
            return executeTaskWithResults(intent, result);
        } else {
            return handleMissingDependencies(intent, expectedSize);
        }
    }

    private Boolean isOKTask(List<CompletableFuture<String>> result) {
        for (CompletableFuture<String> future : result) {
            if (future == null) {
                return false;
            }
        }
        return true;
    }

    private String executeTaskWithResults(String intent, List<CompletableFuture<String>> futures) {
        List<String> dependencyResults = new ArrayList<>();

        for (int i = 0; i < futures.size(); i++) {
            try {
                String result = futures.get(i).get(5, TimeUnit.SECONDS);
                dependencyResults.add(result);
            } catch (TimeoutException te) {
                log.warn("前置任务{}等待超时，任务[{}]将重试", i, intent);
                throw new RuntimeException("前置任务执行超时", te);
            } catch (ExecutionException | InterruptedException e) {
                log.error("获取前置任务{}结果异常，任务[{}]", i, intent, e);
                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
                throw new RuntimeException("获取前置结果异常", e);
            }
        }

        StringBuilder resultBuilder = new StringBuilder();
        for (int i = 0; i < dependencyResults.size(); i++) {
            resultBuilder.append("前置任务").append(i).append("的结果为")
                    .append(dependencyResults.get(i)).append("\n");
        }

        String prompt = UserPrompts.getTaskPrompt(intent, resultBuilder.toString());
        ChatResponse chatResponse = chatLanguageModel.chat(UserMessage.from(prompt));
        String finalResult = chatResponse.aiMessage().text();
        log.info("执行[{}]成功,结果为[{}]", intent, finalResult);
        return finalResult;
    }

    private String handleMissingDependencies(String intent, int expectedSize) {
        log.info("执行[{}]失败,前置条件未满足,将推迟处理", intent);

        List<CompletableFuture<String>> dependencyResults = waitForDependencies(intent, expectedSize);
        if (dependencyResults != null && !dependencyResults.isEmpty() && isOKTask(dependencyResults)) {
            if (!isTaskInDelayedQueue(intent)) {
                intentDelayedQueueClien.addTask(new IntentBatchTask(intent, dependencyResults, 500));
                log.info("[{}]任务已加入到延时队列中，即将重新执行", intent);
            } else {
                log.info("[{}]任务已在延时队列中，跳过重复提交", intent);
            }
            return null;
        } else {
            log.warn("获取任务[{}]依赖结果失败，前置任务可能丢失或异常", intent);
            throw new RuntimeException("任务依赖缺失: " + intent);
        }
    }

    private List<CompletableFuture<String>> waitForDependencies(String intent, int expectedSize) {
        List<CompletableFuture<String>> dependencyResults = new ArrayList<>();
        int retryCount = 0;
        final int maxRetries = 10;
        final long sleepInterval = 200;

        while (retryCount < maxRetries) {
            try {
                List<String> paretents = createDiagram.getParetents(intent);
                dependencyResults.clear();

                for (String parent : paretents) {
                    List<CompletableFuture<String>> parentResults = resultCilent.getResult(parent);
                    if (parentResults != null) {
                        dependencyResults.addAll(parentResults);
                    }
                }

                if (dependencyResults.size() == expectedSize && isOKTask(dependencyResults)) {
                    log.info("任务[{}]的依赖结果已准备完成", intent);
                    break;
                }

                Thread.sleep(sleepInterval);
                retryCount++;

                if (retryCount % 2 == 0) {
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

    private boolean isTaskInDelayedQueue(String intent) {
        if (intent != null && !StrUtil.isBlank(intent)) {
            return intentDelayedQueueClien.containsTask(intent);
        } else {
            log.error("任务intent为null");
            throw new RuntimeException("任务id为空");
        }
    }

    @Override
    protected boolean shouldUpdateGraphOnFailure() {
        return true; // LLM任务失败时也要更新图状态
    }
}