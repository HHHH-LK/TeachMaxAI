package com.aiproject.smartcampus.pojo.dto;

import lombok.Data;

@Data
public class AdminUserDTO {
    private Integer userId; // 用户ID
    private String realName; // 真实姓名
    private String userType; // 用户类型
    private String email; // 邮箱
}
