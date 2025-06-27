package com.aiproject.smartcampus.pojo.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @description: 试卷-题目关联表
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("exam_questions")
public class ExamQuestion {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id; // 主键ID，自增

    @TableField("examId")
    private Long examId; // 试卷ID

    @TableField("questionId")
    private Long questionId; // 题目ID

    @TableField("sortOrder")
    private Integer sortOrder; // 题目排序

    @TableField("score")
    private Integer score; // 题目分值
}