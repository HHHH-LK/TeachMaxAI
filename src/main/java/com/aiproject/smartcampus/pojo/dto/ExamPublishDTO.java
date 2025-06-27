package com.aiproject.smartcampus.pojo.dto;

import lombok.Data;

import java.util.Date;
import java.util.List;

//考试发布
@Data
public class ExamPublishDTO {
    private long examId;        // 考试ID
    private Date startTime;     // 考核开始时间
    private Date endTime;       // 考核结束时间
    private List<Long> disciplineIds; // 发布到的ID列表
    private Boolean isTimed;     // 是否计时考核
    private Integer timeLimit;   // 时间限制（分钟）
    private Boolean isPublic;    // 是否公开试卷
    private Boolean showAnswerAfterSubmit; // 提交后显示答案
}