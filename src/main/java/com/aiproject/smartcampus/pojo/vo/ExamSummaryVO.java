package com.aiproject.smartcampus.pojo.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ExamSummaryVO {
    // 课程总体统计
    private Integer studentCount; // 学生总数
    private Integer examCount; // 总考试数
    private BigDecimal overallAverageScore; // 总平均分
    private BigDecimal passRate; // 总及格率（>=60分）

    // 成绩分布
    private Integer scoreA; // 90-100
    private Integer scoreB; // 80-89
    private Integer scoreC; // 70-79
    private Integer scoreD; // 60-69
    private Integer scoreF; // 0-59

    // 各考试详情
    private List<ExamDetailVO> examDetails;
}