package com.aiproject.smartcampus.pojo.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * @author lk_hhh
 * @description: 试卷-学生关联表
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("exam_students")
public class ExamStudent {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;// 主键ID，自增

    @TableField("examId")
    private Long examId; // 试卷ID

    @TableField("studentId")
    private Long studentId; // 学生ID

    @TableField("status")
    private Integer status; // 0-未参加,1-已参加

    @TableField("score")
    private Integer score; // 学生得分

    @TableField("startTime")
    private Date startTime; // 考试开始时间

    @TableField("submitTime")
    private Date submitTime; // 考试提交时间
}