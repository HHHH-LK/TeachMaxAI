package com.aiproject.smartcampus.pojo.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

// 10. 考试成绩表
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("exam_scores")
public class ExamScore implements Serializable {
    
    @TableId(value = "score_id", type = IdType.AUTO)
    private Integer scoreId;
    
    @TableField("exam_id")
    private Integer examId;
    
    @TableField("student_id")
    private Integer studentId;
    
    @TableField("score")
    private BigDecimal score;
    
    @TableField("submitted_at")
    private LocalDateTime submittedAt;
    
    // 关联信息
    @TableField(exist = false)
    private Exam exam;
    
    @TableField(exist = false)
    private Student student;
}