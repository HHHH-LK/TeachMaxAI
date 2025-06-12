package com.aiproject.smartcampus.model.intent.handler.impl;

import com.aiproject.smartcampus.commons.client.ResultCilent;
import com.aiproject.smartcampus.commons.client.StatusCilent;
import com.aiproject.smartcampus.commons.delayedtask.IntentBatchTask;
import com.aiproject.smartcampus.commons.delayedtask.IntentDelayedQueueClien;
import com.aiproject.smartcampus.commons.utils.CollectionUtils;
import com.aiproject.smartcampus.commons.utils.CreateDiagram;
import com.aiproject.smartcampus.model.intent.handler.AutoRegisterHandler;
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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

//todo 待修改（还存在些逻辑问题）

/**
 * @program: SmartCampus
 * @description: 增强检索处理器
 * @author: lk
 * @create: 2025-05-28 13:37
 **/

@Service
@Slf4j
@RequiredArgsConstructor
public class SeptIntentRagHandler extends AutoRegisterHandler {

    private final CreateDiagram createDiagram;
    private final ChatLanguageModel chatLanguageModel;
    private final IntentDelayedQueueClien intentDelayedQueueClien;
    private final ResultCilent resultCilent;
    private final StatusCilent statusCilent;
    private final ContentRetriever contentRetriever;

    private final String functionDescription = "增强检索生成处理器，基于知识库检索相关信息并生成回答";

    @Override
    public String run(String intent, List<CompletableFuture<String>> result) {
        log.info("RAG处理器开始执行");
        try {
            //更新任务状态
            TaskAction action = TaskAction.statusUpdate(intent, "RUNNING");
            statusCilent.push(action);
            String results = null;
            //判断是否需要前置条件(基于入度进行判断)
            int inDegree = createDiagram.getInDegree(intent);
            if (inDegree == 0) {
                //任务无需前置要求
                results = executeDirectTask(intent);
            } else {
                //任务需要前置要求
                results = executeWithDependencies(intent, result);
            }
            log.info("执行结果为{}", results);
            return results!=null?results:null;
        } catch (Exception e) {
            log.error("任务[{}]执行失败", intent);
            //处理状态更新
            TaskAction action = TaskAction.statusUpdate(intent, "FAILED");
            statusCilent.push(action);
            throw new RuntimeException("任务执行失败: " + intent, e);
        }
    }

    //没有前置依赖需求
    private String executeDirectTask(String intent) {
        try {
            List<Content> retrieve = contentRetriever.retrieve(Query.from(intent));
            //构建结果
            String result = CollectionUtils.ContentSplicing(retrieve);
            log.info("执行[{}]成功,结果为[{}]", intent, result);
            //更新状态
            TaskAction action = TaskAction.statusUpdate(intent, "SUCCESS");
            statusCilent.push(action);
            // 减少相关任务的入度
            TaskAction decreaseAction = TaskAction.indegreeDecrease(intent);
            statusCilent.push(decreaseAction);
            return result;
        } catch (Exception e) {
            log.error("执行[{}]失败", intent);
            TaskAction action = TaskAction.statusUpdate(intent, "FAILED");
            statusCilent.push(action);
            throw new RuntimeException("任务执行失败: " + intent, e);
        }

    }

    //需要前置需求
    private String executeWithDependencies(String intent, List<CompletableFuture<String>> result) {
        //首先判断前置任务是否执行完毕了
        int size = createDiagram.getParetents(intent).size();
        if (result != null && result.size() == size && isOKTask(result)) {
            //前置任务执行完毕
            return executeWithDependenciesOK(intent, result);
        } else {
            //前置任务未执行完毕(延迟进行)
             executeWithDependenciesFaild(intent, result);
             return null;
        }
    }

