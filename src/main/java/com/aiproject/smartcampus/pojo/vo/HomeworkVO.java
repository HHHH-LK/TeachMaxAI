package com.aiproject.smartcampus.pojo.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;

@Data
public class HomeworkVO {
    // 作业基本信息
    private Integer assignmentId;
    private String title;
    private String description;
    private LocalDateTime dueDate;
    private BigDecimal maxScore;
    private String status;

    // 作业统计信息
    private Integer submissionCount; // 提交人数
    private BigDecimal averageScore; // 平均分
    private BigDecimal submissionRate; // 提交率

    // 成绩分布
    private Integer scoreA; // 90-100
    private Integer scoreB; // 80-89
    private Integer scoreC; // 70-79
    private Integer scoreD; // 60-69
    private Integer scoreF; // 0-59
}