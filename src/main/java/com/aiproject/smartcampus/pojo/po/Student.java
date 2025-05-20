package com.aiproject.smartcampus.pojo.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.io.Serializable;

/**
 * 学生实体类
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("student")
public class Student implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 学生ID
     */
    @TableId(value = "student_id", type = IdType.AUTO)
    private Integer studentId;

    /**
     * 学生账号
     */
    @TableField("student_account")
    private String studentAccount;

    /**
     * 班级号
     */
    @TableField("class_id")
    private String classId;

    /**
     * 学生姓名
     */
    @TableField("student_name")
    private String studentName;

    /**
     * 学生密码
     */
    @TableField("password")
    private String password;

    /**
     * 学生年龄
     */
    @TableField("age")
    private Integer age;

    /**
     * 学生头像路径
     */
    @TableField("avatar_path")
    private String avatarPath;

    /**
     * 学生电话
     */
    @TableField("phone")
    private String phone;

    /**
     * 学生家庭住址
     */
    @TableField("address")
    private String address;

    /**
     * 辅导员ID
     */
    @TableField("counselor_id")
    private Integer counselorId;

    /**
     * 所属院系
     */
    @TableField("department")
    private String department;
}
