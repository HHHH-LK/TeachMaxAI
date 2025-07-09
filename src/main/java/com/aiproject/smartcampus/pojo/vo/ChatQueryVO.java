package com.aiproject.smartcampus.pojo.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ChatQueryVO {
    
    /**
     * 会话ID
     */
    private Long conversationId;

    /**
     * 学生ID
     */
    private Integer studentId;

    /**
     * 教师ID
     */
    private Integer teacherId;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}