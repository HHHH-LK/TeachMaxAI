package com.aiproject.smartcampus.pojo.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author lk_hhh
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("discipline")
public class Discipline {
    @TableId(value = "discipline_id")
    private Long disciplineId; // 学科ID，自增主键

    @TableField("discipline_name")
    private String disciplineName; //学科名称
}
