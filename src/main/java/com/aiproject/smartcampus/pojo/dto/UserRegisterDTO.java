package com.aiproject.smartcampus.pojo.dto;

import com.aiproject.smartcampus.pojo.po.Admin;

import com.aiproject.smartcampus.pojo.po.Student;
import com.aiproject.smartcampus.pojo.po.Teacher;
import lombok.Data;

/**
 * @program: SmartCampus
 * @description: 用户注册个人信息填写
 * @author: lk
 * @create: 2025-05-20 13:45
 **/

@Data
public class UserRegisterDTO {

    private String character;
    private Teacher teacher;
    private Student student;
    private Admin admin;

}