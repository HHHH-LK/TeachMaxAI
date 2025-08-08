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
 * @description: 塔层实体
 * @author: lk_hhh
 * @create: 2025-08-08 13:45
 **/

@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("tower_floor")
public class TowerFloor implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 塔层ID，主键，自增
     */
    @TableId(value = "floor_id", type = IdType.AUTO)
    private Long floorId;

    /**
     * 所属塔ID
     */
    @TableField(value = "tower_id")
    private Long towerId;

    /**
     * 第几层（楼层编号）
     */
    @TableField(value = "floor_no")
    private Integer floorNo;

    /**
     * 是否解锁该层
     */
    @TableField(value = "unlocked")
    private Boolean unlocked;

    /**
     * 关卡介绍（剧情说明）
     */
    @TableField(value = "description")
    private String description;

    /**
     * 是否通过该层
     */
    @TableField(value = "is_pass")
    private Integer isPass;

}
