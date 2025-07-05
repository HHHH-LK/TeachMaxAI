package com.aiproject.smartcampus.pojo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PasswordResetDTO {

    private String credential; // 邮箱或手机号凭证
    private String username;
    private String value;
    private String verifyCode;

    @NotBlank(message = "新密码不能为空")
    @Size(min = 8, message = "密码至少8位")
    private String newPassword;

    @NotBlank(message = "新密码不能为空")
    @Size(min = 8, message = "密码至少8位")
    private String reNewPassword;
}
