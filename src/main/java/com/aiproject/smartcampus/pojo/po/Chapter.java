package com.aiproject.smartcampus.pojo.po;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;

// 1. 章节表实体类
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("chapters")
public class Chapter {
    
    @TableId(value = "chapter_id", type = IdType.AUTO)
    private Integer chapterId;
    
    @TableField("course_id")
    private Integer courseId;
    
    @TableField("chapter_name")
    private String chapterName;
    
    @TableField("chapter_order")
    private Integer chapterOrder;
    
    @TableField("description")
    private String description;
    
    @TableField("difficulty_level")
    private DifficultyLevel difficultyLevel;
    
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    
    public enum DifficultyLevel {
        EASY("easy"),
        MEDIUM("medium"),
        HARD("hard");

        @EnumValue  // 告诉MyBatis-Plus使用这个字段的值与数据库交互
        @JsonValue  // JSON序列化时返回这个值
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
}