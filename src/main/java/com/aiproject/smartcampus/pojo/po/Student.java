package com.aiproject.smartcampus.pojo.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

// 2. 学生信息表
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("students")
public class Student implements Serializable {
    
    @TableId(value = "student_id", type = IdType.AUTO)
    private Integer studentId;
    
    @TableField("user_id")
    private Integer userId;
    
    @TableField("student_number")
    private String studentNumber;
    
    @TableField("grade")
    private String grade;
    
    @TableField("class_name")
    private String className;
    
    // 关联用户信息
    @TableField(exist = false)
    private User user;
}