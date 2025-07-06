package com.aiproject.smartcampus.pojo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PasswordResetDTO {

//    private String credential; // 邮箱或手机号凭证
    private String username;
    private String value;
    private String verifyCode;

    private String newPassword;
    private String reNewPassword;
}
