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

    @Override
    public String toString() {
        return "课程学情统计报告：\n" +
                "课程ID：" + courseId + "\n" +
                "课程名称：" + courseName + "\n" +
                "平均分：" + averageScore + "分\n" +
                "通过率：" + PassRate + "%\n" +
                "优秀率：" + ExcellentRate + "%\n" +
                "不及格人数：" + FailNumber + "人\n" +
                "及格人数（60-69分）：" + PassNumber + "人\n" +
                "中等人数（70-79分）：" + NormalNumber + "人\n" +
                "良好人数（80-89分）：" + GoodNumber + "人\n" +
                "优秀人数（90-100分）：" + ExcellentNumber + "人";
    }
}