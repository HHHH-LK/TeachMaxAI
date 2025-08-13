package com.aiproject.smartcampus.pojo.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;

/**
 * 战斗记录表，记录每次战斗的详细信息
 */
@Data
@TableName("battle_log")
public class BattleLog implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 战斗记录ID，主键
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
     * 怪物ID
     */
    @TableField("monster_id")
    private Long monsterId;

    /**
     * 战斗结果（胜利/失败）
     */
    @TableField("result")
    private String result;

    /**
     * 当前战斗回合数
     */
    @TableField("total_turns")
    private Integer totalTurns;

    /**
     * 关联的题目ID（与答题系统关联）
     */
    @TableField("question_id")
    private Long questionId;

    /**
     * 所选技能（普通攻击，技能，必杀技）
     */
    @TableField("detail")
    private String detail;

    /**
     * 开始闯关id
     */
    @TableField("tower_challenge_log_id")
    private Integer towerChallengeLogId;
}
