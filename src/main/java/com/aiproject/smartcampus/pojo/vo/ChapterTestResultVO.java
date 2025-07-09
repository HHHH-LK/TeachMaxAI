package com.aiproject.smartcampus.pojo.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

/**
 * @program: ss
 * @description: 章节测试整体结果VO
 * @author: lk_hhh
 * @create: 2025-07-09 15:12
 **/
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChapterTestResultVO {

    /**
     * 基本信息
     */
    private String studentId;
    private String chapterId;
    private String courseId;
    private String type;  // 测试类型：practice, test, exam
    
    /**
     * 测试统计信息
     */
    private Integer totalQuestions;      // 总题数
    private Integer answeredQuestions;   // 已答题数
    private Integer correctQuestions;    // 答对题数
    private Integer wrongQuestions;      // 答错题数
    private Integer unansweredQuestions; // 未答题数
    
    /**
     * 分数统计
     */
    private BigDecimal totalScore;       // 总分
    private BigDecimal earnedScore;      // 获得分数
    private BigDecimal accuracy;         // 正确率（百分比）
    private BigDecimal scoreRate;        // 得分率（百分比）
    
    /**
     * 各题型统计
     */
    private QuestionTypeStats singleChoiceStats;
    private QuestionTypeStats multipleChoiceStats;
    private QuestionTypeStats trueFalseStats;
    private QuestionTypeStats fillBlankStats;
    private QuestionTypeStats shortAnswerStats;
    
    /**
     * 时间信息
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime testStartTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime testEndTime;
    private Integer durationMinutes;     // 答题用时（分钟）
    
    /**
     * 详细答题结果
     */
    private List<StudentAnswerResultVO> questionResults;
    
    /**
     * 测试评价
     */
    private String overallComment;       // 整体评价
    private String suggestion;           // 学习建议
    
    /**
     * 题型统计内部类
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class QuestionTypeStats {
        private String questionType;
        private String questionTypeCn;
        private Integer totalCount;
        private Integer correctCount;
        private Integer wrongCount;
        private Integer unansweredCount;
        private BigDecimal totalScore;
        private BigDecimal earnedScore;
        private BigDecimal accuracy;  // 正确率
    }
    
    /**
     * 计算统计信息
     */
    public void calculateStatistics() {
        if (questionResults == null || questionResults.isEmpty()) {
            return;
        }
        
        // 基本统计
        this.totalQuestions = questionResults.size();
        this.answeredQuestions = (int) questionResults.stream()
                .filter(StudentAnswerResultVO::getIsAnswered)
                .count();
        this.correctQuestions = (int) questionResults.stream()
                .filter(q -> Boolean.TRUE.equals(q.getIsCorrect()))
                .count();
        this.wrongQuestions = (int) questionResults.stream()
                .filter(q -> Boolean.TRUE.equals(q.getIsAnswered()) && Boolean.FALSE.equals(q.getIsCorrect()))
                .count();
        this.unansweredQuestions = this.totalQuestions - this.answeredQuestions;
        
        // 分数统计
        this.totalScore = questionResults.stream()
                .map(StudentAnswerResultVO::getScorePoints)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        this.earnedScore = questionResults.stream()
                .map(StudentAnswerResultVO::calculateScoreEarned)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // 正确率和得分率
        if (this.totalQuestions > 0) {
            this.accuracy = BigDecimal.valueOf(correctQuestions)
                    .divide(BigDecimal.valueOf(totalQuestions), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        }
        
        if (this.totalScore.compareTo(BigDecimal.ZERO) > 0) {
            this.scoreRate = this.earnedScore
                    .divide(this.totalScore, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        }
        
        // 计算各题型统计
        calculateQuestionTypeStats();
        
        // 生成评价和建议
        generateCommentAndSuggestion();
    }
    
    /**
     * 计算各题型统计
     */
    private void calculateQuestionTypeStats() {
        this.singleChoiceStats = calculateStatsForType("single_choice", "单选题");
        this.multipleChoiceStats = calculateStatsForType("multiple_choice", "多选题");
        this.trueFalseStats = calculateStatsForType("true_false", "判断题");
        this.fillBlankStats = calculateStatsForType("fill_blank", "填空题");
        this.shortAnswerStats = calculateStatsForType("short_answer", "简答题");
    }
    
    /**
     * 计算指定题型的统计信息
     */
    private QuestionTypeStats calculateStatsForType(String questionType, String questionTypeCn) {
        List<StudentAnswerResultVO> typeQuestions = questionResults.stream()
                .filter(q -> questionType.equals(q.getQuestionType()))
                .toList();
        
        if (typeQuestions.isEmpty()) {
            return null;
        }
        
        int totalCount = typeQuestions.size();
        int correctCount = (int) typeQuestions.stream()
                .filter(q -> Boolean.TRUE.equals(q.getIsCorrect()))
                .count();
        int wrongCount = (int) typeQuestions.stream()
                .filter(q -> Boolean.TRUE.equals(q.getIsAnswered()) && Boolean.FALSE.equals(q.getIsCorrect()))
                .count();
        int unansweredCount = totalCount - correctCount - wrongCount;
        
        BigDecimal totalScore = typeQuestions.stream()
                .map(StudentAnswerResultVO::getScorePoints)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal earnedScore = typeQuestions.stream()
                .map(StudentAnswerResultVO::calculateScoreEarned)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal accuracy = totalCount > 0 ? 
                BigDecimal.valueOf(correctCount)
                        .divide(BigDecimal.valueOf(totalCount), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100)) : BigDecimal.ZERO;
        
        return QuestionTypeStats.builder()
                .questionType(questionType)
                .questionTypeCn(questionTypeCn)
                .totalCount(totalCount)
                .correctCount(correctCount)
                .wrongCount(wrongCount)
                .unansweredCount(unansweredCount)
                .totalScore(totalScore)
                .earnedScore(earnedScore)
                .accuracy(accuracy)
                .build();
    }
    
    /**
     * 生成评价和建议
     */
    private void generateCommentAndSuggestion() {
        if (scoreRate == null) {
            return;
        }
        
        // 根据得分率生成评价
        if (scoreRate.compareTo(BigDecimal.valueOf(90)) >= 0) {
            this.overallComment = "优秀！您对本章节内容掌握得非常好。";
            this.suggestion = "继续保持，可以尝试更有挑战性的练习。";
        } else if (scoreRate.compareTo(BigDecimal.valueOf(80)) >= 0) {
            this.overallComment = "良好！您对本章节内容掌握得不错。";
            this.suggestion = "建议复习错题，巩固薄弱知识点。";
        } else if (scoreRate.compareTo(BigDecimal.valueOf(60)) >= 0) {
            this.overallComment = "及格，但还有提升空间。";
            this.suggestion = "建议重新学习本章节内容，多做练习题。";
        } else {
            this.overallComment = "需要加强学习。";
            this.suggestion = "建议重新学习本章节的基础知识，并寻求老师或同学的帮助。";
        }
    }
    
    /**
     * 获取测试等级
     */
    public String getTestLevel() {
        if (scoreRate == null) {
            return "未知";
        }
        if (scoreRate.compareTo(BigDecimal.valueOf(90)) >= 0) {
            return "优秀";
        } else if (scoreRate.compareTo(BigDecimal.valueOf(80)) >= 0) {
            return "良好";
        } else if (scoreRate.compareTo(BigDecimal.valueOf(70)) >= 0) {
            return "中等";
        } else if (scoreRate.compareTo(BigDecimal.valueOf(60)) >= 0) {
            return "及格";
        } else {
            return "不及格";
        }
    }
}