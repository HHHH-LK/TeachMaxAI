package com.aiproject.smartcampus.pojo.dto;

import lombok.Data;

@Data
public class emailGetDTO {
    private String email; // 用户邮箱
    private String code;
    private String resetPasswordSubject;
    private String resetPasswordHtml;
}
