package com.aiproject.smartcampus.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @program: lecture-langchain-20250525
 * @description: 本地消息存储
 * @author: lk
 * @create: 2025-05-11 10:21
 **/

@Data
@TableName("memorystore")
public class LocalMessage {

    @TableId(value = "id", type = IdType.AUTO)
    private String userId;
    @TableField(value = "content")
    private String content;
    @TableField(value = "create_time")
    private LocalDateTime createTime;

}
