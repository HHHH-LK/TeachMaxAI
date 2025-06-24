package com.aiproject.smartcampus.model.intent.handler.impl;

import com.aiproject.smartcampus.commons.client.ResultCilent;

import com.aiproject.smartcampus.commons.client.StatusCilent;
import com.aiproject.smartcampus.commons.delayedtask.IntentBatchTask;
import com.aiproject.smartcampus.commons.delayedtask.IntentDelayedQueueClien;
import com.aiproject.smartcampus.commons.utils.CollectionUtils;
import com.aiproject.smartcampus.commons.utils.CreateDiagram;
import com.aiproject.smartcampus.model.intent.handler.EnhancedAutoRegisterHandler;
import com.aiproject.smartcampus.model.prompts.UserPrompts;
import com.aiproject.smartcampus.pojo.bo.TaskAction;
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

/**
 * @program: SmartCampus
 * @description: 增强检索处理器
 * @author: lk
 * @create: 2025-05-28 13:37
 **/
@Service
@Slf4j
@RequiredArgsConstructor
public class SeptIntentRagHandler extends EnhancedAutoRegisterHandler {

    private final CreateDiagram createDiagram;
    private final ChatLanguageModel chatLanguageModel;
    private final IntentDelayedQueueClien intentDelayedQueueClient;
    private final ResultCilent resultClient;
    private final StatusCilent statusClient;
    private final ContentRetriever contentRetriever;

    // 常量定义
    private static final int MAX_RETRY_TIMES = 10;
    private static final int SLEEP_TIME_MS = 200;
    private static final int TASK_TIMEOUT_SECONDS = 5;

    @Override
    public String run(String intent, List<CompletableFuture<String>> result) {
        if (!StringUtils.hasText(intent)) {
            log.error("意图参数为空");
            throw new IllegalArgumentException("意图参数不能为空");
        }

        log.info("RAG处理器开始执行，意图: {}", intent);

        try {
            // 更新任务状态
            updateTaskStatus(intent, "RUNNING");

            String results;
            // 判断是否需要前置条件(基于入度进行判断)
            int inDegree = createDiagram.getInDegree(intent);
            if (inDegree == 0) {
                // 任务无需前置要求
                results = executeDirectTask(intent);
            } else {
                // 任务需要前置要求
                results = executeWithDependencies(intent, result);
            }

            log.info("任务[{}]执行完成，结果长度: {}", intent,
                    results != null ? results.length() : 0);
            return results;

        } catch (Exception e) {
            log.error("任务[{}]执行失败", intent, e);
            updateTaskStatus(intent, "FAILED");
            throw new RuntimeException("任务执行失败: " + intent, e);
        }
    }

    /**
     * 执行无依赖的直接任务
     */
    private String executeDirectTask(String intent) {
        try {
            List<Content> retrieve = contentRetriever.retrieve(Query.from(intent));
            if (retrieve == null || retrieve.isEmpty()) {
                log.warn("任务[{}]未找到相关内容", intent);
                return "未找到相关内容";
            }

            // 构建结果
            String result = CollectionUtils.ContentSplicing(retrieve);
            log.info("执行[{}]成功，结果长度: {}", intent, result.length());

            // 更新状态并减少相关任务的入度
            updateTaskStatus(intent, "SUCCESS");
            decreaseTaskIndegree(intent);

            return result;

        } catch (Exception e) {
            log.error("执行直接任务[{}]失败", intent, e);
            updateTaskStatus(intent, "FAILED");
            throw new RuntimeException("直接任务执行失败: " + intent, e);
        }
    }

    /**
     * 执行有依赖的任务
     */
    private String executeWithDependencies(String intent, List<CompletableFuture<String>> dependencies) {
        List<String> parentTasks = createDiagram.getParetents(intent);
        int expectedDependencyCount = parentTasks.size();

        if (dependencies != null && dependencies.size() == expectedDependencyCount &&
                areAllTasksCompleted(dependencies)) {
            // 前置任务执行完毕
            return executeTaskWithCompletedDependencies(intent, dependencies);
        } else {
            // 前置任务未执行完毕，延迟执行
            handlePendingDependencies(intent, dependencies, expectedDependencyCount);
            return null;
        }
    }

