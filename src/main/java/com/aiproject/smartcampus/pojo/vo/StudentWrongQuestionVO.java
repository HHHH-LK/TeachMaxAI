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
 * 学生错误题目信息VO
 * 用于封装学生答题记录及相关的题目、考试、课程信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentWrongQuestionVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 答题记录ID
     */
    private Integer answerId;

    /**
     * 考试ID
     */
    private Integer examId;

    /**
     * 学生ID
     */
    private Integer studentId;

    /**
     * 题目ID
     */
    private Integer questionId;

    /**
     * 学生答案
     */
    private String studentAnswer;

    /**
     * 获得分数
     */
    private BigDecimal scoreEarned;

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
     * 题目分值
     */
    private BigDecimal scorePoints;

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
     * 课程名称
     */
    private String courseName;

    /**
     * 学生真实姓名
     */
    private String studentName;

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
     * 获取得分率
     */
    public BigDecimal getScoreRate() {
        if (scoreEarned == null || scorePoints == null || scorePoints.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return scoreEarned.divide(scorePoints, 4, BigDecimal.ROUND_HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }

    /**
     * 判断是否为错题
     */
    public boolean isWrongAnswer() {
        return scoreEarned == null || scoreEarned.compareTo(scorePoints) < 0;
    }

    /**
     * 获取答题状态描述
     */
    public String getAnswerStatusDesc() {
        if (scoreEarned == null) {
            return "未评分";
        }
        if (scoreEarned.compareTo(scorePoints) == 0) {
            return "完全正确";
        } else if (scoreEarned.compareTo(BigDecimal.ZERO) == 0) {
            return "完全错误";
        } else {
            return "部分正确";
        }
    }

    @Override
    public String toString() {
        return "学生错误题目信息{" +
                "答题记录ID=" + answerId +
                ", 考试ID=" + examId +
                ", 学生ID=" + studentId +
                ", 题目ID=" + questionId +
                ", 学生答案='" + studentAnswer + '\'' +
                ", 获得分数=" + scoreEarned +
                ", 题目内容='" + questionContent + '\'' +
                ", 题目类型='" + getQuestionTypeDesc() + '\'' +
                ", 正确答案='" + correctAnswer + '\'' +
                ", 答案解析='" + explanation + '\'' +
                ", 难度等级='" + getDifficultyLevelDesc() + '\'' +
                ", 题目分值=" + scorePoints +
                ", 考试标题='" + examTitle + '\'' +
                ", 考试时间=" + examDate +
                ", 课程名称='" + courseName + '\'' +
                ", 学生姓名='" + studentName + '\'' +
                ", 得分率=" + getScoreRate() + "%" +
                ", 答题状态='" + getAnswerStatusDesc() + '\'' +
                '}';
    }
}


/**
 * 如果你使用的是JPA/Hibernate，可以考虑使用以下注解版本
 */
/*
import javax.persistence.*;

@Entity
@Table(name = "student_wrong_question_view")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentWrongQuestionEntity implements Serializable {

    @Id
    @Column(name = "answer_id")
    private Integer answerId;

    @Column(name = "exam_id")
    private Integer examId;

    @Column(name = "student_id")
    private Integer studentId;

    @Column(name = "question_id")
    private Integer questionId;

    @Column(name = "student_answer", columnDefinition = "TEXT")
    private String studentAnswer;

    @Column(name = "score_earned", precision = 4, scale = 1)
    private BigDecimal scoreEarned;

    @Column(name = "question_content", columnDefinition = "TEXT")
    private String questionContent;

    @Enumerated(EnumType.STRING)
    @Column(name = "question_type")
    private QuestionType questionType;

    @Column(name = "correct_answer", columnDefinition = "JSON")
    private String correctAnswer;

    @Column(name = "explanation", columnDefinition = "TEXT")
    private String explanation;

    @Enumerated(EnumType.STRING)
    @Column(name = "difficulty_level")
    private DifficultyLevel difficultyLevel;

    @Column(name = "score_points", precision = 4, scale = 1)
    private BigDecimal scorePoints;

    @Column(name = "exam_title", length = 200)
    private String examTitle;

    @Column(name = "exam_date")
    private LocalDateTime examDate;

    @Column(name = "course_name", length = 100)
    private String courseName;

    @Column(name = "student_name", length = 50)
    private String studentName;

    // 枚举定义
    public enum QuestionType {
        SINGLE_CHOICE("single_choice", "单选题"),
        MULTIPLE_CHOICE("multiple_choice", "多选题"),
        TRUE_FALSE("true_false", "判断题"),
        FILL_BLANK("fill_blank", "填空题"),
        SHORT_ANSWER("short_answer", "简答题");

        private final String code;
        private final String desc;

        QuestionType(String code, String desc) {
            this.code = code;
            this.desc = desc;
        }

        public String getCode() {
            return code;
        }

        public String getDesc() {
            return desc;
        }
    }

    public enum DifficultyLevel {
        EASY("easy", "简单"),
        MEDIUM("medium", "中等"),
        HARD("hard", "困难");

        private final String code;
        private final String desc;

        DifficultyLevel(String code, String desc) {
            this.code = code;
            this.desc = desc;
        }

        public String getCode() {
            return code;
        }

        public String getDesc() {
            return desc;
        }
    }
}
*/