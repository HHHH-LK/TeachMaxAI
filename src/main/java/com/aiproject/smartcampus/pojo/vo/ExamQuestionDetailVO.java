package com.aiproject.smartcampus.pojo.vo;

import cn.hutool.core.annotation.Alias;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExamQuestionDetailVO {

    /**
     * 题目ID
     */
    private Integer questionId;

    private Integer questionOrder;  // 题目在试卷中的顺序

    /**
     * 题目类型：single_choice-单选题, multiple_choice-多选题,
     * true_false-判断题, fill_blank-填空题, short_answer-简答题
     */
    private String questionType;

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
     * 题目分值
     */
    private BigDecimal scorePoints;

    /**
     * 知识点名称
     */
    private String pointName;

    /**
     * 章节名称
     */
    private String chapterName;

    /**
     * 章节ID
     */
    private Integer chapterId;

    // ========== 学生答题情况相关字段 ==========

    /**
     * 学生答案（如果已答过）
     */
    private String studentAnswer;

    /**
     * 是否正确（如果已答过）
     */
    private Boolean isCorrect;

    /**
     * 获得分数（如果已答过）
     */
    private BigDecimal scoreEarned;

    /**
     * 答题记录ID（如果已答过）
     */
    private Integer answerId;

    // ========== 扩展字段（保持原有功能） ==========

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    /**
     * 该题的权重
     */
    private Integer t;

    // ========== 辅助方法 ==========

    /**
     * 获取题目类型的中文描述
     * @return 题目类型中文描述
     */
    public String getQuestionTypeCn() {
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
            case "short_answer":
                return "简答题";
            default:
                return questionType;
        }
    }

    /**
     * 获取难度等级的中文描述
     * @return 难度等级中文描述
     */
    public String getDifficultyLevelCn() {
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
     * 判断学生是否已经答过这道题
     * @return 是否已答题
     */
    public boolean isAnswered() {
        return answerId != null;
    }

    /**
     * 判断学生是否未作答
     * @return 是否未作答
     */
    public boolean isNotAnswered() {
        return !isAnswered() || studentAnswer == null || studentAnswer.trim().isEmpty();
    }

    /**
     * 获取答题状态描述
     * @return 答题状态描述
     */
    public String getAnswerStatus() {
        if (!isAnswered()) {
            return "未答题";
        }
        if (isNotAnswered()) {
            return "未作答";
        }
        if (Boolean.TRUE.equals(isCorrect)) {
            return "答案正确";
        }
        return "答案错误";
    }

    /**
     * 获取得分率（百分比）
     * @return 得分率
     */
    public String getScoreRate() {
        if (!isAnswered() || scorePoints == null || scorePoints.compareTo(BigDecimal.ZERO) == 0) {
            return "0%";
        }
        if (scoreEarned == null) {
            return "0%";
        }

        BigDecimal rate = scoreEarned.divide(scorePoints, 4, BigDecimal.ROUND_HALF_UP)
                .multiply(new BigDecimal("100"));
        return rate.setScale(0, BigDecimal.ROUND_HALF_UP) + "%";
    }

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
     * 获取完整的题目描述（包含章节和知识点信息）
     * @return 完整题目描述
     */
    public String getFullQuestionInfo() {
        StringBuilder info = new StringBuilder();
        if (chapterName != null) {
            info.append("【").append(chapterName).append("】");
        }
        if (pointName != null) {
            info.append("【").append(pointName).append("】");
        }
        info.append(" ").append(getQuestionTypeCn());
        info.append("(").append(getDifficultyLevelCn()).append(")");
        return info.toString();
    }

    /**
     * 重写toString方法，便于调试
     */
    @Override
    public String toString() {
        return String.format("ChapterQuestionDetailVO{id=%d, type='%s', difficulty='%s', score=%s, answered=%s}",
                questionId, getQuestionTypeCn(), getDifficultyLevelCn(),
                scorePoints != null ? scorePoints.toString() : "未设置",
                isAnswered() ? "已答题" : "未答题");
    }
}
