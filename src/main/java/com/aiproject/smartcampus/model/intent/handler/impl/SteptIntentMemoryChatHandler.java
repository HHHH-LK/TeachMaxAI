package com.aiproject.smartcampus.model.intent.handler.impl;

import com.aiproject.smartcampus.model.intent.handler.AutoRegisterHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * @program: SmartCampus
 * @description: 记忆处理处理器
 * @author: lk
 * @create: 2025-05-28 13:33
 **/

@Service
@Slf4j
public class SteptIntentMemoryChatHandler extends AutoRegisterHandler {

    private final String functionDescription="基于对话记忆回答用户问题，处理上下文相关的查询";

    @Override
    public String run(String intent, List<CompletableFuture<String>> result) {

        log.info("基于记忆的回答" + intent);

        return "基于记忆的回答：" + intent;

    }


}
