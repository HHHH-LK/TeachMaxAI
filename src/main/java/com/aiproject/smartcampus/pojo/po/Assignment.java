package com.aiproject.smartcampus.pojo.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * @author lk_hhh
 */ // 6. 作业表
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("assignments")
public class Assignment implements Serializable {
    
    @TableId(value = "assignment_id", type = IdType.AUTO)
    private Integer assignmentId;
    
    @TableField("course_id")
    private Integer courseId;
    
    @TableField("title")
    private String title;
    
    @TableField("description")
    private String description;
    
    @TableField("due_date")
    private LocalDateTime dueDate;
    
    @TableField("max_score")
    private BigDecimal maxScore;
    
    @TableField("status")
    private AssignmentStatus status;
    
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    
    // 关联课程信息
    @TableField(exist = false)
    private Course course;
    
    public enum AssignmentStatus {
        DRAFT("draft"), PUBLISHED("published"), CLOSED("closed");
        
        private final String value;
        AssignmentStatus(String value) { this.value = value; }
        public String getValue() { return value; }
    }
}