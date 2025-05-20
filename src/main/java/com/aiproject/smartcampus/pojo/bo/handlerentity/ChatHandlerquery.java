package com.aiproject.smartcampus.pojo.bo.handlerentity;

import lombok.Data;

/**
 * @program: SmartCampus
 * @description: model chat层
 * @author: lk
 * @create: 2025-05-19 17:25
 **/

@Data
public class ChatHandlerquery extends Handlerquery{

    private String userId;
    private String memoryContent;
    private String RagContent;


}
