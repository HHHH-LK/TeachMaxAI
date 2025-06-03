package com.aiproject.smartcampus.model.intent.handler.impl;

import com.aiproject.smartcampus.commons.ResultCilent;
import com.aiproject.smartcampus.commons.StatusCilent;
import com.aiproject.smartcampus.commons.delayedtask.IntentBatchTask;
import com.aiproject.smartcampus.commons.delayedtask.IntentDelayedQueueClien;
import com.aiproject.smartcampus.commons.utils.CreateDiagram;
import com.aiproject.smartcampus.commons.utils.TaskStatusChange;
import com.aiproject.smartcampus.model.intent.handler.AutoRegisterHandler;
import com.aiproject.smartcampus.pojo.bo.TaskAction;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static com.aiproject.smartcampus.pojo.bo.TaskAction.ActionType.INDEGREE_DECREASE;

/**
 * @program: SmartCampus
 * @description: LLM基础意图处理器
 * @author: lk
 * @create: 2025-05-29 20:05
 **/

@Service
@Slf4j
@RequiredArgsConstructor
public class StepIntentLLMChatResponseHandler extends AutoRegisterHandler {

    private final CreateDiagram createDiagram;
    private final ChatLanguageModel chatLanguageModel;
    private final IntentDelayedQueueClien intentDelayedQueueClien;
    private final ResultCilent resultCilent;
    private final StatusCilent statusCilent;

    private final String functionDescription = "基础大语言模型对话处理器，处理一般性对话和问答";

    @Override
    public String run(String intent, List<CompletableFuture<String>> result) {

        log.info("基础LLM执行[{}]中",intent);
        //定义结果
        String finalResult = null;
        //首先获取入度看是否需要前置条件
        int inDegree = createDiagram.getInDegree(intent);
        if (inDegree == 0) {
            //执行业务
            ChatResponse chatResponse = chatLanguageModel.chat(UserMessage.from(intent));
            finalResult = chatResponse.aiMessage().text();
            log.info("执行[{}]成功,结果为[{}]", intent, finalResult);
            //更新状态
            TaskAction action2 = TaskAction.statusUpdate(intent, "SUCCESS");
            statusCilent.push(action2);
            //减少入度
            TaskAction action1 =TaskAction.indegreeDecreaseWithDeps(intent, result);
            statusCilent.push(action1);

        } else {
            log.info("执行[{}]失败,前置条件未满足,将推迟处理", intent);
            List<CompletableFuture<String>> result1 = null;
            int retryCount = 0;
            //todo 后续将改进成异步回调
            while (result1 == null && retryCount < 10) {
                try {
                    //尝试获取
                    result1 = resultCilent.getResult(intent);
                    if (result1 == null) {
                        Thread.sleep(200);
                    }
                    retryCount++;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.error("等待依赖结果被中断: {}", intent, e);
                    break;
                }
            }
            if (result1 != null&&!result1.isEmpty()) {
                intentDelayedQueueClien.addTask(new IntentBatchTask(intent, result1, 200));
                log.info("[{}]任务已加入到延时对列中，即将重新执行，请稍作等待");
            } else {
                log.warn("获取任务[{}]结果失败，重试次数达到上限，前置任务可能丢失或异常", intent);
                throw new RuntimeException("任务缺失");
            }
        }

        log.info("执行结果为{}",finalResult);
        return finalResult;
    }

}
