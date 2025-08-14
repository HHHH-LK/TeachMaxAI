package com.aiproject.smartcampus.pojo.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 游戏用户表，存储玩家基本信息
 */
@Data
@TableName("game_user")
public class GameUser implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 玩家ID，主键
     */
    @TableId(value = "user_id", type = IdType.AUTO)
    private Long userId;

    /**
     * 游戏昵称
     */
    @TableField("game_name")
    private String gameName;

    /**
     * 玩家等级
     */
    private Integer level;

    /**
     * 当前经验值
     */
    private Integer exp;

    /**
     * 注册时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 学生Id
     */
    @TableField("studentId") // 如果数据库字段是 studentId 驼峰就不用加
    private Integer studentId;
}
