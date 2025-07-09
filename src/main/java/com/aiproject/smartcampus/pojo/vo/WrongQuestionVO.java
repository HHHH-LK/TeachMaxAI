package com.aiproject.smartcampus.pojo.vo;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Builder;

/**
 * 错题查询结果实体类
 * 对应简化版错题查询SQL的返回结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WrongQuestionVO {
    
    /**
     * 用户答案
     */
    private String studentAnswer;
    
    /**
     * 题目内容
     */
    private String questionContent;
    
    /**
     * 正确答案
     */
    private String correctAnswer;
    
    /**
     * 题目解析
     */
    private String explanation;
    
    /**
     * 知识点名称
     */
    private String pointName;
    
    /**
     * 章节名称
     */
    private String chapterName;
    
    /**
     * 题目难度（easy, medium, hard）
     */
    private String difficultyLevel;
    
    /**
     * 答题ID（用于排序）
     */
    private Integer answerId;
    
    /**
     * 获取难度等级的中文描述
     * @return 难度等级中文描述
     */
    public String getDifficultyLevelCN() {
        if (difficultyLevel == null) {
            return "未知";
        }
        switch (difficultyLevel.toLowerCase()) {
            case "easy":
                return "简单";
            case "medium":
                return "中等";
            case "hard":
                return "困难";
            default:
                return difficultyLevel;
        }
    }
    
    /**
     * 判断是否未作答
     * @return 是否未作答
     */
    public boolean isNotAnswered() {
        return studentAnswer == null || studentAnswer.trim().isEmpty();
    }
    
    /**
     * 获取答题状态描述
     * @return 答题状态描述
     */
    public String getAnswerStatus() {
        if (isNotAnswered()) {
            return "未作答";
        }
        return "答案错误";
    }
}
