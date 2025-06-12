package com.aiproject.smartcampus.pojo.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("notification_ack")
public class NotificationAck implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("notification_id")
    private Long notificationId;

    @TableField("receiver_id")
    private Integer receiverId;

    /**
     * student 或 teacher
     * */
    @TableField("role")
    private String role;

    @TableField("read_time")
    private LocalDateTime readTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;


}
