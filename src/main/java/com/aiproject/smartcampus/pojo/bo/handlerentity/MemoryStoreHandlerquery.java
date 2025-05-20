package com.aiproject.smartcampus.pojo.bo.handlerentity;

import dev.langchain4j.data.message.ChatMessage;
import lombok.Data;

import java.util.List;

/**
 * @program: SmartCampus
 * @description: 存储层
 * @author: lk
 * @create: 2025-05-17 16:16
 **/

@Data
public class MemoryStoreHandlerquery extends Handlerquery {

    private String userId;
    private List<ChatMessage> chatMessageList;


}
