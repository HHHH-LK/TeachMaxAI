package com.aiproject.smartcampus.pojo.dto;

import lombok.Data;

/**
 * @program: SmartCampus
 * @description: 用户登录接受层
 * @author: lk
 * @create: 2025-05-23 17:48
 **/

@Data
public class UserLoginDTO {

    //选取登陆方式
    private String type;
    //Account
    private String userAccount;
    //phone
    private String userPhone;
    //password
    private String password;
    //角色
    private String character;

}
