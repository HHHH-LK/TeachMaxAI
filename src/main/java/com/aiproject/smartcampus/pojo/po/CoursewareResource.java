package com.aiproject.smartcampus.pojo.po;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * @program: SmartCampus
 * @description: 课件资源表实体类
 * @author: lk
 * @create: 2025-07-01
 **/
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("courseware_resources")
public class CoursewareResource implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 资源ID
     */
    @TableId(value = "resource_id", type = IdType.AUTO)
    private Integer resourceId;

    /**
     * 资源标题
     */
    @TableField("resource_title")
    private String resourceTitle;

    /**
     * 资源描述
     */
    @TableField("resource_description")
    private String resourceDescription;

    /**
     * 所属课程ID
     */
    @TableField("course_id")
    private Integer courseId;

    /**
     * 创建教师ID
     */
    @TableField("teacher_id")
    private Integer teacherId;

    /**
     * 资源类型
     */
    @TableField("resource_type")
    private ResourceType resourceType;

    /**
     * 文件名
     */
    @TableField("file_name")
    private String fileName;

    /**
     * 文件URL
     */
    @TableField("file_url")
    private String fileUrl;

    /**
     * 文件大小(MB)
     */
    @TableField("file_size_mb")
    private BigDecimal fileSizeMb;

    /**
     * 标签，逗号分隔
     */
    @TableField("tags")
    private String tags;

    /**
     * 下载次数
     */
    @TableField("download_count")
    private Integer downloadCount;

    /**
     * 是否公开（0-私有，1-公开）
     */
    @TableField("is_public")
    private Boolean isPublic;

    /**
     * 状态
     */
    @TableField("status")
    private ResourceStatus status;

    /**
     * 创建时间
     */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    // ==================== 关联对象（不映射到数据库）====================

    /**
     * 关联课程信息
     */
    @TableField(exist = false)
    private Course course;

    /**
     * 创建教师信息
     */
    @TableField(exist = false)
    private Teacher teacher;

    /**
     * 下载记录列表
     */
    @TableField(exist = false)
    private List<ResourceDownload> downloads;

    // ==================== 枚举定义 ====================

    /**
     * 资源类型枚举
     */
    public enum ResourceType {
        COURSEWARE("courseware", "课程资料"),
        EXERCISE("exercise", "练习题"),
        VIDEO("video", "视频资源"),
        DOCUMENT("document", "文档资源"),
        IMAGE("image", "图片资源"),
        OTHER("other", "其他类型资源");

        @EnumValue  // 告诉MyBatis-Plus使用这个字段的值与数据库交互
        @JsonValue  // JSON序列化时返回这个值
        private final String value;
        private final String description;

        ResourceType(String value, String description) {
            this.value = value;
            this.description = description;
        }

        public String getValue() {
            return value;
        }

        public String getDescription() {
            return description;
        }

        public static ResourceType fromValue(String value) {
            for (ResourceType type : ResourceType.values()) {
                if (type.getValue().equals(value)) {
                    return type;
                }
            }
            return ResourceType.OTHER; // 默认值
        }
    }

    /**
     * 资源状态枚举
     */
    public enum ResourceStatus {
        ACTIVE("active", "活跃"),
        INACTIVE("inactive", "非活跃");

        @EnumValue  // 告诉MyBatis-Plus使用这个字段的值与数据库交互
        @JsonValue  // JSON序列化时返回这个值
        private final String value;
        private final String description;

        ResourceStatus(String value, String description) {
            this.value = value;
            this.description = description;
        }

        public String getValue() {
            return value;
        }

        public String getDescription() {
            return description;
        }

        public static ResourceStatus fromValue(String value) {
            for (ResourceStatus status : ResourceStatus.values()) {
                if (status.getValue().equals(value)) {
                    return status;
                }
            }
            return ResourceStatus.ACTIVE; // 默认值
        }
    }


}