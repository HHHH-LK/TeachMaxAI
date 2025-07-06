package com.aiproject.smartcampus.pojo.dto;

import lombok.Data;

@Data
public class CompleteStudentDTO {
    private String realName; // 真实姓名
    private String studentNumber; // 学号
    private String email; // 邮箱
    private String phone; // 手机号
    private String grade; // 年级
    private String className; // 班级
}
