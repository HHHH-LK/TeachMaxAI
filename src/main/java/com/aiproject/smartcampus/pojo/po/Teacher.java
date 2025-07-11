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


    /**
     * 重写toString方法，使用中文描述
     * 重点展示员工号和部门信息
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("教师信息{");
        sb.append("教师ID=").append(teacherId);
        sb.append(", 用户ID=").append(userId);
        sb.append(", 员工号='").append(employeeNumber).append('\'');
        sb.append(", 所属部门='").append(department).append('\'');

        // 如果关联了用户信息，也显示用户的基本信息
        if (user != null) {
            sb.append(", 用户信息={");
            sb.append("姓名='").append(user.getRealName()).append('\'');
            sb.append(", 用户名='").append(user.getUsername()).append('\'');
            sb.append(", 邮箱='").append(user.getEmail()).append('\'');
            sb.append(", 用户类型='").append(user.getUserType()).append('\'');
            sb.append("}");
        }

        sb.append('}');
        return sb.toString();
    }

}