package com.aiproject.smartcampus.pojo.po;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 塔层挑战记录表，记录玩家在某一塔层的挑战次数及状态
 */
@Data
@TableName("tower_challenge_log")
public class TowerChallengeLog implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 挑战记录ID，主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 玩家ID
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 塔层ID
     */
    @TableField("floor_id")
    private Long floorId;

    /**
     * 当前塔层的挑战次数（第几次闯关）
     */
    @TableField("challenge_count")
    private Integer challengeCount;

    /**
     * 最近一次挑战时间
     */
    @TableField("last_challenge_time")
    private LocalDateTime lastChallengeTime;

    /**
     * 挑战状态（进行中、成功、失败）
     */
    @TableField("status")
    private String status;

    /**
     * 记录创建时间
     */
    @TableField("create_time")
    private LocalDateTime createTime;

    /**
     * 记录更新时间
     */
    @TableField("update_time")
    private LocalDateTime updateTime;


}
