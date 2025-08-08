package com.aiproject.smartcampus.pojo.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * @program: TeacherMaxAI
 * @description: 任务实体
 * @author: lk_hhh
 * @create: 2025-08-08
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("task")
public class Task implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 任务ID，主键，自增
     */
    @TableId(value = "task_id", type = IdType.AUTO)
    private Long taskId;

    /**
     * 所属塔层ID
     */
    @TableField("floor_id")
    private Long floorId;

    /**
     * 关联知识点ID（可与题库关联）
     */
    @TableField("point_ids")
    private String pointIds;

    /**
     * 完成任务获得的经验
     */
    @TableField("reward_exp")
    private Integer rewardExp;

    /**
     * 完成任务奖励的道具的稀有度
     */
    @TableField("reward_item_rarity")
    private Integer rewardItemRarity;

    /**
     * 奖励道具数量
     */
    @TableField("reward_item_qty")
    private Integer rewardItemQty;
}