    private String executeWithDependenciesOK(String intent, List<CompletableFuture<String>> result) {
        try {
            //获取依赖结果
            List<String> dependencyResults = new ArrayList<>();
            // 获取所有前置任务结果
            for (int i = 0; i < result.size(); i++) {
                try {
                    String results = result.get(i).get(5, TimeUnit.SECONDS);
                    dependencyResults.add(results);
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
            String results = resultBuilder.toString();
            //关联前置结果与当前需求
            ChatResponse chatResponse = chatLanguageModel.chat(UserMessage.from(UserPrompts.chainUserPrompts(intent, results)));
            String query = chatResponse.aiMessage().text();
            log.info("关联依赖结果成功，将执行[{}]", query);
            //进行查询rag
            List<Content> retrieve = contentRetriever.retrieve(Query.from(query));
            //构建结果
            String finallyresult = CollectionUtils.ContentSplicing(retrieve);
            log.info("执行[{}]成功,结果为[{}]", query, finallyresult);
            //更新状态
            TaskAction action = TaskAction.statusUpdate(intent, "SUCCESS");
            statusCilent.push(action);
            // 减少相关任务的入度
            TaskAction decreaseAction = TaskAction.indegreeDecrease(intent);
            statusCilent.push(decreaseAction);
            return finallyresult;

        } catch (Exception e) {

            log.error("任务[{}]执行失败", intent);
            TaskAction action = TaskAction.statusUpdate(intent, "FAILED");
            statusCilent.push(action);
            throw new RuntimeException("任务执行失败: " + intent, e);
        }

    }

    private void executeWithDependenciesFaild(String intent, List<CompletableFuture<String>> result) {
            //缺失前置依赖
            log.info("任务[{}]缺失前置依赖，需要延迟执行", intent);
            TaskAction action = TaskAction.statusUpdate(intent, "WAITING_DEPENDENCY");
            statusCilent.push(action);
            //等待依赖执行
            List<CompletableFuture<String>> completableFutures = waitDependenciesOk(intent, result.size());
            if (completableFutures != null && completableFutures.size() == result.size() && isOKTask(completableFutures)) {
                //加入延时队列中
                if (!intentDelayedQueueClien.containsTask(intent)) {
                    log.info("任务[{}]的依赖结果已准备完成，加入延时队列", intent);
                    intentDelayedQueueClien.addTask(new IntentBatchTask(intent, completableFutures, 500));
                } else {
                    log.info("任务[{}]已存在延时队列中", intent);
                }
            }else{
                log.info("任务[{}]的依赖结果未准备完成，等待下次执行", intent);
                TaskAction failedAction = TaskAction.statusUpdate(intent, "DEPENDENCIES_FAILED");
                statusCilent.push(failedAction);
                throw new RuntimeException("任务依赖缺失: " + intent);
            }

    }

    private List<CompletableFuture<String>> waitDependenciesOk(String intent, int size) {
        //定义参数限定避免无线执行
        int PRE_TIMES = 0;
        int MAX_IMIT_SIZE = 10;
        int SLEEP_TIME = 200;
        List<CompletableFuture<String>> result = new ArrayList<>();
        while (PRE_TIMES < MAX_IMIT_SIZE) {
            try {
                //获取依赖结果
                List<String> paretents = createDiagram.getParetents(intent);
                paretents.stream().map(parent -> resultCilent.getResult(intent)).forEach(result::addAll);
                //判断依赖是否执行完
                if (result != null && result.size() == size && isOKTask(result)) {
                    log.info("任务[{}]的依赖结果已准备完成", intent);
                    break;
                }
                Thread.sleep(SLEEP_TIME);
                PRE_TIMES++;
                // 每2次重试记录一次日志
                if (PRE_TIMES % 2 == 0) {
                    log.info("任务[{}]等待依赖结果中，重试次数: {}/{}", intent, PRE_TIMES, MAX_IMIT_SIZE);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("等待依赖结果被中断: {}", intent, e);
                break;
            } catch (Exception e) {
                log.error("获取依赖结果时发生异常: {}", intent, e);
                PRE_TIMES++;
            }
        }
        return result;
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
}
