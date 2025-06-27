package com.aiproject.smartcampus.pojo.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

//试卷主体类 作业同理
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("exams")
public class TeacherExam {
    @TableId(value = "exam_id", type = IdType.AUTO)
    private Long examId;// 考试ID，自增主键

    @TableField("teacher_id")
    private Long teacherId;// 发布教师ID

    @TableField("title")
    private String title;   // 试卷标题

    @TableField("type")
    private String type;// 试卷类型

    @TableField("difficulty")
    private String difficulty;// 试卷难度

    @TableField("description")
    private Integer totalQuestions; // 题目总数

    @TableField("total_score")
    private Integer totalScore; // 试卷总分

    @TableField("question_type")
    private String questionType; // 题目类型（单选/多选/判断/填空/简答等）

    @TableField("special_requirements")
    private String specialRequirements; // 特殊要求

    @TableField("status")
    private String status; // 试卷状态

    @TableField("start_time")
    private Date startTime; // 考试开始时间

    @TableField("end_time")
    private Date endTime;  // 考试结束时间

    @TableField("time_limit")
    private Integer timeLimit; // 时间限制（分钟）

    @TableField("is_public")
    private Boolean isPublic; // 是否公开试卷

    @TableField("show_answer_after_submit")
    private Boolean showAnswerAfterSubmit; // 提交后是否显示答案

    @TableField("create_timed")
    private Date createTime; // 创建时间

    @TableField("update_time")
    private Date updateTime;  // 更新时间
}