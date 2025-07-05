package com.aiproject.smartcampus.pojo.dto;

import lombok.Data;

import java.util.Date;

@Data
public class ExamQueryDTO {
    private Long teacherId;//发布教师
    private String title;//试卷名称
    private String type;//试卷类型
    private String status; //试卷状态
    private Date startTime;//考试开始时间
    private Date endTime;//考试结束时间
}