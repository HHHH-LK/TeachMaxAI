package com.aiproject.smartcampus.pojo.vo;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.math.BigDecimal;

/**
 * 课程资源简化信息VO
 * 对应简化查询的结果字段
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MaterialSimpleVO {
    
    /**
     * 资料ID
     */
    private Integer materialId;
    
    /**
     * 资料标题
     */
    private String materialTitle;
    
    /**
     * 资料描述
     */
    private String materialDescription;
    
    /**
     * 资料类型：courseware-课件, video-视频, document-文档, 
     * link-链接, exercise-练习, reference-参考资料, supplement-补充资料
     */
    private String materialType;
    
    /**
     * 外部资源链接
     */
    private String externalResourceUrl;
    
    /**
     * 预计学习时间（分钟）
     */
    private Integer estimatedTime;
    
    /**
     * 难度等级：easy-简单, medium-中等, hard-困难
     */
    private String difficultyLevel;
    
    /**
     * 是否允许下载：1-允许, 0-不允许
     */
    private Integer isDownloadable;
    
    /**
     * 文件名
     */
    private String fileName;
    
    /**
     * 文件URL
     */
    private String fileUrl;
    
    /**
     * 文件大小(MB)
     */
    private BigDecimal fileSizeMb;
    
    /**
     * 获取资料类型中文描述
     * @return 资料类型中文描述
     */
    public String getMaterialTypeCn() {
        if (materialType == null) {
            return "";
        }
        switch (materialType) {
            case "courseware":
                return "课件";
            case "video":
                return "视频";
            case "document":
                return "文档";
            case "link":
                return "链接";
            case "exercise":
                return "练习";
            case "reference":
                return "参考资料";
            case "supplement":
                return "补充资料";
            default:
                return materialType;
        }
    }
    
    /**
     * 获取难度等级中文描述
     * @return 难度等级中文描述
     */
    public String getDifficultyLevelCn() {
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
     * 判断是否允许下载
     * @return true-允许下载, false-不允许下载
     */
    public boolean isDownloadAllowed() {
        return isDownloadable != null && isDownloadable == 1;
    }
    
    /**
     * 获取文件大小描述
     * @return 文件大小描述，如："15.6 MB"
     */
    public String getFileSizeDesc() {
        if (fileSizeMb == null) {
            return "未知大小";
        }
        return fileSizeMb.toString() + " MB";
    }
    
    /**
     * 获取学习时间描述
     * @return 学习时间描述，如："30分钟" 或 "1小时30分钟"
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
            if (minutes == 0) {
                return hours + "小时";
            } else {
                return hours + "小时" + minutes + "分钟";
            }
        }
    }
    
    /**
     * 判断是否有有效的文件资源
     * @return true-有文件资源, false-无文件资源
     */
    public boolean hasFileResource() {
        return (fileName != null && !fileName.trim().isEmpty()) 
            || (fileUrl != null && !fileUrl.trim().isEmpty());
    }
    
    /**
     * 判断是否有外部资源链接
     * @return true-有外部链接, false-无外部链接
     */
    public boolean hasExternalResource() {
        return externalResourceUrl != null && !externalResourceUrl.trim().isEmpty();
    }
    
    /**
     * 获取资源访问URL（优先返回文件URL，其次返回外部资源URL）
     * @return 资源访问URL
     */
    public String getResourceUrl() {
        if (fileUrl != null && !fileUrl.trim().isEmpty()) {
            return fileUrl;
        }
        if (externalResourceUrl != null && !externalResourceUrl.trim().isEmpty()) {
            return externalResourceUrl;
        }
        return null;
    }
    
    /**
     * 获取资源类型图标（用于前端显示）
     * @return 资源类型对应的图标类名
     */
    public String getTypeIcon() {
        if (materialType == null) {
            return "file";
        }
        switch (materialType) {
            case "courseware":
                return "presentation";
            case "video":
                return "video";
            case "document":
                return "file-text";
            case "link":
                return "link";
            case "exercise":
                return "edit";
            case "reference":
                return "book";
            case "supplement":
                return "plus-circle";
            default:
                return "file";
        }
    }
}
