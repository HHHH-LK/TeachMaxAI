package com.aiproject.smartcampus.pojo.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @description: 试卷-学科关联表
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("exam_discipline")
public class ExamDiscipline {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id; // 主键ID，自增

    @TableField("exam_id")
    private Long examId; // 考试ID

    @TableField("discipline_id")
    private Long disciplineId; // 学科ID
}