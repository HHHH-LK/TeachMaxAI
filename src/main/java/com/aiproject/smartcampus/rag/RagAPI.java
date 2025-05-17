package com.aiproject.smartcampus.rag;

import com.aiproject.smartcampus.commons.utils.ChatMemoryBuilder;
import com.aiproject.smartcampus.commons.utils.JsonUtils;
import com.aiproject.smartcampus.commons.utils.PromptUtils;
import com.aiproject.smartcampus.pojo.entity.ToolList;
import com.aiproject.smartcampus.pojo.entity.User;
import com.aiproject.smartcampus.store.LocalStore;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.ChatRequestParameters;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.query.Query;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static com.aiproject.smartcampus.contest.CommonContest.TOOL_SCAN_NAME;

/**
 * @program: SmartCampus
 * @description: ragAPI服务
 * @author: lk
 * @create: 2025-05-17 17:57
 **/

@Slf4j
@Service
@RequiredArgsConstructor
public class RagAPI {

        private final ChatLanguageModel chatLanguageModel;
        private final ContentRetriever contentRetriever;
        private final ToolList toolList;
        private final LocalStore store;
        private final Map<String, ChatMemory> memoryMap = new ConcurrentHashMap<>();

    public String doEasyRag(String memoryId, String userMessage) {
        // 实现隔离
        memoryMap.computeIfAbsent(memoryId, id -> ChatMemoryBuilder.buildChatMemory(id, store));
        ChatMemory chatMemory = memoryMap.get(memoryId);
        chatMemory.add(UserMessage.from(userMessage));
        List<ChatMessage> messages = chatMemory.messages();
        List<Content> contentList = contentRetriever.retrieve(Query.from(userMessage));
        // 构建提示词
        StringBuilder stringBuilder = new StringBuilder(messages.toString()).append("\n");
        for (Content content : contentList) {
            stringBuilder.append(content.textSegment().text()).append("\n");
        }
        String content = stringBuilder.toString();
        //对messgae进行总结
        String summaryPrompt = PromptUtils.buildSummaryPrompt(messages);
        String answer = chatLanguageModel.chat(summaryPrompt);
        // 添加工具
        List<ToolSpecification> tools = toolList.getTools();
        ChatResponse chatResponse = chatLanguageModel.doChat(ChatRequest.builder()
                //当messages多条时会
                .messages(UserMessage.from(answer))
                .parameters(ChatRequestParameters.builder()
                        .toolSpecifications(tools)
                        .build())
                .build()
        );
        // 获取工具执行请求
        AtomicReference<String> result = new AtomicReference<>();
        List<ToolExecutionRequest> toolRequests = chatResponse.aiMessage().toolExecutionRequests();
        // 遍历工具执行请求并处理
        if (toolRequests != null && !toolRequests.isEmpty()) {
            toolRequests.forEach(toolExecutionRequest -> {
                try {
                    Class<?> aClass = Class.forName(TOOL_SCAN_NAME + toolExecutionRequest.name());
                    Runnable calculatorRunnable = (Runnable) JsonUtils.toJsonObject(toolExecutionRequest.arguments().toString(), aClass);
                    calculatorRunnable.run();
                    Method getResultMethod = aClass.getMethod("getResult");
                    String resultValue = (String) getResultMethod.invoke(calculatorRunnable);
                    result.set(resultValue);
                } catch (Exception e) {
                    log.error("调用工具:{}失败", toolExecutionRequest.name(), e);
                }
            });
        } else {
            log.warn("未触发任何工具调用");
        }
        String prompt = PromptUtils.buildAnswerPrompt(result.get(), content, userMessage);
        return chatLanguageModel.chat(SystemMessage.systemMessage(prompt), UserMessage.from(userMessage)).aiMessage().text();
    }

    }

