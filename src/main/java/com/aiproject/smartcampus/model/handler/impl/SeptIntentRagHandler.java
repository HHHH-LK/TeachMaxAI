// =============================================================================
// 1. SeptIntentRagHandler.java - RAG处理器
// =============================================================================
package com.aiproject.smartcampus.model.handler.impl;

import com.aiproject.smartcampus.commons.client.ResultCilent;
import com.aiproject.smartcampus.commons.delayedtask.IntentBatchTask;
import com.aiproject.smartcampus.commons.delayedtask.IntentDelayedQueueClien;
import com.aiproject.smartcampus.commons.utils.CollectionUtils;
import com.aiproject.smartcampus.commons.utils.CreateDiagram;
import com.aiproject.smartcampus.model.handler.BaseEnhancedHandler;
import com.aiproject.smartcampus.model.prompts.UserPrompts;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.query.Query;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
@Slf4j
@RequiredArgsConstructor
public class SeptIntentRagHandler extends BaseEnhancedHandler {

    private final CreateDiagram createDiagram;
    private final ChatLanguageModel chatLanguageModel;
    private final IntentDelayedQueueClien intentDelayedQueueClient;
    private final ResultCilent resultClient;
    private final ContentRetriever contentRetriever;

    // 常量定义
    private static final int MAX_RETRY_TIMES = 10;
    private static final int SLEEP_TIME_MS = 200;
    private static final int TASK_TIMEOUT_SECONDS = 5;

    @Override
    protected String executeBusinessLogic(String intent, List<CompletableFuture<String>> result) {
        if (!StringUtils.hasText(intent)) {
            throw new IllegalArgumentException("意图参数不能为空");
        }

        log.info("RAG处理器开始执行业务逻辑，意图: {}", intent);

        // 判断是否需要前置条件(基于入度进行判断)
        int inDegree = createDiagram.getInDegree(intent);
        if (inDegree == 0) {
            return executeDirectTask(intent);
        } else {
            return executeWithDependencies(intent, result);
        }
    }

    private String executeDirectTask(String intent) {
        try {
            List<Content> retrieve = contentRetriever.retrieve(Query.from(intent));
            if (retrieve == null || retrieve.isEmpty()) {
                log.warn("任务[{}]未找到相关内容", intent);
                return "未找到相关内容";
            }

            String result = CollectionUtils.ContentSplicing(retrieve);
            log.info("执行[{}]成功，结果长度: {}", intent, result.length());
            return result;

        } catch (Exception e) {
            log.error("执行直接任务[{}]失败", intent, e);
            throw new RuntimeException("直接任务执行失败: " + intent, e);
        }
    }

    private String executeWithDependencies(String intent, List<CompletableFuture<String>> dependencies) {
        List<String> parentTasks = createDiagram.getParetents(intent);
        int expectedDependencyCount = parentTasks.size();

        if (dependencies != null && dependencies.size() == expectedDependencyCount &&
                areAllTasksCompleted(dependencies)) {
            return executeTaskWithCompletedDependencies(intent, dependencies);
        } else {
            return handlePendingDependencies(intent, dependencies, expectedDependencyCount);
        }
    }

    private String executeTaskWithCompletedDependencies(String intent,
                                                        List<CompletableFuture<String>> dependencies) {
        try {
            List<String> dependencyResults = collectDependencyResults(intent, dependencies);
            String combinedResults = buildDependencyResultString(dependencyResults);
            String enhancedQuery = generateEnhancedQuery(intent, combinedResults);

            log.info("生成增强查询成功，将执行: {}", enhancedQuery);

            List<Content> retrievedContent = contentRetriever.retrieve(Query.from(enhancedQuery));
            if (retrievedContent == null || retrievedContent.isEmpty()) {
                log.warn("任务[{}]未找到相关内容", intent);
                return "未找到相关内容";
            }

            String finalResult = CollectionUtils.ContentSplicing(retrievedContent);
            log.info("执行[{}]成功，结果长度: {}", intent, finalResult.length());
            return finalResult;

        } catch (Exception e) {
            log.error("执行依赖任务[{}]失败", intent, e);
            throw new RuntimeException("依赖任务执行失败: " + intent, e);
        }
    }

