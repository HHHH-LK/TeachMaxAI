package com.aiproject.smartcampus.handler.memorystorehandler;

import com.aiproject.smartcampus.pojo.bo.handlerentity.MemoryStoreHandlerResponse;
import com.aiproject.smartcampus.pojo.bo.handlerentity.MemoryStoreHandlerquery;

/**
 * @program: SmartCampus
 * @description: 责任链规范化
 * @author: lk
 * @create: 2025-05-17 16:14
 **/

public abstract class MemoryStoreHandler {

    protected MemoryStoreHandler nextHandler;
    protected MemoryStoreHandlerResponse PhandlerResponse=new MemoryStoreHandlerResponse();

    public MemoryStoreHandler setNextHandler(MemoryStoreHandler next) {
        this.nextHandler = next;
        return next;
    }

    public MemoryStoreHandlerResponse getPHandlerResponse(){
        return PhandlerResponse;
    }
   public abstract void getMessagesHandle(MemoryStoreHandlerquery handlerquery, MemoryStoreHandlerResponse handlerResponse);
   public abstract void updateMessagesHandle(MemoryStoreHandlerquery handlerquery, MemoryStoreHandlerResponse handlerResponse);
   public abstract void deleteMessagesHandle(MemoryStoreHandlerquery handlerquery, MemoryStoreHandlerResponse handlerResponse);

}
