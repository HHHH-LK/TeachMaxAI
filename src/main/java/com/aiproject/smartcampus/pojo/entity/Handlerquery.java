package com.aiproject.smartcampus.pojo.entity;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.memory.ChatMemory;
import lombok.Data;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @program: SmartCampus
 * @description: 责任链请求体
 * @author: lk
 * @create: 2025-05-17 16:16
 **/

@Data
public class Handlerquery {

    private Map<String, ChatMemory> queryMap=new ConcurrentHashMap<>();
    private String userId;
    private List<ChatMessage> chatMessageList;


}
