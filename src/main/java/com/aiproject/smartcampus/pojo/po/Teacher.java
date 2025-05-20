package com.aiproject.smartcampus.pojo.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;

/**
 * 教师表实体类
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("teacher")
public class Teacher implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 教师ID，自增主键 */
    @TableId(value = "teacher_id", type = IdType.AUTO)
    private Integer teacherId;

    /** 教师姓名 */
    @TableField("teacher_name")
    private String teacherName;

    /** 教师账号，唯一 */
    @TableField("account")
    private String account;

    /** 教师密码 */
    @TableField("password")
    private String password;

    /** 教师年龄 */
    @TableField("age")
    private Short age;

    /** 教师电话 */
    @TableField("phone")
    private String phone;

    /** 教师头像路径 */
    @TableField("avatar_path")
    private String avatarPath;

    /** 教师地址 */
    @TableField("address")
    private String address;

    /** 所属院系 */
    @TableField("department")
    private String department;
}
