package com.aiproject.smartcampus.model.intent.impl;

import com.aiproject.smartcampus.model.handler.Handler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

/**
 * @program: ss
 * @description: 智能生成题目处理器
 * @author: lk_hhh
 * @create: 2025-07-05 10:03
 **/

@Service
@RequiredArgsConstructor
@Slf4j
public class TeacherIntentHandler implements Handler {
    @Override
    public String run(String intent, List<CompletableFuture<String>> result) throws ExecutionException, InterruptedException, TimeoutException {
        return "";
    }
}