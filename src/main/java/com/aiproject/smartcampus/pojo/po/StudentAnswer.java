package com.aiproject.smartcampus.pojo.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

// 14. 学生答题记录表
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("student_answers")
public class StudentAnswer implements Serializable {
    
    @TableId(value = "answer_id", type = IdType.AUTO)
    private Integer answerId;
    
    @TableField("exam_id")
    private Integer examId;
    
    @TableField("student_id")
    private Integer studentId;
    
    @TableField("question_id")
    private Integer questionId;
    
    @TableField("student_answer")
    private String studentAnswer;
    
    @TableField("is_correct")
    private Boolean isCorrect;
    
    @TableField("score_earned")
    private BigDecimal scoreEarned;
    
    // 关联信息
    @TableField(exist = false)
    private Exam exam;
    
    @TableField(exist = false)
    private Student student;
    
    @TableField(exist = false)
    private QuestionBank question;
}