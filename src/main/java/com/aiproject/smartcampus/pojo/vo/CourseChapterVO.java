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
 * 课程章节信息VO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseChapterVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer chapterId;
    private Integer courseId;
    private String chapterName;
    private Integer chapterOrder;
    private String description;
    private String difficultyLevel;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updatedAt;

    private String courseName;
    private String semester;

    private BigDecimal progressRate;
    private String studentMasteryLevel;
    private Integer studentStudyTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime studentLastStudyAt;

    private Integer currentMaterialId;
    private String completedMaterials;

    private Integer totalKnowledgePoints;
    private Integer totalMaterials;
    private Integer coreKnowledgePoints;

    // ---------- 派生字段 / 方法 ----------

    public String getDifficultyLevelDesc() {
        if (difficultyLevel == null) {
            return "";
        }
        return switch (difficultyLevel) {
            case "easy" -> "简单";
            case "medium" -> "中等";
            case "hard" -> "困难";
            default -> difficultyLevel;
        };
    }

    public String getStudentMasteryLevelDesc() {
        if (studentMasteryLevel == null) {
            return "";
        }
        return switch (studentMasteryLevel) {
            case "not_started" -> "未开始";
            case "learning" -> "学习中";
            case "completed" -> "已完成";
            case "mastered" -> "已掌握";
            default -> studentMasteryLevel;
        };
    }

    public String getStudyTimeDesc() {
        if (studentStudyTime == null || studentStudyTime == 0) {
            return "暂无学习记录";
        }
        int hours = studentStudyTime / 60;
        int minutes = studentStudyTime % 60;
        return hours > 0 ? hours + "小时" + (minutes > 0 ? minutes + "分钟" : "") : minutes + "分钟";
    }

    public boolean isStarted() {
        return studentMasteryLevel != null && !"not_started".equals(studentMasteryLevel);
    }

    public boolean isCompleted() {
        return "completed".equals(studentMasteryLevel) || "mastered".equals(studentMasteryLevel);
    }

    public String getProgressStatusDesc() {
        if (progressRate == null || progressRate.compareTo(BigDecimal.ZERO) == 0) {
            return "未开始";
        } else if (progressRate.compareTo(new BigDecimal("100")) >= 0) {
            return "已完成";
        } else {
            return "进行中 (" + progressRate + "%)";
        }
    }

    public String getLearningAdvice() {
        if (!isStarted()) {
            return "建议开始学习本章节";
        } else if (progressRate != null && progressRate.compareTo(new BigDecimal("50")) < 0) {
            return "继续加强学习";
        } else if (progressRate != null && progressRate.compareTo(new BigDecimal("80")) < 0) {
            return "学习进度良好，继续保持";
        } else if (isCompleted()) {
            return "已完成学习，可以复习巩固";
        } else {
            return "继续学习";
        }
    }

    @Override
    public String toString() {
        return "课程章节信息{" +
                "章节ID=" + chapterId +
                ", 课程ID=" + courseId +
                ", 章节名称='" + chapterName + '\'' +
                ", 章节顺序=" + chapterOrder +
                ", 章节描述='" + description + '\'' +
                ", 难度等级='" + getDifficultyLevelDesc() + '\'' +
                ", 创建时间=" + createdAt +
                ", 更新时间=" + updatedAt +
                ", 课程名称='" + courseName + '\'' +
                ", 学期='" + semester + '\'' +
                ", 学习进度=" + (progressRate != null ? progressRate + "%" : "0%") +
                ", 掌握程度='" + getStudentMasteryLevelDesc() + '\'' +
                ", 学习时长='" + getStudyTimeDesc() + '\'' +
                ", 最后学习时间=" + studentLastStudyAt +
                ", 当前资料ID=" + currentMaterialId +
                ", 总知识点数=" + totalKnowledgePoints +
                ", 总资料数=" + totalMaterials +
                ", 核心知识点数=" + coreKnowledgePoints +
                ", 进度状态='" + getProgressStatusDesc() + '\'' +
                ", 学习建议='" + getLearningAdvice() + '\'' +
                '}';
    }
}
