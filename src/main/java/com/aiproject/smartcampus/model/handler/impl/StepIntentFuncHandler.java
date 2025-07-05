package com.aiproject.smartcampus.model.handler.impl;

import com.aiproject.smartcampus.commons.client.ResultCilent;
import com.aiproject.smartcampus.commons.delayedtask.IntentBatchTask;
import com.aiproject.smartcampus.commons.delayedtask.IntentDelayedQueueClien;
import com.aiproject.smartcampus.commons.utils.CreateDiagram;
import com.aiproject.smartcampus.commons.utils.JsonUtils;
import com.aiproject.smartcampus.model.handler.BaseEnhancedHandler;
import com.aiproject.smartcampus.model.prompts.UserPrompts;
import com.aiproject.smartcampus.model.toollist.ChatAgentToolList;
import com.github.xiaoymin.knife4j.core.util.StrUtil;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.ChatRequestParameters;
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
import java.util.concurrent.atomic.AtomicReference;

import static com.aiproject.smartcampus.contest.CommonContest.TOOL_SCAN_NAME;

@Service
@Slf4j
@RequiredArgsConstructor
public class StepIntentFuncHandler extends BaseEnhancedHandler {

    private final ChatAgentToolList chatAgentToolList;
    private final ChatLanguageModel chatLanguageModel;
    private final CreateDiagram createDiagram;
    private final ResultCilent resultCilent;
    private final IntentDelayedQueueClien intentDelayedQueueClien;

    @Override
    protected String executeBusinessLogic(String intent, List<CompletableFuture<String>> result) {
        log.info("工具函数处理器执行[{}]中", intent);

        int inDegree = createDiagram.getInDegree(intent);
        if (inDegree == 0) {
            return executeDirectTask(intent);
        } else {
            return executeWithDependencies(intent, result);
        }
    }

    private String executeDirectTask(String intent) {
        try {
            ChatResponse chatResponse = chatLanguageModel.doChat(ChatRequest.builder()
                    .messages(UserMessage.from(intent))
                    .parameters(ChatRequestParameters.builder()
                            .toolSpecifications(chatAgentToolList.getTools())
                            .build())
                    .build());

            String result = executeToolsFromResponse(chatResponse);
            log.info("执行工具[{}]成功,结果为[{}]", intent, result);
            return result;

        } catch (Exception e) {
            log.error("执行直接工具任务[{}]失败", intent, e);
            throw new RuntimeException("工具执行失败", e);
        }
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

    private String executeTaskWithResults(String intent, List<CompletableFuture<String>> futures) {
        List<String> dependencyResults = new ArrayList<>();

        for (int i = 0; i < futures.size(); i++) {
            try {
                String result = futures.get(i).get(5, TimeUnit.SECONDS);
                dependencyResults.add(result);
            } catch (TimeoutException te) {
                log.warn("前置任务{}等待超时，工具任务[{}]将重试", i, intent);
                throw new RuntimeException("前置任务执行超时", te);
            } catch (ExecutionException | InterruptedException e) {
                log.error("获取前置任务{}结果异常，工具任务[{}]", i, intent, e);
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

        ChatResponse chatResponse = chatLanguageModel.doChat(ChatRequest.builder()
                .messages(UserMessage.from(prompt))
                .parameters(ChatRequestParameters.builder()
                        .toolSpecifications(chatAgentToolList.getTools())
                        .build())
                .build());

        String finalResult = executeToolsFromResponse(chatResponse);
        log.info("执行工具[{}]成功,结果为[{}]", intent, finalResult);
        return finalResult;
    }

    private String executeToolsFromResponse(ChatResponse chatResponse) {
        AtomicReference<String> chatResponseRef = new AtomicReference<>("");

        log.info("执行工具调用中...");
        List<ToolExecutionRequest> toolExecutionRequests = chatResponse.aiMessage().toolExecutionRequests();

        try {
            if (toolExecutionRequests != null && !toolExecutionRequests.isEmpty()) {
                StringBuilder resultBuilder = new StringBuilder();
                for (ToolExecutionRequest toolExecutionRequest : toolExecutionRequests) {
                    String toolResult = executeCustomTool(toolExecutionRequest);
                    resultBuilder.append(toolResult).append("\n");
                }
                chatResponseRef.set(resultBuilder.toString().trim());
            } else {
                chatResponseRef.set(chatResponse.aiMessage().text());
            }
        } catch (Exception ex) {
            log.error("执行工具调用时发生异常", ex);
            throw new RuntimeException("工具调用执行失败", ex);
        }

        return chatResponseRef.get();
    }

    private String handleMissingDependencies(String intent, int expectedSize) {
        log.info("执行工具[{}]失败,前置条件未满足,将推迟处理", intent);

        List<CompletableFuture<String>> dependencyResults = waitForDependencies(intent, expectedSize);
        if (!dependencyResults.isEmpty() && isOKTask(dependencyResults)) {
            if (!isTaskInDelayedQueue(intent)) {
                intentDelayedQueueClien.addTask(new IntentBatchTask(intent, dependencyResults, 500));
                log.info("工具[{}]任务已加入到延时队列中，即将重新执行", intent);
            } else {
                log.info("工具[{}]任务已在延时队列中，跳过重复提交", intent);
            }
            return null;
        } else {
            log.warn("获取工具任务[{}]依赖结果失败，前置任务可能丢失或异常", intent);
            throw new RuntimeException("工具任务依赖缺失: " + intent);
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
                    log.info("工具任务[{}]的依赖结果已准备完成", intent);
                    break;
                }

                Thread.sleep(sleepInterval);
                retryCount++;

                if (retryCount % 2 == 0) {
                    log.info("工具任务[{}]等待依赖结果中，重试次数: {}/{}", intent, retryCount, maxRetries);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("等待工具依赖结果被中断: {}", intent, e);
                break;
            } catch (Exception e) {
                log.error("获取工具依赖结果时发生异常: {}", intent, e);
                retryCount++;
            }
        }
        return dependencyResults;
    }

    private String executeCustomTool(ToolExecutionRequest req) throws Exception {
        log.info("执行自定义工具: {}", req.name());
        Class<?> cls = Class.forName(TOOL_SCAN_NAME + req.name());
        Runnable tool = (Runnable) JsonUtils.toJsonObject(req.arguments().toString(), cls);
        tool.run();
        return (String) cls.getMethod("getResult").invoke(tool);
    }

    private Boolean isOKTask(List<CompletableFuture<String>> result) {
        for (CompletableFuture<String> future : result) {
            if (future == null) {
                return false;
            }
        }
        return true;
    }

    private boolean isTaskInDelayedQueue(String intent) {
        if (intent != null && !StrUtil.isBlank(intent)) {
            return intentDelayedQueueClien.containsTask(intent);
        } else {
            log.error("工具任务intent为null");
            throw new RuntimeException("工具任务id为空");
        }
    }

    @Override
    protected boolean shouldUpdateGraphOnFailure() {
        return true; // 工具任务失败时也要更新图状态
    }
}