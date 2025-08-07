package com.aiproject.smartcampus.pojo.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

@Data
public class ExamDetailVO {
    private Integer examId;
    private String title;
    private LocalDateTime examDate;
    private BigDecimal maxScore;
    private BigDecimal averageScore; // 该考试平均分

    // 学生考试详情
    private List<StudentExamScoreVO> studentScores;
}