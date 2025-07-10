package com.aiproject.smartcampus.pojo.dto;

import lombok.Data;

@Data
public class TeacherGetSituationDTO {
    private Integer courseId; // 课程ID
    private String courseName; // 课程名称
    private Double averageScore; // 平均分
    private Double PassRate; // 通过率
    private Double ExcellentRate; //优秀
    private Integer FailNumber; //不及格人数
    private Integer PassNumber; //60-69人数
    private Integer NormalNumber; //70-79的人数
    private Integer GoodNumber; //80-89
    private Integer ExcellentNumber; //90-100
}
