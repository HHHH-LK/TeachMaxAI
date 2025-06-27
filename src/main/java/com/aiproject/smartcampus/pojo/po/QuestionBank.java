package com.aiproject.smartcampus.pojo.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * @description: 题目库表
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("question_bank")
public class QuestionBank {
    @TableId(value = "question_id", type = IdType.AUTO)
    private Long questionId; // 题目ID，自增主键

    @TableField("question_type")
    private String questionType; // 题目类型（单选、多选、判断、填空、简答等）

    @TableField("content")
    private String content; // 题目内容

    @TableField("options")
    private String options; // 题目选项（适用于选择题）

    @TableField("answer")
    private String answer; // 正确答案

    @TableField("subject")
    private String subject; // 题目所属学科

    @TableField("difficulty")
    private String difficulty; // 题目难度（简单、中等、困难）

    @TableField("knowledge_points")
    private String knowledgePoints; // 知识点（JSON格式存储）

    @TableField("status")
    private Date createTime;

    @TableField("status")
    private Date updateTime;
}