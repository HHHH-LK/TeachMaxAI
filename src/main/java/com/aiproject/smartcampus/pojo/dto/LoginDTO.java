package com.aiproject.smartcampus.pojo.dto;

import lombok.Data;

@Data
public class LoginDTO {
    private String principal; // 登录凭证，可以是用户名手机号邮箱
    private String phone; // 手机号
    private String email; // 邮箱
    private String username; // 用户名
    private String password; // 密码
    private String userType; // 用户类型，学生、教师、管理员等
}
