package com.aiproject.smartcampus.pojo.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @program: lecture-langchain-20250525
 * @description: 本地消息存储 (AI记忆表)
 * @author: lk
 * @create: 2025-05-11 10:21
 **/

@Data
@TableName("memory_store")
public class LocalMessage {

    @TableId(value = "id", type = IdType.INPUT)  // 改为INPUT，手动设置用户ID
    private Integer userId;  // 改为Integer类型匹配数据库int类型

    @TableField(value = "content")
    private String content;

    @TableField(value = "create_time")
    private LocalDateTime createTime;  // 改为String类型匹配数据库varchar(32)类型
}