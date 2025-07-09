package com.aiproject.smartcampus.pojo.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 学生章节资源查询结果VO
 * 包含资源信息、课程章节信息和学生学习进度
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentChapterResourceVO {
    
    // ========== 资源基本信息 ==========
    
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
     * 资料类型中文描述
     */
    private String materialTypeCn;
    
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
     * 难度等级中文描述
     */
    private String difficultyLevelCn;
    
    /**
     * 是否允许下载：1-允许, 0-不允许
     */
    private Integer isDownloadable;
    
    /**
     * 标签，逗号分隔
     */
    private String tags;
    
    /**
     * 资料创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
    
    // ========== 课件文件信息 ==========
    
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
     * 文件类型
     */
    private String fileType;
    
    // ========== 课程和章节信息 ==========
    
    /**
     * 课程ID
     */
    private Integer courseId;
    
    /**
     * 课程名称
     */
    private String courseName;
    
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
    
    // ========== 学生学习进度信息 ==========
    
    /**
     * 是否已完成：1-已完成, 0-未完成
     */
    private Integer isCompleted;
    
    /**
     * 当前正在学习的资料ID
     */
    private Integer currentMaterialId;
    
    /**
     * 是否为当前正在学习的资料：1-是, 0-否
     */
    private Integer isCurrentLearning;
    
    // ========== 业务逻辑方法 ==========
    
    /**
     * 判断是否已完成学习
     * @return true-已完成, false-未完成
     */
    public boolean isLearningCompleted() {
        return isCompleted != null && isCompleted == 1;
    }
    
    /**
     * 判断是否为当前正在学习的资料
     * @return true-正在学习, false-不是当前学习资料
     */
    public boolean isCurrentlyLearning() {
        return isCurrentLearning != null && isCurrentLearning == 1;
    }
    
    /**
     * 判断是否允许下载
     * @return true-允许下载, false-不允许下载
     */
    public boolean isDownloadAllowed() {
        return isDownloadable != null && isDownloadable == 1;
    }
    
    /**
     * 获取学习状态
     * @return 学习状态：completed-已完成, learning-学习中, not_started-未开始
     */
    public String getLearningStatus() {
        if (isLearningCompleted()) {
            return "completed";
        } else if (isCurrentlyLearning()) {
            return "learning";
        } else {
            return "not_started";
        }
    }
    
    /**
     * 获取学习状态中文描述
     * @return 学习状态中文描述
     */
    public String getLearningStatusCn() {
        switch (getLearningStatus()) {
            case "completed":
                return "已完成";
            case "learning":
                return "学习中";
            case "not_started":
                return "未开始";
            default:
                return "未知状态";
        }
    }
    
    /**
     * 获取资料类型图标
     * @return 图标类名
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
    
    /**
     * 获取难度等级颜色
     * @return 颜色代码
     */
    public String getDifficultyColor() {
        if (difficultyLevel == null) {
            return "#666666";
        }
        switch (difficultyLevel) {
            case "easy":
                return "#52c41a";  // 绿色
            case "medium":
                return "#faad14";  // 橙色
            case "hard":
                return "#f5222d";  // 红色
            default:
                return "#666666";  // 灰色
        }
    }
    
    /**
     * 获取学习状态颜色
     * @return 状态颜色代码
     */
    public String getLearningStatusColor() {
        switch (getLearningStatus()) {
            case "completed":
                return "#52c41a";  // 绿色
            case "learning":
                return "#1890ff";  // 蓝色
            case "not_started":
                return "#d9d9d9";  // 灰色
            default:
                return "#666666";
        }
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
     * 获取标签数组
     * @return 标签数组
     */
    public String[] getTagArray() {
        if (tags == null || tags.trim().isEmpty()) {
            return new String[0];
        }
        return tags.split(",");
    }
    
    /**
     * 判断是否为外部资源
     * @return true-外部资源, false-内部资源
     */
    public boolean isExternalResource() {
        return "link".equals(materialType) || "reference".equals(materialType) || hasExternalResource();
    }
    
    /**
     * 判断是否为交互式资源（需要用户操作的资源）
     * @return true-交互式资源, false-静态资源
     */
    public boolean isInteractiveResource() {
        return "exercise".equals(materialType) || "link".equals(materialType);
    }
    
    /**
     * 获取资源优先级（用于排序）
     * @return 优先级数值，数值越小优先级越高
     */
    public int getResourcePriority() {
        if (materialType == null) {
            return 999;
        }
        switch (materialType) {
            case "courseware":
                return 1;
            case "video":
                return 2;
            case "document":
                return 3;
            case "exercise":
                return 4;
            case "reference":
                return 5;
            case "supplement":
                return 6;
            case "link":
                return 7;
            default:
                return 8;
        }
    }
    
    /**
     * 判断是否为有效的资源对象
     * @return true-有效, false-无效
     */
    public boolean isValidResource() {
        return materialId != null && 
               materialTitle != null && 
               !materialTitle.trim().isEmpty() &&
               materialType != null && 
               !materialType.trim().isEmpty() &&
               courseId != null &&
               chapterId != null;
    }
    
    /**
     * 重写toString方法，便于调试
     */
    @Override
    public String toString() {
        return String.format("StudentChapterResourceVO{materialId=%d, title='%s', type='%s', status='%s', course='%s', chapter='%s'}", 
                            materialId, materialTitle, materialTypeCn, getLearningStatusCn(), courseName, chapterName);
    }
}