package com.aiproject.smartcampus.pojo.dto;

import lombok.Data;

/*
    教师信息更新
 */
@Data
public class TeacherUpdateDTO {
    private Long teacherId; //教师ID
    private Long userId;
    private String username; //教师名称
    private String real_name; //教师真实姓名
    private String employee_number; //教师工号
    private String department; //教师所在院系
    private String status; //教师状态（在职、离职等）
    private String email; //教师邮箱
    private String phone; //教师电话
}
