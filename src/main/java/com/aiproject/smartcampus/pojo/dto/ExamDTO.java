package com.aiproject.smartcampus.pojo.dto;
import com.aiproject.smartcampus.pojo.po.Exam;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

//考核基本信息
@Data
public class ExamDTO {
    //考核id
    private Integer exam_id;
    private Integer course_id;
    //考核名称
    private String title;
    private String exam_date;
    private Integer duration_minutes;
    private BigDecimal maxScore;
    private String status;
    private LocalDateTime createTime;
}
