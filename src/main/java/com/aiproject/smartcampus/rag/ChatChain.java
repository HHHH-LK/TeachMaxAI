package com.aiproject.smartcampus.rag;

import com.aiproject.smartcampus.handler.chathandler.handlerImpl.ChatBaseHandler;
import com.aiproject.smartcampus.handler.chathandler.handlerImpl.FunctionCallingHandler;
import com.aiproject.smartcampus.handler.chathandler.handlerImpl.RagHandler;
import com.aiproject.smartcampus.pojo.bo.ToolList;
import com.aiproject.smartcampus.pojo.bo.handlerentity.ChatHandlerResponse;
import com.aiproject.smartcampus.pojo.bo.handlerentity.ChatHandlerquery;
import com.aiproject.smartcampus.store.LocalStore;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.web.search.searchapi.SearchApiWebSearchEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @program: SmartCampus
 * @description: rag链
 * @author: lk
 * @create: 2025-05-19 19:08
 **/

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatChain {

    private final ChatLanguageModel chatLanguageModel;
    private final ContentRetriever contentRetriever;
    private final ToolList toolList;
    private final LocalStore store;
    private final SearchApiWebSearchEngine searchEngine;
    private Map<String, ChatMemory> memoryMap = new ConcurrentHashMap<>();
    
    public String chat(String userMessage, String memoryId) {
        //构建调用链
        ChatBaseHandler chatbaseChain = new ChatBaseHandler(chatLanguageModel, store);
        RagHandler ragChain = new RagHandler(contentRetriever, chatLanguageModel);
        FunctionCallingHandler funcChain = new FunctionCallingHandler(toolList, chatLanguageModel, searchEngine);
        //构建请求响应
        ChatHandlerquery chatHandlerquery = new ChatHandlerquery();
        chatHandlerquery.setQueryContent(userMessage);
        chatHandlerquery.setUserId(memoryId);
        chatHandlerquery.setQueryMap(memoryMap);
        ChatHandlerResponse chatHandlerResponse = new ChatHandlerResponse();
        //调用链处理
        chatbaseChain.setNextHandler(ragChain).setNextHandler(funcChain);
        chatbaseChain.chatHandle(chatHandlerquery, chatHandlerResponse);
        //获取响应结果
        return chatHandlerResponse.getChatAnswer();

    }

}
