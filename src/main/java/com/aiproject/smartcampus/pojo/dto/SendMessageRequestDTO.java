package com.aiproject.smartcampus.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 发送消息请求DTO
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SendMessageRequestDTO {
    /**
     * 会话ID
     */
    private Long conversationId;
    
    /**
     * 消息内容
     */
    private String content;
    
    /**
     * 消息类型（text/image/file）
     */
    private String messageType = "text";
    
    /**
     * 文件URL（图片或文件消息时使用）
     */
    private String fileUrl;
    
    /**
     * 接收用户Id（用于验证会话）
     */
    private Integer useId;
}
