package com.aiproject.smartcampus.handler.memorystorehandler;

import com.aiproject.smartcampus.pojo.entity.HandlerResponse;
import com.aiproject.smartcampus.pojo.entity.Handlerquery;

/**
 * @program: SmartCampus
 * @description: 责任链规范化
 * @author: lk
 * @create: 2025-05-17 16:14
 **/

public abstract class Handler {

    protected Handler nextHandler;
    protected HandlerResponse PhandlerResponse=new HandlerResponse();

    public Handler setNextHandler(Handler next) {
        this.nextHandler = next;
        return next;
    }

    public HandlerResponse getPHandlerResponse(){
        return PhandlerResponse;
    }
   public abstract void getMessagesHandle(Handlerquery handlerquery, HandlerResponse handlerResponse);
   public abstract void updateMessagesHandle(Handlerquery handlerquery,HandlerResponse handlerResponse);
   public abstract void deleteMessagesHandle(Handlerquery handlerquery,HandlerResponse handlerResponse);

}
