package com.aiproject.smartcampus.model.handler;


import com.aiproject.smartcampus.commons.client.EnhancedHandlerRegisterClient;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

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
        log.info("处理器已注册" + handlerName);
        handlerRegisterClient.addHandler(handlerName, this);

    }






}