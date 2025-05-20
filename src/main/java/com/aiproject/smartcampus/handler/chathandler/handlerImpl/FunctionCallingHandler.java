package com.aiproject.smartcampus.handler.chathandler.handlerImpl;

import com.aiproject.smartcampus.commons.utils.JsonUtils;
import com.aiproject.smartcampus.commons.utils.PromptUtils;
import com.aiproject.smartcampus.exception.MemoryExpection;
import com.aiproject.smartcampus.exception.RagExpection;
import com.aiproject.smartcampus.handler.chathandler.ChatHandler;
import com.aiproject.smartcampus.pojo.bo.ToolList;
import com.aiproject.smartcampus.pojo.bo.handlerentity.ChatHandlerResponse;
import com.aiproject.smartcampus.pojo.bo.handlerentity.ChatHandlerquery;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.ChatRequestParameters;
import dev.langchain4j.model.chat.response.ChatResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static com.aiproject.smartcampus.contest.CommonContest.TOOL_SCAN_NAME;

/**
 * @program: SmartCampus
 * @description: 工具调用处理
 * @author: lk
 * @create: 2025-05-19 17:12
 **/
@Service
@Slf4j
@RequiredArgsConstructor
public class FunctionCallingHandler extends ChatHandler {

    private final ToolList toolList;
    private final ChatLanguageModel chatLanguageModel;

    @Override
    public void chatHandle(ChatHandlerquery handlerquery, ChatHandlerResponse handlerResponse) {

        if (!handlerResponse.getIsSuccess()) {
            log.error("FunctionCalling前置步骤失败");
            throw new RagExpection("FunctionCalling前置步骤失败");
        }
        log.info("执行工具调用中...");
        String userMessage= handlerquery.getQueryContent();
        String ragContent = handlerquery.getRagContent();
        //调用工具
        List<ToolSpecification> tools = toolList.getTools();
        ChatResponse chatResponse = chatLanguageModel.doChat(ChatRequest.builder()
                //当messages多条时会
                .messages(UserMessage.from(userMessage))
                .parameters(ChatRequestParameters.builder()
                        .toolSpecifications(tools)
                        .build())
                .build()
        );
        // 解析并执行工具请求
        AtomicReference<String> toolResultRef = new AtomicReference<>("");
        List<ToolExecutionRequest> toolRequests = chatResponse.aiMessage().toolExecutionRequests();
        if (toolRequests != null && !toolRequests.isEmpty()) {
            for (ToolExecutionRequest req : toolRequests) {
                try {
                    Class<?> cls = Class.forName(TOOL_SCAN_NAME + req.name());
                    Runnable tool = (Runnable) JsonUtils.toJsonObject(req.arguments().toString(), cls);
                    tool.run();
                    String result = (String) cls.getMethod("getResult").invoke(tool);
                    toolResultRef.set(result);
                } catch (Exception e) {
                    log.error("调用工具 {} 失败", req.name(), e);
                    throw new MemoryExpection(e.getMessage());
                }
            }
        } else {
            log.warn("未触发任何工具调用");
        }
        log.info("工具调用结束");
        String toolResult = toolResultRef.get();
        String memoryContent = handlerquery.getMemoryContent();
        //判定能否用工具结果回答
        String canPrompt = PromptUtils.buildFuncCanPrompt(toolResult, memoryContent,ragContent, userMessage);
        String canResult = chatLanguageModel.chat(canPrompt).trim();

        if ("CAN".equalsIgnoreCase(canResult)) {
            //生成答案
            String answerPrompt = PromptUtils.buildFuncAnswerPrompt(toolResult, memoryContent,ragContent, userMessage);
            String finalAnswer  = chatLanguageModel.chat(answerPrompt);
            handlerResponse.setIsSuccess(true);
            handlerResponse.setChatAnswer(finalAnswer);
        } else {
            //处理最终结果
            handlerResponse.setIsSuccess(true);
            handlerResponse.setChatAnswer("很抱歉，我无法根据现有的信息回答您的问题。您可以尝试重新表述问题或提供更多上下文。");
        }
    }

}
