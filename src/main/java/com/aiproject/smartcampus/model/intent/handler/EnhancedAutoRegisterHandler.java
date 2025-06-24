package com.aiproject.smartcampus.model.intent.handler;


import com.aiproject.smartcampus.commons.client.EnhancedHandlerRegisterClient;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Order(5)
@Slf4j
@Component
public abstract class EnhancedAutoRegisterHandler implements Handler {

    @Autowired
    private EnhancedHandlerRegisterClient handlerRegisterClient;

    @PostConstruct
    public void setHandlerRegisterClient(){
        //自动注册逻辑
        String handlerName = this.getClass().getSimpleName();
        handlerRegisterClient.addHandler(handlerName, this);


    }






}