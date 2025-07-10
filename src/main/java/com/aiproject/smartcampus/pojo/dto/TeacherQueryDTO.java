package com.aiproject.smartcampus.pojo.dto;

import lombok.Data;

import java.time.LocalDateTime;

/*
    教师信息查询
 */
@Data
public class TeacherQueryDTO {
    private Long userId; // 用户ID
    private Long teacherId; // 教师ID
    private String employee_number; // 教师工号
    private String username;
    private String real_name;
    private String department; // 所属院系
    private String email; // 教师邮箱
    private String phone; // 教师联系电话
//    private String profilePicure; // 教师头像URL
}
