package com.aiproject.smartcampus.pojo.dto;

import lombok.Data;

@Data
public class PasswordResetVerificationDTO {
    private String credential; //验证凭证，邮箱/手机号
    private String value; //邮箱/手机号
    private String verificationCode; //验证码
}
