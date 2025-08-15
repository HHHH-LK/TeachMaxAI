package com.aiproject.smartcampus.pojo.vo;

import com.aiproject.smartcampus.pojo.po.Exam;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExamInfoVO {
    private Integer examId; // 考试ID
    private Integer courseId; // 课程ID
    private String title; // 考试名称
    private LocalDateTime examDate; // 考试时间
    private Integer duration; // 考试时长（分钟）
    private BigDecimal max_score; // 考试总分
    private Exam.ExamStatus status; // 考试状态
    private LocalDateTime createdAt;
    private Integer questionCount; // 提交人数
    private Integer markingCount; // 已批改人数
}
