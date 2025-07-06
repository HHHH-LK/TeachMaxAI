package com.aiproject.smartcampus.pojo.dto;

import lombok.Data;

@Data
public class CompleteTeacherDTO {
    private String realName; // 真实姓名
    private String employeeNumber; // 教师编号
    private String email; // 邮箱
    private String phone; // 手机号
    private String department; // 所属专业
}
