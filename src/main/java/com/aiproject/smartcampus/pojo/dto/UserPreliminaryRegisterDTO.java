package com.aiproject.smartcampus.pojo.dto;

import lombok.Data;

/**
 * @program: SmartCampus
 * @description: 用户接收层
 * @author: lk
 * @create: 2025-05-19 15:28
 **/

@Data
public class UserPreliminaryRegisterDTO {
    //用来判断是否为学生（student/teacher/controller）
    private String Character;
    private String password;
    private String rePassword;
    private String phone;
    private String userAccount;



}
