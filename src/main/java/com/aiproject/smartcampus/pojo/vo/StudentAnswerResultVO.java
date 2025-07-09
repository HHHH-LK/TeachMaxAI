package com.aiproject.smartcampus.pojo.vo;

import com.aiproject.smartcampus.pojo.dto.StudentAnswerDTO;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * @program: ss
 * @description: 学生章节测试答题结果VO（单题结果）
 * @author: lk_hhh
 * @create: 2025-07-09 15:12
 **/
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentAnswerResultVO {

    /**
     * 题目基本信息（从ChapterQuestionDetailVO复制而来）
     */
    private Integer questionId;
    private String questionType;
    private String questionTypeCn;
    private String questionContent;
    private String questionOptions;
    private String correctAnswer;
    private String explanation;
    private String difficultyLevel;
    private String difficultyLevelCn;
    private BigDecimal scorePoints;
    private String chapterQuestionType;
    private String chapterQuestionTypeCn;
    private Integer t;  // 题目权重/排序

    /**
     * 学生答题信息
     */
    private String studentAnswer;        // 学生的答案
    private Boolean isCorrect;           // 是否答对
    private BigDecimal scoreEarned;      // 获得分数

    /**
     * 答题状态
     */
    private Boolean isAnswered;          // 是否已答题（用于区分未答和答错）
    private String answerStatus;         // 答题状态：correct-正确，wrong-错误，unanswered-未答

    /**
     * 答案对比信息（便于前端展示）
     */
    private String studentAnswerDisplay; // 学生答案显示文本（格式化后）
    private String correctAnswerDisplay; // 正确答案显示文本（格式化后）

    /**
     * 时间信息
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime answeredAt;    // 答题时间

    /**
     * 计算获得分数
     * @return 根据是否正确计算得分
     */
    public BigDecimal calculateScoreEarned() {
        if (scorePoints == null) {
            return BigDecimal.ZERO;
        }
        if (Boolean.TRUE.equals(isCorrect)) {
            return scorePoints;
        }
        return BigDecimal.ZERO;
    }

    /**
     * 设置答题状态
     */
    public void setAnswerStatusByCorrectness() {
        if (studentAnswer == null || studentAnswer.trim().isEmpty()) {
            this.answerStatus = "unanswered";
            this.isAnswered = false;
            this.isCorrect = false;
        } else {
            this.isAnswered = true;
            if (Boolean.TRUE.equals(isCorrect)) {
                this.answerStatus = "correct";
            } else {
                this.answerStatus = "wrong";
            }
        }
    }

    /**
     * 判断是否为选择题
     */
    public boolean isChoiceQuestion() {
        return "single_choice".equals(questionType) || "multiple_choice".equals(questionType);
    }

    /**
     * 判断是否为主观题
     */
    public boolean isSubjectiveQuestion() {
        return "fill_blank".equals(questionType) || "short_answer".equals(questionType);
    }

    /**
     * 获取题目内容预览
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
     * 获取答题结果描述
     */
    public String getResultDescription() {
        if (!isAnswered) {
            return "未答题";
        }
        if (Boolean.TRUE.equals(isCorrect)) {
            return "答题正确，得分：" + calculateScoreEarned() + "分";
        } else {
            return "答题错误，得分：0分";
        }
    }

    /**
     * 重写toString方法
     */
    @Override
    public String toString() {
        return String.format("StudentAnswerResultVO{questionId=%d, type='%s', correct=%s, score=%s}",
                questionId, questionTypeCn, isCorrect, scoreEarned);
    }
}