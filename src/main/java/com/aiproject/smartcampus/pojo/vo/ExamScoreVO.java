package com.aiproject.smartcampus.pojo.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 考试成绩VO
 */
@Data
public class ExamScoreVO {
    private Integer examId;
    private String examTitle;
    private BigDecimal score;
    private BigDecimal maxScore;
    private BigDecimal percentage;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime submittedAt;
}