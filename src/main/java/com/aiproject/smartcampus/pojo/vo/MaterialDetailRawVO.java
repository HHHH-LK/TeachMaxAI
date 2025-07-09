package com.aiproject.smartcampus.pojo.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 原始查询结果VO（内部使用）
 */
@Data
public class MaterialDetailRawVO {
    private Integer materialId;
    private String materialTitle;
    private String materialDescription;
    private String materialType;
    private String externalResourceUrl;
    private Integer estimatedTime;
    private String difficultyLevel;
    private Integer isDownloadable;
    private String tags;
    private String materialSource;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer courseId;
    private String courseName;
    private String semester;
    private Integer chapterId;
    private String chapterName;
    private Integer chapterOrder;
    private Integer coursewareResourceId;
    private String fileName;
    private String fileUrl;
    private BigDecimal fileSizeMb;
    private String fileType;
    private Integer downloadCount;
    private Integer isPublic;
    private String creatorName;
}