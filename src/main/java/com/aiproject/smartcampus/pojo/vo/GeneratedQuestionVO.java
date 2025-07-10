package com.aiproject.smartcampus.pojo.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 生成的题目VO
 */
@Data
public class GeneratedQuestionVO {
    private String questionType;
    private String questionContent;
    private String questionOptions; // JSON格式
    private String correctAnswer; // JSON格式
    private String explanation;
    private String difficultyLevel;
    private BigDecimal scorePoints;
    private String knowledgePointName;
}