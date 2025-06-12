package com.aiproject.smartcampus.pojo.po;

import com.aiproject.smartcampus.commons.utils.NotificationTypeHandler;
import com.aiproject.smartcampus.commons.utils.SenderTypeHandler;
import com.aiproject.smartcampus.pojo.enums.NotificationType;
import com.aiproject.smartcampus.pojo.enums.SenderType;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.io.Serializable;
import java.time.LocalDateTime;

import static org.bouncycastle.asn1.x500.style.RFC4519Style.description;

/**
 * @program: SmartCampus
 * @description: 通知实体类
 * @author: lk_hhh
 * @create: 2025-06-11 10:44
 **/
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("notification")
public class Notificate implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 通知ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 通知标题
     */
    @TableField("title")
    private String title;

    /**
     * 通知内容
     */
    @TableField("content")
    private String content;

    /**
     * 发送者ID
     */
    @TableField("sender_id")
    private Integer senderId;


    /**
     * 接收者ID（学生ID，NULL表示全体）
     */
    @TableField("receiver_id")
    private Integer receiverId;

    /**
     * 是否已读（0未读，1已读）
     */
    @TableField("is_read")
    private Boolean isRead;

    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /**
     * 阅读时间
     */
    @TableField("read_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime readTime;

    /**
     * 发送者类型（管理员/教师）
     */
    @TableField(value = "sender_type", typeHandler = SenderTypeHandler.class)
    private SenderType senderType;

    /**
     * 通知类型
     */
    @TableField(value = "type", typeHandler = NotificationTypeHandler.class)
    private NotificationType type;


}