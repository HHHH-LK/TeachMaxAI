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
 */ // 9. 试卷表
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("exam_papers")
public class ExamPaper implements Serializable {
    
    @TableId(value = "paper_id", type = IdType.AUTO)
    private Integer paperId;
    
    @TableField("exam_id")
    private Integer examId;
    
    @TableField("paper_title")
    private String paperTitle;
    
    @TableField("total_score")
    private BigDecimal totalScore;
    
    @TableField("question_count")
    private Integer questionCount;
    
    // 关联考试信息
    @TableField(exist = false)
    private Exam exam;
}


