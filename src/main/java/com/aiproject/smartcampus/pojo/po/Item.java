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
 * @description: 道具实体类
 * 用于映射数据库表 item，定义游戏中可使用的各种道具
 * @author: lk_hhh
 * @create: 2025-08-08
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("item") // 指定数据库表名
public class Item implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 道具ID，主键，自增
     */
    @TableId(value = "item_id", type = IdType.AUTO)
    private Long itemId;

    /**
     * 道具名称
     */
    @TableField("name")
    private String name;

    /**
     * 道具类型（攻击、防御、治疗、辅助）
     * attack / defense / heal / assist
     */
    @TableField("type")
    private String type;

    /**
     * 道具效果数值（如加血、加攻等）
     */
    @TableField("effect_value")
    private Integer effectValue;

    /**
     * 道具描述
     */
    @TableField("description")
    private String description;

    /**
     * 道具稀有度 (0/1/2)
     * 0 - 普通，1 - 稀有，2 - 史诗
     */
    @TableField("rarity")
    private Integer rarity;
}
