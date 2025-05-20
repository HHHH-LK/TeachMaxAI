package com.aiproject.smartcampus.handler.chathandler.handlerImpl;

import com.aiproject.smartcampus.commons.utils.CollectionUtils;
import com.aiproject.smartcampus.commons.utils.PromptUtils;
import com.aiproject.smartcampus.exception.RagExpection;
import com.aiproject.smartcampus.handler.chathandler.ChatHandler;
import com.aiproject.smartcampus.pojo.bo.handlerentity.ChatHandlerResponse;
import com.aiproject.smartcampus.pojo.bo.handlerentity.ChatHandlerquery;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.query.Query;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @program: SmartCampus
 * @description: rag增强检索
 * @author: lk
 * @create: 2025-05-19 17:12
 **/

@Service
@RequiredArgsConstructor
@Slf4j
public class RagHandler extends ChatHandler {

    private final  ContentRetriever contentRetriever;
    private final ChatLanguageModel chatLanguageModel;

    @Override
    public void chatHandle(ChatHandlerquery handlerquery, ChatHandlerResponse handlerResponse) {

        // 1. 确保上一步（Memory）成功
        if (!handlerResponse.getIsSuccess()) {
            log.error("RAG 前步骤失败");
            throw new RagExpection("RAG 前步骤失败");
        }
        log.info("执行rag增强检索中。。。。。");
        String userMessage   = handlerquery.getQueryContent();
        String memoryContent = handlerquery.getMemoryContent();

        // 2. 调用检索器
        List<Content> retrieved = contentRetriever
                .retrieve(Query.from(userMessage))
                .stream().distinct().collect(Collectors.toList());
        String retrievedContent = CollectionUtils.ContentSplicing(retrieved);

        // 3. STEP 1: 判定能否回答
        String canPrompt = PromptUtils.buildRagCanPrompt(retrievedContent, memoryContent, userMessage);
        String canResult = chatLanguageModel.chat(canPrompt).trim();
        handlerResponse.setIsSuccess(true);
        if ("CAN".equalsIgnoreCase(canResult)) {
            // STEP 2: 真正回答
            String answerPrompt = PromptUtils.buildRagAnswerPrompt(retrievedContent, memoryContent, userMessage);
            String finalAnswer  = chatLanguageModel.chat(answerPrompt);
            handlerResponse.setChatAnswer(finalAnswer);
            log.info("rag增强检索结束");

        } else {
            // fallback：交给下一个 handler
            handlerquery.setRagContent(retrievedContent + "\n" + memoryContent);
            if (nextHandler != null) {
                log.info("rag增强检索结束");
                nextHandler.chatHandle(handlerquery, handlerResponse);
            }
        }
    }
}
