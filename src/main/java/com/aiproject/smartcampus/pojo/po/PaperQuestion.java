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
 */ // 13. 试卷题目关联表
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("paper_questions")
public class PaperQuestion implements Serializable {
    
    @TableId(value = "paper_question_id", type = IdType.AUTO)
    private Integer paperQuestionId;
    
    @TableField("paper_id")
    private Integer paperId;
    
    @TableField("question_id")
    private Integer questionId;
    
    @TableField("question_order")
    private Integer questionOrder;
    
    @TableField("custom_score")
    private BigDecimal customScore;
    
    // 关联信息
    @TableField(exist = false)
    private ExamPaper examPaper;
    
    @TableField(exist = false)
    private QuestionBank question;
}