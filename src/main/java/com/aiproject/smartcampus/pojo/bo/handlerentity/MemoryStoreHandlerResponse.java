package com.aiproject.smartcampus.pojo.bo.handlerentity;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.memory.ChatMemory;
import lombok.Data;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @program: SmartCampus
 * @description: 存储层
 * @author: lk
 * @create: 2025-05-17 16:17
 **/

@Data
public class MemoryStoreHandlerResponse extends HandlerResponse {

    private Map<String, ChatMemory> memoryMap=new ConcurrentHashMap<>();

    private List<ChatMessage> result;


}
