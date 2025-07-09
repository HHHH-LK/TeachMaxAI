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
 * 学生知识点信息VO
 * 用于封装学生知识点掌握情况及相关的课程、章节、父级知识点信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentKnowledgePointVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 知识点ID
     */
    private Integer pointId;

    /**
     * 知识点名称
     */
    private String pointName;

    /**
     * 知识点描述
     */
    private String description;

    /**
     * 知识点难度等级
     * easy: 简单
     * medium: 中等  
     * hard: 困难
     */
    private String difficultyLevel;

    /**
     * 知识点关键词（逗号分隔）
     */
    private String keywords;

    /**
     * 课程ID
     */
    private Integer courseId;

    /**
     * 课程名称
     */
    private String courseName;

    /**
     * 掌握程度
     * not_learned: 未学习
     * learning: 学习中
     * mastered: 已掌握
     */
    private String masteryLevel;

    /**
     * 练习得分
     */
    private BigDecimal practiceScore;

    /**
     * 练习次数
     */
    private Integer practiceCount;

    /**
     * 掌握情况最后更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime masteryLastUpdated;

    /**
     * 父级知识点名称
     */
    private String parentPointName;

    /**
     * 父级知识点ID
     */
    private Integer parentPointId;

    /**
     * 章节ID
     */
    private Integer chapterId;

    /**
     * 章节名称
     */
    private String chapterName;

    /**
     * 章节顺序
     */
    private Integer chapterOrder;

    /**
     * 章节难度等级
     * easy: 简单
     * medium: 中等
     * hard: 困难
     */
    private String chapterDifficulty;

    /**
     * 知识点在章节中的顺序
     */
    private Integer pointOrderInChapter;

    /**
     * 是否为核心知识点（1-是，0-否）
     */
    private Boolean isCorePoint;

    /**
     * 章节进度百分比
     */
    private BigDecimal chapterProgressRate;

    /**
     * 章节掌握程度
     * not_started: 未开始
     * learning: 学习中
     * completed: 已完成
     * mastered: 已掌握
     */
    private String chapterMasteryLevel;

    /**
     * 章节学习时长（分钟）
     */
    private Integer chapterStudyTime;

    /**
     * 章节最后学习时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime chapterLastStudyAt;

    /**
     * 获取知识点难度等级的中文描述
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
     * 获取章节难度等级的中文描述
     */
    public String getChapterDifficultyDesc() {
        if (chapterDifficulty == null) {
            return "";
        }
        switch (chapterDifficulty) {
            case "easy":
                return "简单";
            case "medium":
                return "中等";
            case "hard":
                return "困难";
            default:
                return chapterDifficulty;
        }
    }

    /**
     * 获取掌握程度的中文描述
     */
    public String getMasteryLevelDesc() {
        if (masteryLevel == null) {
            return "";
        }
        switch (masteryLevel) {
            case "not_learned":
                return "未学习";
            case "learning":
                return "学习中";
            case "mastered":
                return "已掌握";
            default:
                return masteryLevel;
        }
    }

    /**
     * 获取章节掌握程度的中文描述
     */
    public String getChapterMasteryLevelDesc() {
        if (chapterMasteryLevel == null) {
            return "";
        }
        switch (chapterMasteryLevel) {
            case "not_started":
                return "未开始";
            case "learning":
                return "学习中";
            case "completed":
                return "已完成";
            case "mastered":
                return "已掌握";
            default:
                return chapterMasteryLevel;
        }
    }

    /**
     * 获取核心知识点标识的中文描述
     */
    public String getCorePointDesc() {
        if (isCorePoint == null) {
            return "否";
        }
        return isCorePoint ? "是" : "否";
    }

    /**
     * 判断是否已掌握
     */
    public boolean isMastered() {
        return "mastered".equals(masteryLevel);
    }

    /**
     * 判断是否正在学习
     */
    public boolean isLearning() {
        return "learning".equals(masteryLevel);
    }

    /**
     * 判断是否未开始学习
     */
    public boolean isNotLearned() {
        return "not_learned".equals(masteryLevel) || masteryLevel == null;
    }

    /**
     * 获取学习建议
     */
    public String getLearningAdvice() {
        if (isNotLearned()) {
            return isCorePoint != null && isCorePoint ? "建议优先学习（核心知识点）" : "建议开始学习";
        } else if (isLearning()) {
            if (practiceScore != null) {
                if (practiceScore.compareTo(new BigDecimal("60")) < 0) {
                    return "需要加强练习";
                } else if (practiceScore.compareTo(new BigDecimal("80")) < 0) {
                    return "继续巩固提升";
                } else {
                    return "接近掌握，继续保持";
                }
            } else {
                return "继续学习";
            }
        } else if (isMastered()) {
            if (practiceScore != null && practiceScore.compareTo(new BigDecimal("85")) < 0) {
                return "建议复习巩固";
            } else {
                return "掌握良好";
            }
        }
        return "继续学习";
    }

    /**
     * 获取学习优先级（数字越小优先级越高）
     */
    public int getLearningPriority() {
        if (isCorePoint != null && isCorePoint && isNotLearned()) {
            return 1; // 核心知识点且未学习
        } else if (isCorePoint != null && isCorePoint && isLearning() && 
                   practiceScore != null && practiceScore.compareTo(new BigDecimal("60")) < 0) {
            return 2; // 核心知识点且学习中但分数较低
        } else if (isNotLearned()) {
            return 3; // 普通知识点未学习
        } else if (isLearning() && practiceScore != null && practiceScore.compareTo(new BigDecimal("60")) < 0) {
            return 4; // 学习中但分数较低
        } else if (isLearning() && practiceScore != null && practiceScore.compareTo(new BigDecimal("80")) < 0) {
            return 5; // 学习中分数中等
        } else {
            return 6; // 其他情况
        }
    }

    /**
     * 获取章节学习时长描述
     */
    public String getStudyTimeDesc() {
        if (chapterStudyTime == null || chapterStudyTime == 0) {
            return "暂无学习记录";
        }
        int hours = chapterStudyTime / 60;
        int minutes = chapterStudyTime % 60;
        if (hours > 0) {
            return hours + "小时" + (minutes > 0 ? minutes + "分钟" : "");
        } else {
            return minutes + "分钟";
        }
    }

    @Override
    public String toString() {
        return "学生知识点信息{" +
                "知识点ID=" + pointId +
                ", 知识点名称='" + pointName + '\'' +
                ", 知识点描述='" + description + '\'' +
                ", 知识点难度='" + getDifficultyLevelDesc() + '\'' +
                ", 关键词='" + keywords + '\'' +
                ", 课程ID=" + courseId +
                ", 课程名称='" + courseName + '\'' +
                ", 掌握程度='" + getMasteryLevelDesc() + '\'' +
                ", 练习得分=" + practiceScore +
                ", 练习次数=" + practiceCount +
                ", 掌握更新时间=" + masteryLastUpdated +
                ", 父级知识点='" + parentPointName + '\'' +
                ", 父级知识点ID=" + parentPointId +
                ", 章节ID=" + chapterId +
                ", 章节名称='" + chapterName + '\'' +
                ", 章节顺序=" + chapterOrder +
                ", 章节难度='" + getChapterDifficultyDesc() + '\'' +
                ", 章节内顺序=" + pointOrderInChapter +
                ", 是否核心知识点='" + getCorePointDesc() + '\'' +
                ", 章节进度=" + chapterProgressRate + "%" +
                ", 章节掌握程度='" + getChapterMasteryLevelDesc() + '\'' +
                ", 章节学习时长='" + getStudyTimeDesc() + '\'' +
                ", 章节最后学习时间=" + chapterLastStudyAt +
                ", 学习建议='" + getLearningAdvice() + '\'' +
                ", 学习优先级=" + getLearningPriority() +
                '}';
    }
}

