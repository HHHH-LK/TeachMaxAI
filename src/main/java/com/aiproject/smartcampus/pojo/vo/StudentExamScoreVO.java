package com.aiproject.smartcampus.pojo.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class StudentExamScoreVO {
    private Integer studentId;
    private String studentName;
    private String studentNumber;
    private BigDecimal score;
    private Integer rank; // 在该考试中的排名
    private LocalDateTime submittedAt;
}