    /**
     * 执行依赖已完成的任务
     */
    private String executeTaskWithCompletedDependencies(String intent,
                                                        List<CompletableFuture<String>> dependencies) {
        try {
            // 获取依赖结果
            List<String> dependencyResults = collectDependencyResults(intent, dependencies);

            // 构建依赖结果字符串
            String combinedResults = buildDependencyResultString(dependencyResults);

            // 关联前置结果与当前需求
            String enhancedQuery = generateEnhancedQuery(intent, combinedResults);
            log.info("生成增强查询成功，将执行: {}", enhancedQuery);

            // 执行RAG查询
            List<Content> retrievedContent = contentRetriever.retrieve(Query.from(enhancedQuery));
            if (retrievedContent == null || retrievedContent.isEmpty()) {
                log.warn("任务[{}]未找到相关内容", intent);
                return "未找到相关内容";
            }

            String finalResult = CollectionUtils.ContentSplicing(retrievedContent);
            log.info("执行[{}]成功，结果长度: {}", intent, finalResult.length());

            // 更新状态并减少相关任务的入度
            updateTaskStatus(intent, "SUCCESS");
            decreaseTaskIndegree(intent);

            return finalResult;

        } catch (Exception e) {
            log.error("执行依赖任务[{}]失败", intent, e);
            updateTaskStatus(intent, "FAILED");
            throw new RuntimeException("依赖任务执行失败: " + intent, e);
        }
    }

    /**
     * 收集依赖任务结果
     */
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
                updateTaskStatus(intent, "WAITING_RETRY");
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

    /**
     * 构建依赖结果字符串
     */
    private String buildDependencyResultString(List<String> dependencyResults) {
        StringBuilder resultBuilder = new StringBuilder();
        for (int i = 0; i < dependencyResults.size(); i++) {
            resultBuilder.append("前置任务").append(i + 1).append("的结果为：")
                    .append(dependencyResults.get(i)).append("\n");
        }
        return resultBuilder.toString();
    }

    /**
     * 生成增强查询
     */
    private String generateEnhancedQuery(String intent, String dependencyResults) {
        ChatResponse chatResponse = chatLanguageModel.chat(
                UserMessage.from(UserPrompts.chainUserPrompts(intent, dependencyResults))
        );
        return chatResponse.aiMessage().text();
    }

    /**
     * 处理待定依赖
     */
    private void handlePendingDependencies(String intent, List<CompletableFuture<String>> dependencies,
                                           int expectedCount) {
        log.info("任务[{}]缺失前置依赖，需要延迟执行", intent);
        updateTaskStatus(intent, "WAITING_DEPENDENCY");

        // 等待依赖执行完成
        List<CompletableFuture<String>> completedDependencies =
                waitForDependenciesToComplete(intent, expectedCount);

        if (completedDependencies != null && completedDependencies.size() == expectedCount &&
                areAllTasksCompleted(completedDependencies)) {
            // 加入延时队列
            addToDelayedQueue(intent, completedDependencies);
        } else {
            log.warn("任务[{}]的依赖结果未准备完成，标记为依赖失败", intent);
            updateTaskStatus(intent, "DEPENDENCIES_FAILED");
            throw new RuntimeException("任务依赖缺失: " + intent);
        }
    }

    /**
     * 等待依赖完成
     */
    private List<CompletableFuture<String>> waitForDependenciesToComplete(String intent, int expectedCount) {
        int retryCount = 0;
        List<CompletableFuture<String>> results = new ArrayList<>();

        while (retryCount < MAX_RETRY_TIMES) {
            try {
                results.clear();

                // 获取父任务列表
                List<String> parentTasks = createDiagram.getParetents(intent);

                // 修复：正确获取每个父任务的结果
                for (String parentTask : parentTasks) {
                    List<CompletableFuture<String>> parentResults = resultClient.getResult(parentTask);
                    if (parentResults != null) {
                        results.addAll(parentResults);
                    }
                }

                // 判断依赖是否执行完成
                if (results.size() == expectedCount && areAllTasksCompleted(results)) {
                    log.info("任务[{}]的依赖结果已准备完成", intent);
                    break;
                }

                Thread.sleep(SLEEP_TIME_MS);
                retryCount++;

                // 每2次重试记录一次日志
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

    /**
     * 添加到延时队列
     */
    private void addToDelayedQueue(String intent, List<CompletableFuture<String>> dependencies) {
        if (!intentDelayedQueueClient.containsTask(intent)) {
            log.info("任务[{}]的依赖结果已准备完成，加入延时队列", intent);
            intentDelayedQueueClient.addTask(new IntentBatchTask(intent, dependencies, 500));
        } else {
            log.info("任务[{}]已存在延时队列中", intent);
        }
    }

    /**
     * 判断所有任务是否完成
     */
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

    /**
     * 更新任务状态
     */
    private void updateTaskStatus(String intent, String status) {
        try {
            TaskAction action = TaskAction.statusUpdate(intent, status);
            statusClient.push(action);
        } catch (Exception e) {
            log.error("更新任务[{}]状态为[{}]失败", intent, status, e);
        }
    }

    /**
     * 减少任务入度
     */
    private void decreaseTaskIndegree(String intent) {
        try {
            TaskAction decreaseAction = TaskAction.indegreeDecrease(intent);
            statusClient.push(decreaseAction);
        } catch (Exception e) {
            log.error("减少任务[{}]入度失败", intent, e);
        }
    }
}