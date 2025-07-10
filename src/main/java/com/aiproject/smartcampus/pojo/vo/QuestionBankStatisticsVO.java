package com.aiproject.smartcampus.pojo.vo;

import lombok.Data;

/**
 * 题库统计VO
 */
@Data
public class QuestionBankStatisticsVO {
    private Integer totalQuestions;
    private Integer singleChoiceCount;
    private Integer multipleChoiceCount;
    private Integer trueFalseCount;
    private Integer fillBlankCount;
    private Integer shortAnswerCount;
    private Integer easyCount;
    private Integer mediumCount;
    private Integer hardCount;
    
    // 计算各类型题目占比
    public Double getSingleChoiceRatio() {
        return totalQuestions > 0 ? (double) singleChoiceCount / totalQuestions : 0.0;
    }
    
    public Double getEasyRatio() {
        return totalQuestions > 0 ? (double) easyCount / totalQuestions : 0.0;
    }
    
    // ... 其他比例计算方法
}