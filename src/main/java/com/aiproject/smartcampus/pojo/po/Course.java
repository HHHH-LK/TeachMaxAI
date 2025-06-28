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
 */ // 4. 课程信息表
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("courses")
public class Course implements Serializable {
    
    @TableId(value = "course_id", type = IdType.AUTO)
    private Integer courseId;
    
    @TableField("course_name")
    private String courseName;
    
    @TableField("teacher_id")
    private Integer teacherId;
    
    @TableField("semester")
    private String semester;
    
    @TableField("status")
    private CourseStatus status;
    
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    
    // 关联教师信息
    @TableField(exist = false)
    private Teacher teacher;
    
    public enum CourseStatus {
        ACTIVE("active"), INACTIVE("inactive");
        
        private final String value;
        CourseStatus(String value) { this.value = value; }
        public String getValue() { return value; }
    }
}