package com.aiproject.smartcampus.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * 师生聊天消息推送DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessagePushDto {
    
    /**
     * 消息ID
     */
    private Long messageId;
    
    /**
     * 会话ID
     */
    private Long conversationId;
    
    /**
     * 发送者ID
     */
    private String senderId;
    
    /**
     * 发送者用户ID (兼容原有字段)
     */
    private Integer senderUserId;
    
    /**
     * 发送者姓名
     */
    private String senderName;
    
    /**
     * 发送者头像
     */
    private String senderAvatar;
    
    /**
     * 接收者ID
     */
    private String receiverId;
    
    /**
     * 接收者用户ID
     */
    private Integer receiverUserId;
    
    /**
     * 消息内容
     */
    private String content;
    
    /**
     * 消息类型 (text/image/file等)
     */
    private String messageType;
    
    /**
     * 文件URL
     */
    private String fileUrl;
    
    /**
     * 发送时间
     */
    private LocalDateTime sendTime;
    
    /**
     * 是否已读
     */
    private Boolean isRead;
    
    /**
     * 时间戳
     */
    private Long timestamp;
}