package com.aiproject.smartcampus.pojo.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 考试创建请求VO
 */
@Data
public class ExamCreationRequestVO {
    private Integer courseId;
    private String examTitle;
    private String examDescription;
    private Integer durationMinutes;
    private BigDecimal totalScore;
    private Map<String, Double> difficultyDistribution; // 难度分布
    private Map<String, Integer> questionTypeDistribution; // 题型分布
    private List<String> knowledgePointFocus; // 重点知识点
}