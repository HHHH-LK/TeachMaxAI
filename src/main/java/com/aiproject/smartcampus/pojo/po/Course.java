package com.aiproject.smartcampus.pojo.po;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 实体类：课程信息
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Course {

    private String weekday;          // 星期几
    @JsonProperty("time_slot")
    private String timeSlot;         // 时间段
    private String period;           // 节次
    @JsonProperty("course_name")
    private String courseName;       // 课程名称
    @JsonProperty("course_type")
    private String courseType;       // 课程类型
    private String weeks;            // 上课周次
    private String campus;           // 校区
    private String location;         // 场地
    private String teacher;          // 教师
    @JsonProperty("class_id")
    private String classId;          // 教学班编号
    private List<String> students;   // 学号列表
    private String assessment;       // 考核方式
    @JsonProperty("weekly_hours")
    private int weeklyHours;         // 周学时
    private double credits;          // 学分

    @Override
    public String toString() {
        return "Course{" +
                "weekday='" + weekday + '\'' +
                ", timeSlot='" + timeSlot + '\'' +
                ", period='" + period + '\'' +
                ", courseName='" + courseName + '\'' +
                ", courseType='" + courseType + '\'' +
                ", weeks='" + weeks + '\'' +
                ", campus='" + campus + '\'' +
                ", location='" + location + '\'' +
                ", teacher='" + teacher + '\'' +
                ", classId='" + classId + '\'' +
                ", students=" + students +
                ", assessment='" + assessment + '\'' +
                ", weeklyHours=" + weeklyHours +
                ", credits=" + credits +
                '}';
    }
}
