package com.aiproject.smartcampus.pojo.entity;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.memory.ChatMemory;
import lombok.Data;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @program: SmartCampus
 * @description: 责任链响应体
 * @author: lk
 * @create: 2025-05-17 16:17
 **/

@Data
public class HandlerResponse {
    private Boolean isSuccess;
    private Map<String, ChatMemory> memoryMap=new ConcurrentHashMap<>();
    //对于id的Memory是否为空
    private List<ChatMessage> result;
    private String errorMsg;
}
