package com.aiproject.smartcampus.model;

import com.aiproject.smartcampus.model.intent.Intent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * @program: SmartCampus
 * @description: 智能体实现
 * @author: lk
 * @create: 2025-05-27 23:05
 **/

@Component
@RequiredArgsConstructor
public class Agent {

    private final Intent intentHandler;
    //注入意图处理器

    public void start(String userMessage){

        String answer = intentHandler.handlerIntent(userMessage);

    }


}
