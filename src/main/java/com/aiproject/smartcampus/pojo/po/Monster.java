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
 * @program: SmartCampus
 * @description: 怪物实体类
 * @author: lk_hhh
 * @create: 2025-08-08
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("monster")
public class Monster implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 怪物ID，主键，自增
     */
    @TableId(value = "monster_id", type = IdType.AUTO)
    private Long monsterId;

    /**
     * 怪物名称
     */
    @TableField("name")
    private String name;

    /**
     * 怪物血量
     */
    @TableField("hp")
    private Integer hp;

    /**
     * 所属塔层ID
     */
    @TableField("floor_id")
    private Long floorId;
}
