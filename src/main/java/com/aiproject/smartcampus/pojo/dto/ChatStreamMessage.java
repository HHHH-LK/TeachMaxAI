package com.aiproject.smartcampus.pojo.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatStreamMessage {
    
    /**
     * 消息类型：thinking, chunk, completed, error
     */
    private String messageType;
    
    /**
     * 消息内容
     */
    private String content;
    
    /**
     * 用户ID
     */
    private String userId;
    
    /**
     * 时间戳
     */
    private Long timestamp;
    
    /**
     * 消息ID（可选）
     */
    private String messageId;
    
    /**
     * 是否为完整消息（用于区分chunk）
     */
    private Boolean isComplete;
    
    /**
     * 额外的元数据（可选）
     */
    private Object metadata;


}