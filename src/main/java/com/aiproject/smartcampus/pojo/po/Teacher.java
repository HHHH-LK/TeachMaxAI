package com.aiproject.smartcampus.pojo.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * @author lk_hhh
 */ // 3. 教师信息表
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("teachers")
public class Teacher implements Serializable {
    
    @TableId(value = "teacher_id", type = IdType.AUTO)
    private Integer teacherId;
    
    @TableField("user_id")
    private Integer userId;
    
    @TableField("employee_number")
    private String employeeNumber;
    
    @TableField("department")
    private String department;
    
    // 关联用户信息
    @TableField(exist = false)
    private User user;
}