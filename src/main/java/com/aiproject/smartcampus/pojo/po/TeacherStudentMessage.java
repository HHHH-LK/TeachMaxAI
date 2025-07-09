package com.aiproject.smartcampus.pojo.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 师生对话消息表实体类
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("teacher_student_messages")
public class TeacherStudentMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 消息ID
     */
    @TableId(value = "message_id", type = IdType.AUTO)
    private Long messageId;

    /**
     * 会话ID
     */
    @TableField("conversation_id")
    private Long conversationId;

    /**
     * 发送者用户ID
     */
    @TableField("sender_user_id")
    private Integer senderUserId;

    /**
     * 消息内容
     */
    @TableField("content")
    private String content;

    /**
     * 消息类型：text-文本, image-图片, file-文件
     */
    @TableField("message_type")
    private MessageType messageType;

    /**
     * 文件路径（当message_type不是text时使用）
     */
    @TableField("file_url")
    private String fileUrl;

    /**
     * 是否已读
     */
    @TableField("is_read")
    private Boolean isRead;

    /**
     * 创建时间
     */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 消息类型枚举
     */
    public enum MessageType {
        /**
         * 文本消息
         */
        text,
        /**
         * 图片消息
         */
        image,
        /**
         * 文件消息
         */
        file
    }
}