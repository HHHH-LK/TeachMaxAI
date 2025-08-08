package com.aiproject.smartcampus.pojo.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * 游戏用户表，存储玩家基本信息
 */
@Data
@TableName("game_user")       // MyBatis-Plus 表名
public class GameUser {

    /**
     * 玩家ID，主键，自增
     */
    @TableId(value = "user_id", type = IdType.AUTO)  // MP 主键及自增策略
    private Long userId;

    /**
     * 游戏昵称，不能为空
     */
    @TableField("game_name")
    private String gameName;

    /**
     * 玩家等级，默认1
     */
    @TableField("level")
    private Integer level;

    /**
     * 当前经验值，默认0
     */
    @TableField("exp")
    private Integer exp;

    /**
     * 注册时间，默认当前时间
     */
    @TableField("create_time")
    private LocalDateTime createTime;

    /**
     * 学生Id，不能为空
     */
    @TableField("studentId")
    private Integer studentId;
}
