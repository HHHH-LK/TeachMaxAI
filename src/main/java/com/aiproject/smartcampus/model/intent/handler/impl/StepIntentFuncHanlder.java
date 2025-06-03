package com.aiproject.smartcampus.model.intent.handler.impl;

import com.aiproject.smartcampus.model.intent.handler.AutoRegisterHandler;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * @program: SmartCampus
 * @description: 工具处理器
 * @author: lk
 * @create: 2025-05-28 13:38
 **/

@Service
public class StepIntentFuncHanlder extends AutoRegisterHandler {

    private final String functionDescription="工具函数处理器，执行特定的工具调用和功能操作";
    @Override
    public String run(String intent, List<CompletableFuture<String>> result) {


        return "";
    }
}
