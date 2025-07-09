package com.aiproject.smartcampus.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * SSE推送消息DTO（新增，不影响原有代码）
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ChatMessagePushDto {
    /**
     * 会话ID
     */
    private Long conversationId;

    /**
     * 发送者用户ID
     */
    private Integer senderUserId;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 消息类型
     */
    private String messageType;

    /**
     * 文件URL
     */
    private String fileUrl;

    /**
     * 时间戳
     */
    private Long timestamp;
}