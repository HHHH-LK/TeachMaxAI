package com.aiproject.smartcampus.pojo.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * @program: SmartCampus
 * @description: 学生信息管理
 * @author: lk_hhh
 * @create: 2025-07-01 17:27
 **/


@Data
public class StudentsImformationVO {

    /**
     * 学号
     */
    private String studentNumber;

    /**
     * 年级
     */
    private String grade;

    /**
     * 班级
     */
    private String className;

    /**
     * 真实姓名
     */
    private String realName;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 状态
     */
    private String status;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}