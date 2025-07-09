
package com.aiproject.smartcampus.pojo.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 章节测试题详细信息VO
 * 对应章节测试题查询的结果字段
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class  ChapterQuestionDetailVO {
    
    /**
     * 题目ID
     */
    private Integer questionId;
    
    /**
     * 题目类型：single_choice-单选题, multiple_choice-多选题, 
     * true_false-判断题, fill_blank-填空题, short_answer-简答题
     */
    private String questionType;
    
    /**
     * 题目类型中文描述
     */
    private String questionTypeCn;
    
    /**
     * 题目内容
     */
    private String questionContent;
    
    /**
     * 题目选项(JSON格式)
     * 格式: [{"label":"A","content":"选项内容","is_correct":true}]
     */
    private String questionOptions;
    
    /**
     * 正确答案(JSON格式)
     */
    private String correctAnswer;
    
    /**
     * 答案解析
     */
    private String explanation;
    
    /**
     * 难度等级：easy-简单, medium-中等, hard-困难
     */
    private String difficultyLevel;
    
    /**
     * 难度等级中文描述
     */
    private String difficultyLevelCn;
    
    /**
     * 题目分值
     */
    private BigDecimal scorePoints;
    
    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
    
    /**
     * 章节题目类型：practice-练习题, test-测试题, exam-考试题
     */
    private String chapterQuestionType;
    
    /**
     * 章节题目类型中文描述
     */
    private String chapterQuestionTypeCn;

    /**
     * 该题的权重
     * */
    private Integer t;

    
    /**
     * 判断是否为选择题（单选或多选）
     * @return true-选择题，false-非选择题
     */
    public boolean isChoiceQuestion() {
        return "single_choice".equals(questionType) || "multiple_choice".equals(questionType);
    }
    
    /**
     * 判断是否为主观题
     * @return true-主观题，false-客观题
     */
    public boolean isSubjectiveQuestion() {
        return "fill_blank".equals(questionType) || "short_answer".equals(questionType);
    }
    
    /**
     * 判断是否为客观题
     * @return true-客观题，false-主观题
     */
    public boolean isObjectiveQuestion() {
        return !isSubjectiveQuestion();
    }
    
    /**
     * 获取题目内容预览（用于列表显示）
     * @param maxLength 最大长度
     * @return 题目内容预览
     */
    public String getQuestionContentPreview(int maxLength) {
        if (questionContent == null || questionContent.isEmpty()) {
            return "题目内容为空";
        }
        
        if (questionContent.length() <= maxLength) {
            return questionContent;
        }
        
        return questionContent.substring(0, maxLength) + "...";
    }
    
    /**
     * 获取默认长度的题目内容预览
     * @return 题目内容预览（最多80个字符）
     */
    public String getQuestionContentPreview() {
        return getQuestionContentPreview(80);
    }
    
    /**
     * 获取分值描述
     * @return 分值描述，如："5.0分"
     */
    public String getScoreDescription() {
        if (scorePoints == null) {
            return "未设置分值";
        }
        return scorePoints.toString() + "分";
    }
    
    /**
     * 判断是否有解析
     * @return true-有解析，false-无解析
     */
    public boolean hasExplanation() {
        return explanation != null && !explanation.trim().isEmpty();
    }
    
    /**
     * 获取解析预览
     * @param maxLength 最大长度
     * @return 解析预览
     */
    public String getExplanationPreview(int maxLength) {
        if (!hasExplanation()) {
            return "暂无解析";
        }
        
        if (explanation.length() <= maxLength) {
            return explanation;
        }
        
        return explanation.substring(0, maxLength) + "...";
    }
    
    /**
     * 获取题目难度权重（用于排序）
     * @return 难度权重：简单=1，中等=2，困难=3
     */
    public int getDifficultyWeight() {
        if (difficultyLevel == null) {
            return 0;
        }
        switch (difficultyLevel) {
            case "easy":
                return 1;
            case "medium":
                return 2;
            case "hard":
                return 3;
            default:
                return 0;
        }
    }
    
    /**
     * 获取题目类型权重（用于排序）
     * @return 类型权重
     */
    public int getQuestionTypeWeight() {
        if (questionType == null) {
            return 0;
        }
        switch (questionType) {
            case "single_choice":
                return 1;
            case "multiple_choice":
                return 2;
            case "true_false":
                return 3;
            case "fill_blank":
                return 4;
            case "short_answer":
                return 5;
            default:
                return 0;
        }
    }
    
    /**
     * 判断是否为有效的题目
     * @return true-有效，false-无效
     */
    public boolean isValidQuestion() {
        return questionId != null && 
               questionType != null && 
               !questionType.trim().isEmpty() &&
               questionContent != null && 
               !questionContent.trim().isEmpty();
    }
    
    /**
     * 重写toString方法，便于调试
     */
    @Override
    public String toString() {
        return String.format("ChapterQuestionDetailVO{id=%d, type='%s', difficulty='%s', score=%s}", 
                            questionId, questionTypeCn, difficultyLevelCn, 
                            scorePoints != null ? scorePoints.toString() : "未设置");
    }
}