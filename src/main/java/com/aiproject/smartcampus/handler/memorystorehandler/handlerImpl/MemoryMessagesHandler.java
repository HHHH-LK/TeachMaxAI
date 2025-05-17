package com.aiproject.smartcampus.handler.memorystorehandler.handlerImpl;

import com.aiproject.smartcampus.handler.memorystorehandler.Handler;
import com.aiproject.smartcampus.pojo.entity.HandlerResponse;
import com.aiproject.smartcampus.pojo.entity.Handlerquery;
import com.aiproject.smartcampus.commons.utils.CollectionUtils;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.memory.ChatMemory;
import lombok.extern.slf4j.Slf4j;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @program: lecture-langchain-20250525
 * @description: 数据层处理（对来自本地的数据层进行业务处理）
 * @author: lk
 * @create: 2025-05-11 10:54
 **/

@Slf4j
public class MemoryMessagesHandler extends Handler {
    @Override
    public void getMessagesHandle(Handlerquery handlerquery, HandlerResponse handlerResponse) {

        try{
            if(handlerResponse.getIsSuccess()==false){
                log.error("{}流程前错误",MemoryMessagesHandler.class.getName());
                return;
            }
            Map<String, ChatMemory> memoryMap = handlerResponse.getMemoryMap();
            String userId = handlerquery.getUserId();
            ChatMemory chatMemory = memoryMap.get(userId);
            List<ChatMessage> messages = chatMemory.messages();
            handlerResponse.setResult(CollectionUtils.isEmpty(messages)?new LinkedList<>():messages);
            handlerResponse.setIsSuccess(true);

            if (nextHandler != null) {
                nextHandler.getMessagesHandle(handlerquery,handlerResponse);
            }else{
                super.PhandlerResponse=handlerResponse;
            }

        }catch (RuntimeException e){
            log.error("{}流程错误",MemoryMessagesHandler.class.getName());
            handlerResponse.setIsSuccess(false);
        }


    }

    @Override
    public void updateMessagesHandle(Handlerquery handlerquery,HandlerResponse handlerResponse) {



    }

    @Override
    public void deleteMessagesHandle(Handlerquery handlerquery,HandlerResponse handlerResponse) {

    }
}
