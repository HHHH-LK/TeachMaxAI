package com.aiproject.smartcampus.pojo.po;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// 4. 学生章节学习记录表实体类
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("student_chapter_progress")
public class StudentChapterProgress {
    
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;
    
    @TableField("student_id")
    private Integer studentId;
    
    @TableField("chapter_id")
    private Integer chapterId;
    
    @TableField("current_material_id")
    private Integer currentMaterialId;
    
    @TableField("progress_rate")
    private Double progressRate;
    
    @TableField("mastery_level")
    private MasteryLevel masteryLevel;
    
    @TableField("study_time")
    private Integer studyTime;
    
    @TableField("last_study_at")
    private LocalDateTime lastStudyAt;
    
    @TableField("completed_materials")
    private String completedMaterials;
    
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    
    public enum MasteryLevel {
        NOT_STARTED("not_started"),
        LEARNING("learning"),
        COMPLETED("completed"),
        MASTERED("mastered");

        @EnumValue  // 告诉MyBatis-Plus使用这个字段的值与数据库交互
        @JsonValue  // JSON序列化时返回这个值
        private final String value;
        
        MasteryLevel(String value) {
            this.value = value;
        }
        
        public String getValue() {
            return value;
        }
        
        public static MasteryLevel fromValue(String value) {
            for (MasteryLevel level : values()) {
                if (level.value.equals(value)) {
                    return level;
                }
            }
            return NOT_STARTED; // 默认值
        }
    }
}