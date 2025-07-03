package com.aiproject.smartcampus.pojo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterDTO {
    private String username; // 用户名

    @NotBlank(message = "密码不能为空")
    @Size(min = 8, message = "密码至少8位")
    private String password; // 密码
    private String repassword; // 确认密码
    private String userType; // 用户类型（学生、教师、管理员等）
}
