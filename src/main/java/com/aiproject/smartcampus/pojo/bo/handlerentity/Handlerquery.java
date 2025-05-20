package com.aiproject.smartcampus.pojo.bo.handlerentity;

import dev.langchain4j.memory.ChatMemory;
import lombok.Data;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @program: SmartCampus
 * @description: 责任链请求体
 * @author: lk
 * @create: 2025-05-19 17:21
 **/

@Data
public class Handlerquery {

    //查询内容
    protected String queryContent;
    private Map<String, ChatMemory> queryMap=new ConcurrentHashMap<>();
}