    private List<String> collectDependencyResults(String intent,
                                                  List<CompletableFuture<String>> dependencies)
            throws InterruptedException, ExecutionException, TimeoutException {

        List<String> dependencyResults = new ArrayList<>();
        for (int i = 0; i < dependencies.size(); i++) {
            try {
                String result = dependencies.get(i).get(TASK_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                dependencyResults.add(result != null ? result : "");
            } catch (TimeoutException te) {
                log.warn("前置任务{}等待超时，任务[{}]将重试", i, intent);
                throw te;
            } catch (ExecutionException | InterruptedException e) {
                log.error("获取前置任务{}结果异常，任务[{}]", i, intent, e);
                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
                throw e;
            }
        }
        return dependencyResults;
    }

    private String buildDependencyResultString(List<String> dependencyResults) {
        StringBuilder resultBuilder = new StringBuilder();
        for (int i = 0; i < dependencyResults.size(); i++) {
            resultBuilder.append("前置任务").append(i + 1).append("的结果为：")
                    .append(dependencyResults.get(i)).append("\n");
        }
        return resultBuilder.toString();
    }

    private String generateEnhancedQuery(String intent, String dependencyResults) {
        ChatResponse chatResponse = chatLanguageModel.chat(
                UserMessage.from(UserPrompts.chainUserPrompts(intent, dependencyResults))
        );
        return chatResponse.aiMessage().text();
    }

    private String handlePendingDependencies(String intent, List<CompletableFuture<String>> dependencies,
                                             int expectedCount) {
        log.info("任务[{}]缺失前置依赖，需要延迟执行", intent);

        List<CompletableFuture<String>> completedDependencies =
                waitForDependenciesToComplete(intent, expectedCount);

        if (completedDependencies != null && completedDependencies.size() == expectedCount &&
                areAllTasksCompleted(completedDependencies)) {
            addToDelayedQueue(intent, completedDependencies);
            return null;
        } else {
            log.warn("任务[{}]的依赖结果未准备完成，标记为依赖失败", intent);
            throw new RuntimeException("任务依赖缺失: " + intent);
        }
    }

    private List<CompletableFuture<String>> waitForDependenciesToComplete(String intent, int expectedCount) {
        int retryCount = 0;
        List<CompletableFuture<String>> results = new ArrayList<>();

        while (retryCount < MAX_RETRY_TIMES) {
            try {
                results.clear();
                List<String> parentTasks = createDiagram.getParetents(intent);

                for (String parentTask : parentTasks) {
                    List<CompletableFuture<String>> parentResults = resultClient.getResult(parentTask);
                    if (parentResults != null) {
                        results.addAll(parentResults);
                    }
                }

                if (results.size() == expectedCount && areAllTasksCompleted(results)) {
                    log.info("任务[{}]的依赖结果已准备完成", intent);
                    break;
                }

                Thread.sleep(SLEEP_TIME_MS);
                retryCount++;

                if (retryCount % 2 == 0) {
                    log.info("任务[{}]等待依赖结果中，重试次数: {}/{}", intent, retryCount, MAX_RETRY_TIMES);
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
        return results;
    }

    private void addToDelayedQueue(String intent, List<CompletableFuture<String>> dependencies) {
        if (!intentDelayedQueueClient.containsTask(intent)) {
            log.info("任务[{}]的依赖结果已准备完成，加入延时队列", intent);
            intentDelayedQueueClient.addTask(new IntentBatchTask(intent, dependencies, 500));
        } else {
            log.info("任务[{}]已存在延时队列中", intent);
        }
    }

    private boolean areAllTasksCompleted(List<CompletableFuture<String>> futures) {
        if (futures == null || futures.isEmpty()) {
            return false;
        }
        for (CompletableFuture<String> future : futures) {
            if (future == null || !future.isDone()) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected boolean shouldUpdateGraphOnFailure() {
        return true; // RAG任务失败时也要更新图状态，避免阻塞
    }
}