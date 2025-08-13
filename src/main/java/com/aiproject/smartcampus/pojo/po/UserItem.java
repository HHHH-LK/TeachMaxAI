package com.aiproject.smartcampus.pojo.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;

/**
 * 用户道具表，记录玩家持有及使用的道具情况
 */
@Data
@TableName("user_item")
public class UserItem implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 记录ID，主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 道具ID
     */
    @TableField("item_id")
    private Long itemId;

    /**
     * 玩家持有的道具数量
     */
    private Integer quantity;
}
