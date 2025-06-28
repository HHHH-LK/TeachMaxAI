package com.aiproject.smartcampus.pojo.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;


// 7. 作业提交表
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("assignment_submissions")
public class AssignmentSubmission implements Serializable {
    
    @TableId(value = "submission_id", type = IdType.AUTO)
    private Integer submissionId;
    
    @TableField("assignment_id")
    private Integer assignmentId;
    
    @TableField("student_id")
    private Integer studentId;
    
    @TableField("content")
    private String content;
    
    @TableField(value = "submitted_at", fill = FieldFill.INSERT)
    private LocalDateTime submittedAt;
    
    @TableField("score")
    private BigDecimal score;
    
    @TableField("feedback")
    private String feedback;
    
    // 关联信息
    @TableField(exist = false)
    private Assignment assignment;
    
    @TableField(exist = false)
    private Student student;
}