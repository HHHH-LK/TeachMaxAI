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
 */ // 8. 考试表
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("exams")
public class Exam implements Serializable {
    
    @TableId(value = "exam_id", type = IdType.AUTO)
    private Integer examId;
    
    @TableField("course_id")
    private Integer courseId;
    
    @TableField("title")
    private String title;
    
    @TableField("exam_date")
    private LocalDateTime examDate;
    
    @TableField("duration_minutes")
    private Integer durationMinutes;
    
    @TableField("max_score")
    private BigDecimal maxScore;
    
    @TableField("status")
    private ExamStatus status;
    
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    
    // 关联课程信息
    @TableField(exist = false)
    private Course course;
    
    public enum ExamStatus {
        SCHEDULED("scheduled"), COMPLETED("completed");
        
        private final String value;
        ExamStatus(String value) { this.value = value; }
        public String getValue() { return value; }
    }
}