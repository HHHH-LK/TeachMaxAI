package com.aiproject.smartcampus.pojo.dto;

import com.aiproject.smartcampus.pojo.po.User;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class LoginInfoDTO {
    private User user;
    private LocalDateTime loginTime;
}
