package com.aiproject.smartcampus.pojo.vo;

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
public class ChapterTeacherVO {
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
                ", 总知识点数=" + totalKnowledgePoints +
                ", 总资料数=" + totalMaterials +
                ", 核心知识点数=" + coreKnowledgePoints +
                '}';
    }
}
