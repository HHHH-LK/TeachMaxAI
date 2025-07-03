package com.aiproject.smartcampus.pojo.po;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// 5. 课程资料表实体类
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("course_materials")
public class CourseMaterial {
    
    @TableId(value = "material_id", type = IdType.AUTO)
    private Integer materialId;
    
    @TableField("course_id")
    private Integer courseId;
    
    @TableField("chapter_id")
    private Integer chapterId;
    
    @TableField("courseware_resource_id")
    private Integer coursewareResourceId;
    
    @TableField("external_resource_url")
    private String externalResourceUrl;
    
    @TableField("material_title")
    private String materialTitle;
    
    @TableField("material_description")
    private String materialDescription;
    
    @TableField("material_type")
    private MaterialType materialType;
    
    @TableField("material_source")
    private MaterialSource materialSource;
    
    @TableField("estimated_time")
    private Integer estimatedTime;
    
    @TableField("difficulty_level")
    private DifficultyLevel difficultyLevel;
    
    @TableField("is_downloadable")
    private Boolean isDownloadable;
    
    @TableField("status")
    private MaterialStatus status;
    
    @TableField("created_by")
    private Integer createdBy;
    
    @TableField("tags")
    private String tags;
    
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    
    public enum MaterialType {
        COURSEWARE("courseware"),
        VIDEO("video"),
        DOCUMENT("document"),
        LINK("link"),
        EXERCISE("exercise"),
        REFERENCE("reference"),
        SUPPLEMENT("supplement");

        @EnumValue  // 告诉MyBatis-Plus使用这个字段的值与数据库交互
        @JsonValue  // JSON序列化时返回这个值
        private final String value;
        
        MaterialType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
        
        public static MaterialType fromValue(String value) {
            for (MaterialType type : values()) {
                if (type.value.equals(value)) {
                    return type;
                }
            }
            return COURSEWARE; // 默认值
        }
    }
    
    public enum MaterialSource {
        TEACHER_UPLOAD("teacher_upload"),
        EXTERNAL_LINK("external_link"),
        SYSTEM_RESOURCE("system_resource"),
        THIRD_PARTY("third_party");

        @EnumValue  // 告诉MyBatis-Plus使用这个字段的值与数据库交互
        @JsonValue  // JSON序列化时返回这个值
        private final String value;
        
        MaterialSource(String value) {
            this.value = value;
        }
        
        public String getValue() {
            return value;
        }
        
        public static MaterialSource fromValue(String value) {
            for (MaterialSource source : values()) {
                if (source.value.equals(source)) {
                    return source;
                }
            }
            return TEACHER_UPLOAD; // 默认值
        }
    }
    
    public enum DifficultyLevel {
        EASY("easy"),
        MEDIUM("medium"),
        HARD("hard");
        
        private final String value;
        
        DifficultyLevel(String value) {
            this.value = value;
        }
        
        public String getValue() {
            return value;
        }
        
        public static DifficultyLevel fromValue(String value) {
            for (DifficultyLevel level : values()) {
                if (level.value.equals(value)) {
                    return level;
                }
            }
            return MEDIUM; // 默认值
        }
    }
    
    public enum MaterialStatus {
        ACTIVE("active"),
        INACTIVE("inactive"),
        DRAFT("draft");
        
        private final String value;
        
        MaterialStatus(String value) {
            this.value = value;
        }
        
        public String getValue() {
            return value;
        }
        
        public static MaterialStatus fromValue(String value) {
            for (MaterialStatus status : values()) {
                if (status.value.equals(value)) {
                    return status;
                }
            }
            return ACTIVE; // 默认值
        }
    }
}