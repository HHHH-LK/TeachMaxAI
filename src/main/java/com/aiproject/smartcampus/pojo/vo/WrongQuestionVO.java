package com.aiproject.smartcampus.pojo.vo;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Builder;
import java.util.Date;

/**
 * 错题查询结果实体类
 * 对应增强版错题查询SQL的返回结果
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
     * 考试ID（可用于关联获取考试信息）
     */
    private Integer examId;

    /**
     * 章节ID
     */
    private Long chapterId;

    /**
     * 题目类型（如：single_choice, multiple_choice, fill_blank, true_false等）
     */
    private String questionType;

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
     * 获取题目类型的中文描述
     * @return 题目类型中文描述
     */
    public String getQuestionTypeCN() {
        if (questionType == null) {
            return "未知";
        }
        switch (questionType.toLowerCase()) {
            case "single_choice":
                return "单选题";
            case "multiple_choice":
                return "多选题";
            case "fill_blank":
                return "填空题";
            case "true_false":
                return "判断题";
            case "essay":
                return "问答题";
            case "calculation":
                return "计算题";
            default:
                return questionType;
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

    /**
     * 判断是否有关联的考试
     * @return 是否有关联的考试
     */
    public boolean hasExam() {
        return examId != null && examId > 0;
    }

    /**
     * 获取答题来源描述
     * @return 答题来源描述
     */
    public String getAnswerSource() {
        if (hasExam()) {
            return "考试答题";
        }
        return "练习答题";
    }

    /**
     * 判断是否为选择题类型
     * @return 是否为选择题
     */
    public boolean isChoiceType() {
        if (questionType == null) {
            return false;
        }
        String type = questionType.toLowerCase();
        return type.equals("single_choice") || type.equals("multiple_choice");
    }

    /**
     * 判断是否为主观题类型
     * @return 是否为主观题
     */
    public boolean isSubjectiveType() {
        if (questionType == null) {
            return false;
        }
        String type = questionType.toLowerCase();
        return type.equals("essay") || type.equals("fill_blank");
    }
}