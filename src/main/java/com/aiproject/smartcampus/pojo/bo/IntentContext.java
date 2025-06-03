package com.aiproject.smartcampus.pojo.bo;

import dev.langchain4j.memory.ChatMemory;
import lombok.Data;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @program: SmartCampus
 * @description: 意图识别实体
 * @author: lk
 * @create: 2025-05-28 19:59
 **/

@Data
public class IntentContext {
    private String userId;
    private String queryContent;
    private String memoryContent;
    private String ragContent;
    private String toolResult;
    private boolean isSuccess = true;
    private String errorMsg;
    private String finalAnswer;
    private Map<String, ChatMemory> queryMap = new ConcurrentHashMap<>();
}
