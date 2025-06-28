package com.aiproject.smartcampus.pojo.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * @program: SmartCampus
 * @description: 题库表实体类
 * @author: lk
 * @create: 2025-06-28
 **/
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("question_bank")
public class QuestionBank implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 题目ID
     */
    @TableId(value = "question_id", type = IdType.AUTO)
    private Integer questionId;

    /**
     * 所属课程ID
     */
    @TableField("course_id")
    private Integer courseId;

    /**
     * 关联知识点ID
     */
    @TableField("point_id")
    private Integer pointId;

    /**
     * 题目类型
     */
    @TableField("question_type")
    private QuestionType questionType;

    /**
     * 题目内容
     */
    @TableField("question_content")
    private String questionContent;

    /**
     * 题目选项(JSON格式)
     * 格式: [{"label":"A","content":"选项内容","is_correct":true}]
     */
    @TableField("question_options")
    private String questionOptions;

    /**
     * 正确答案(JSON格式)，支持多种格式
     */
    @TableField("correct_answer")
    private String correctAnswer;

    /**
     * 答案解析
     */
    @TableField("explanation")
    private String explanation;

    /**
     * 难度等级
     */
    @TableField("difficulty_level")
    private DifficultyLevel difficultyLevel;

    /**
     * 题目分值
     */
    @TableField("score_points")
    private BigDecimal scorePoints;

    /**
     * 创建教师ID
     */
    @TableField("created_by")
    private Integer createdBy;

    /**
     * 创建时间
     */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    // ==================== 关联对象（不映射到数据库）====================

    /**
     * 关联课程信息
     */
    @TableField(exist = false)
    private Course course;

    /**
     * 关联知识点信息
     */
    @TableField(exist = false)
    private KnowledgePoint knowledgePoint;

    /**
     * 创建者信息
     */
    @TableField(exist = false)
    private Teacher creator;

    // ==================== 枚举定义 ====================

    /**
     * 题目类型枚举
     */
    public enum QuestionType {
        SINGLE_CHOICE("single_choice", "单选题"),
        MULTIPLE_CHOICE("multiple_choice", "多选题"),
        TRUE_FALSE("true_false", "判断题"),
        FILL_BLANK("fill_blank", "填空题"),
        SHORT_ANSWER("short_answer", "简答题");

        private final String value;
        private final String description;

        QuestionType(String value, String description) {
            this.value = value;
            this.description = description;
        }

        public String getValue() { return value; }
        public String getDescription() { return description; }

        public static QuestionType fromValue(String value) {
            for (QuestionType type : QuestionType.values()) {
                if (type.getValue().equals(value)) {
                    return type;
                }
            }
            return SINGLE_CHOICE; // 默认值
        }
    }

    /**
     * 难度等级枚举
     */
    public enum DifficultyLevel {
        EASY("easy", "简单"),
        MEDIUM("medium", "中等"),
        HARD("hard", "困难");

        private final String value;
        private final String description;

        DifficultyLevel(String value, String description) {
            this.value = value;
            this.description = description;
        }

        public String getValue() { return value; }
        public String getDescription() { return description; }

        public static DifficultyLevel fromValue(String value) {
            for (DifficultyLevel level : DifficultyLevel.values()) {
                if (level.getValue().equals(value)) {
                    return level;
                }
            }
            return MEDIUM; // 默认值
        }
    }

    // ==================== 业务方法 ====================

    /**
     * 判断是否为选择题（单选或多选）
     */
    public boolean isChoiceQuestion() {
        return QuestionType.SINGLE_CHOICE.equals(this.questionType) ||
                QuestionType.MULTIPLE_CHOICE.equals(this.questionType);
    }

    /**
     * 判断是否为单选题
     */
    public boolean isSingleChoice() {
        return QuestionType.SINGLE_CHOICE.equals(this.questionType);
    }

    /**
     * 判断是否为多选题
     */
    public boolean isMultipleChoice() {
        return QuestionType.MULTIPLE_CHOICE.equals(this.questionType);
    }

    /**
     * 判断是否为判断题
     */
    public boolean isTrueFalse() {
        return QuestionType.TRUE_FALSE.equals(this.questionType);
    }

    /**
     * 判断是否为填空题
     */
    public boolean isFillBlank() {
        return QuestionType.FILL_BLANK.equals(this.questionType);
    }

    /**
     * 判断是否为简答题
     */
    public boolean isShortAnswer() {
        return QuestionType.SHORT_ANSWER.equals(this.questionType);
    }

    /**
     * 判断是否为客观题（选择题、判断题）
     */
    public boolean isObjectiveQuestion() {
        return isChoiceQuestion() || isTrueFalse();
    }

    /**
     * 判断是否为主观题（填空题、简答题）
     */
    public boolean isSubjectiveQuestion() {
        return isFillBlank() || isShortAnswer();
    }

    /**
     * 获取难度等级描述
     */
    public String getDifficultyDescription() {
        return this.difficultyLevel != null ? this.difficultyLevel.getDescription() : "";
    }

    /**
     * 获取题型描述
     */
    public String getQuestionTypeDescription() {
        return this.questionType != null ? this.questionType.getDescription() : "";
    }

    /**
     * 获取分值（如果为null则返回默认分值1.0）
     */
    public BigDecimal getScorePointsOrDefault() {
        return this.scorePoints != null ? this.scorePoints : BigDecimal.ONE;
    }
}