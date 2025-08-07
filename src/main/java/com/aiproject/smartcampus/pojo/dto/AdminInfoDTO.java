package com.aiproject.smartcampus.pojo.dto;

import lombok.Data;

@Data
public class AdminInfoDTO {
    private String adminNumber; // 管理员工号 表admin
    private String adminName; // 管理员姓名 user
    private String adminLevel; // 管理员级别 admin
    private String adminRole; // 管理员角色 admin position
    private String adminEmail; // 管理员邮箱
    private String adminPhone; // 管理员电话
    private String adminDepartment; // 管理员部门 admin department
}
