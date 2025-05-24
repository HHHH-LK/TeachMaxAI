package com.aiproject.smartcampus.handler.chathandler.handlerImpl;

import com.aiproject.smartcampus.commons.utils.ChatMemoryBuilder;
import com.aiproject.smartcampus.commons.utils.CollectionUtils;
import com.aiproject.smartcampus.commons.utils.PromptUtils;
import com.aiproject.smartcampus.exception.MemoryExpection;
import com.aiproject.smartcampus.handler.chathandler.ChatHandler;
import com.aiproject.smartcampus.pojo.bo.handlerentity.ChatHandlerResponse;
import com.aiproject.smartcampus.pojo.bo.handlerentity.ChatHandlerquery;
import com.aiproject.smartcampus.store.LocalStore;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @program: SmartCampus
 * @description: 记忆处理
 * @author: lk
 * @create: 2025-05-19 17:11
 **/

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatBaseHandler extends ChatHandler {

    private  final ChatLanguageModel chatLanguageModel;
    private  final LocalStore store;
    private Map<String, ChatMemory> memoryMap=new ConcurrentHashMap<>();

    //处理记忆是否能够回答用户问题
    @Override
    public void chatHandle(ChatHandlerquery chatHandlerquery, ChatHandlerResponse handlerResponse) {

        try{
            log.info("执行记忆搜索中。。。。。");
            memoryMap=chatHandlerquery.getQueryMap();
            String memoryId=chatHandlerquery.getUserId();
            String userMessage=chatHandlerquery.getQueryContent();
            memoryMap.computeIfAbsent(memoryId, id -> ChatMemoryBuilder.buildChatMemory(id, store));
            ChatMemory chatMemory = memoryMap.get(memoryId);
            chatMemory.add(UserMessage.from(userMessage));
            List<ChatMessage> messages = chatMemory.messages();
            //构建提示词并调用模型回答问题
            String content = CollectionUtils.MessageSplicing(messages);
            String canPrompt = PromptUtils.buildCanPrompt(content, userMessage);
            String canResult = chatLanguageModel.chat(canPrompt).trim();
            handlerResponse.setIsSuccess(true);
            if ("CAN".equalsIgnoreCase(canResult)) {
                String answerPrompt = PromptUtils.buildAnswerPrompt(content, userMessage);
                String finalAnswer = chatLanguageModel.chat(answerPrompt);
                handlerResponse.setChatAnswer(finalAnswer);
                log.info("记忆搜索结束");
            }else {
                chatHandlerquery.setMemoryContent(content);
                if (nextHandler != null) {
                    log.info("记忆化搜索结束");
                    nextHandler.chatHandle(chatHandlerquery, handlerResponse);
                }
            }

        }catch (Exception e){
            handlerResponse.setIsSuccess(false);
            handlerResponse.setErrorMsg(e.getMessage());

            throw new MemoryExpection(e.getMessage());
        }


    }
}
