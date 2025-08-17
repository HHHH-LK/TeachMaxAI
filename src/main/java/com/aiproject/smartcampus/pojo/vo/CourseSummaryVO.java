package com.aiproject.smartcampus.pojo.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class CourseSummaryVO {
    private Integer totalAssignments; // 课程总作业数
    private Integer totalStudents; // 学生总数
    private BigDecimal overallAverageScore; // 作业平均分a
    private BigDecimal overallSubmissionRate; // 作业总提交率
    private List<HomeworkVO> homeworkList; // 作业列表
}