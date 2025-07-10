package com.aiproject.smartcampus.pojo.vo;

import lombok.Data;
import java.math.BigDecimal;

/**
 * 学生考试作答情况VO
 * 对应SQL查询结果的封装
 */
@Data
public class StudentExamAnswerVO {

    /**
     * 答题记录ID
     */
    private Integer answerId;

    /**
     * 题目在试卷中的顺序
     */
    private Integer questionOrder;

    /**
     * 题目ID
     */
    private Integer questionId;

    /**
     * 题目类型
     */
    private String questionType;

    /**
     * 题目内容
     */
    private String questionContent;

    /**
     * 题目选项(JSON格式)
     */
    private String questionOptions;

    /**
     * 正确答案(JSON格式)
     */
    private String correctAnswer;

    /**
     * 学生答案
     */
    private String studentAnswer;

    /**
     * 是否正确 (1:正确, 0:错误)
     */
    private Boolean isCorrect;

    /**
     * 获得分数
     */
    private BigDecimal scoreEarned;

    /**
     * 该题满分
     */
    private BigDecimal maxScore;

    /**
     * 答案解析
     */
    private String explanation;

    /**
     * 题目难度等级
     */
    private String difficultyLevel;

    /**
     * 关联的知识点名称
     */
    private String knowledgePointName;
}