package com.aiproject.smartcampus.pojo.dto;

import lombok.Data;

@Data
public class TeacherGetStudentDTO {
    private Integer courseId; // 课程ID
    private String courseName; // 课程名称
    private String realName; // 学生姓名
    private String studentNumber; // 学生学号
    private String Email; // 学生邮箱
    private String Phone; // 学生联系电话
    private String class_name; // 班级名称
    private Double score; // 学生成绩
}
