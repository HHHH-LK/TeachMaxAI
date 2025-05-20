package com.aiproject.smartcampus.handler.chathandler;

import com.aiproject.smartcampus.pojo.bo.handlerentity.ChatHandlerResponse;
import com.aiproject.smartcampus.pojo.bo.handlerentity.ChatHandlerquery;
import com.aiproject.smartcampus.pojo.bo.handlerentity.MemoryStoreHandlerResponse;

/**
 * @program: SmartCampus
 * @description: 责任链规范化
 * @author: lk
 * @create: 2025-05-17 16:14
 **/

public abstract class ChatHandler {

    protected ChatHandler nextHandler;
    protected MemoryStoreHandlerResponse PhandlerResponse=new MemoryStoreHandlerResponse();

    public ChatHandler setNextHandler(ChatHandler next) {
        this.nextHandler = next;
        return next;
    }

    public abstract void chatHandle(ChatHandlerquery handlerquery, ChatHandlerResponse handlerResponse);

}
