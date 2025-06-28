package com.aiproject.smartcampus.pojo.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

// 5. 学生选课表
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("course_enrollments")
public class CourseEnrollment implements Serializable {
    
    @TableId(value = "enrollment_id", type = IdType.AUTO)
    private Integer enrollmentId;
    
    @TableField("student_id")
    private Integer studentId;
    
    @TableField("course_id")
    private Integer courseId;
    
    @TableField(value = "enrollment_date", fill = FieldFill.INSERT)
    private LocalDateTime enrollmentDate;
    
    @TableField("final_grade")
    private BigDecimal finalGrade;
    
    // 关联信息
    @TableField(exist = false)
    private Student student;
    
    @TableField(exist = false)
    private Course course;
}