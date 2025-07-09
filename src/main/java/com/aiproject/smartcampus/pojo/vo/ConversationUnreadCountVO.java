package com.aiproject.smartcampus.pojo.vo;

import lombok.Data;

/**
 * 会话未读消息统计DTO
 */
@Data
public class ConversationUnreadCountVO {
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
     * 学生姓名
     */
    private String studentName;
    
    /**
     * 学生学号
     */
    private String studentNumber;
    
    /**
     * 教师姓名
     */
    private String teacherName;
    
    /**
     * 未读消息数量
     */
    private Integer unreadCount;
}