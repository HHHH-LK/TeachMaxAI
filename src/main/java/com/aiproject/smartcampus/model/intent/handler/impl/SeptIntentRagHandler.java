package com.aiproject.smartcampus.model.intent.handler.impl;

import com.aiproject.smartcampus.model.intent.handler.AutoRegisterHandler;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * @program: SmartCampus
 * @description: 增强检索处理器
 * @author: lk
 * @create: 2025-05-28 13:37
 **/

@Service
public class SeptIntentRagHandler extends AutoRegisterHandler {

    private final String functionDescription="增强检索生成处理器，基于知识库检索相关信息并生成回答";

    @Override
    public String run(String intent, List<CompletableFuture<String>> result) {





        return "";
    }
}
