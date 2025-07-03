package com.aiproject.smartcampus.pojo.dto;

import lombok.Data;

import java.time.LocalDateTime;

//考试发布状态
@Data
public class  ExamCreateResponseDTO {
    private Integer examId;
    private String title;
    private String status;
    private LocalDateTime createTime;
}