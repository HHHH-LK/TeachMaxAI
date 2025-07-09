package com.aiproject.smartcampus.pojo.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.io.Serializable;

/**
 * 考试题目信息VO
 * 用于封装考试中的题目详细信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExamQuestionVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 试卷题目关联ID
     */
    private Integer paperQuestionId;

    /**
     * 试卷ID
     */
    private Integer paperId;

    /**
     * 题目ID
     */
    private Integer questionId;

    /**
     * 题目在试卷中的顺序
     */
    private Integer questionOrder;

    /**
     * 自定义分值（如果为空则使用题目原始分值）
     */
    private BigDecimal customScore;

    /**
     * 题目内容
     */
    private String questionContent;

    /**
     * 题目类型
     * single_choice: 单选题
     * multiple_choice: 多选题
     * true_false: 判断题
     * fill_blank: 填空题
     * short_answer: 简答题
     */
    private String questionType;

    /**
     * 题目选项（JSON格式）
     * 格式: [{"label":"A","content":"选项内容","is_correct":false}]
     */
    private String questionOptions;

    /**
     * 正确答案（JSON格式）
     */
    private String correctAnswer;

    /**
     * 答案解析
     */
    private String explanation;

    /**
     * 难度等级
     * easy: 简单
     * medium: 中等
     * hard: 困难
     */
    private String difficultyLevel;

    /**
     * 题目原始分值
     */
    private BigDecimal scorePoints;

    /**
     * 关联知识点ID
     */
    private Integer pointId;

    /**
     * 试卷标题
     */
    private String paperTitle;

    /**
     * 试卷总分
     */
    private BigDecimal paperTotalScore;

    /**
     * 考试ID
     */
    private Integer examId;

    /**
     * 考试标题
     */
    private String examTitle;

    /**
     * 考试时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime examDate;

    /**
     * 考试时长（分钟）
     */
    private Integer durationMinutes;

    /**
     * 考试满分
     */
    private BigDecimal examMaxScore;

    /**
     * 知识点名称
     */
    private String pointName;

    /**
     * 知识点描述
     */
    private String pointDescription;

    /**
     * 获取题目最终分值（优先使用自定义分值）
     */
    public BigDecimal getFinalScore() {
        return customScore != null ? customScore : scorePoints;
    }

    /**
     * 获取题目类型的中文描述
     */
    public String getQuestionTypeDesc() {
        if (questionType == null) {
            return "";
        }
        switch (questionType) {
            case "single_choice":
                return "单选题";
            case "multiple_choice":
                return "多选题";
            case "true_false":
                return "判断题";
            case "fill_blank":
                return "填空题";
            case "short_answer":
                return "简答题";
            default:
                return questionType;
        }
    }

    /**
     * 获取难度等级的中文描述
     */
    public String getDifficultyLevelDesc() {
        if (difficultyLevel == null) {
            return "";
        }
        switch (difficultyLevel) {
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
     * 判断是否为客观题（可自动评分）
     */
    public boolean isObjectiveQuestion() {
        return "single_choice".equals(questionType) || 
               "multiple_choice".equals(questionType) || 
               "true_false".equals(questionType);
    }

    /**
     * 判断是否为主观题（需人工评分）
     */
    public boolean isSubjectiveQuestion() {
        return "fill_blank".equals(questionType) || 
               "short_answer".equals(questionType);
    }

    @Override
    public String toString() {
        return "考试题目信息{" +
                "试卷题目ID=" + paperQuestionId +
                ", 试卷ID=" + paperId +
                ", 题目ID=" + questionId +
                ", 题目顺序=" + questionOrder +
                ", 自定义分值=" + customScore +
                ", 题目内容='" + questionContent + '\'' +
                ", 题目类型='" + getQuestionTypeDesc() + '\'' +
                ", 题目选项='" + questionOptions + '\'' +
                ", 正确答案='" + correctAnswer + '\'' +
                ", 答案解析='" + explanation + '\'' +
                ", 难度等级='" + getDifficultyLevelDesc() + '\'' +
                ", 原始分值=" + scorePoints +
                ", 最终分值=" + getFinalScore() +
                ", 知识点ID=" + pointId +
                ", 试卷标题='" + paperTitle + '\'' +
                ", 考试ID=" + examId +
                ", 考试标题='" + examTitle + '\'' +
                ", 考试时间=" + examDate +
                ", 知识点名称='" + pointName + '\'' +
                '}';
    }
}