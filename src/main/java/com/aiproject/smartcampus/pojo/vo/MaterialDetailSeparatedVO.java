package com.aiproject.smartcampus.pojo.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 资料详细信息VO（分离课件和外部资源）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaterialDetailSeparatedVO {
    
    // 基本信息
    private Integer materialId;
    private String materialTitle;
    private String materialDescription;
    private String materialType;
    private Integer estimatedTime;
    private String difficultyLevel;
    private Integer isDownloadable;
    private String tags;
    private String materialSource;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
    
    // 课程信息
    private Integer courseId;
    private String courseName;
    private String semester;
    
    // 章节信息
    private Integer chapterId;
    private String chapterName;
    private Integer chapterOrder;
    
    // 创建者信息
    private String creatorName;
    
    // 课件资源（如果有）
    private CoursewareResourceVO coursewareResource;
    
    // 外部资源（如果有）
    private ExternalResourceVO externalResource;
    
    /**
     * 获取资料类型中文描述
     */
    public String getMaterialTypeCn() {
        if (materialType == null) {
            return "";
        }
        switch (materialType) {
            case "courseware": return "课件";
            case "video": return "视频";
            case "document": return "文档";
            case "link": return "链接";
            case "exercise": return "练习";
            case "reference": return "参考资料";
            case "supplement": return "补充资料";
            default: return materialType;
        }
    }
    
    /**
     * 获取难度等级中文描述
     */
    public String getDifficultyLevelCn() {
        if (difficultyLevel == null) {
            return "";
        }
        switch (difficultyLevel) {
            case "easy": return "简单";
            case "medium": return "中等";
            case "hard": return "困难";
            default: return difficultyLevel;
        }
    }
    
    /**
     * 获取学习时间描述
     */
    public String getEstimatedTimeDesc() {
        if (estimatedTime == null || estimatedTime <= 0) {
            return "时间未知";
        }
        if (estimatedTime < 60) {
            return estimatedTime + "分钟";
        } else {
            int hours = estimatedTime / 60;
            int minutes = estimatedTime % 60;
            return minutes == 0 ? hours + "小时" : hours + "小时" + minutes + "分钟";
        }
    }
    
    /**
     * 判断是否有课件资源
     */
    public boolean hasCoursewareResource() {
        return coursewareResource != null;
    }
    
    /**
     * 判断是否有外部资源
     */
    public boolean hasExternalResource() {
        return externalResource != null;
    }
    
    /**
     * 获取标签数组
     */
    public String[] getTagArray() {
        if (tags == null || tags.trim().isEmpty()) {
            return new String[0];
        }
        return tags.split(",");
    }
}

