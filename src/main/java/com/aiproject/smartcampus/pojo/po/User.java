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
 */ // 1. 用户基础信息表
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("users")
public class User implements Serializable {
    
    @TableId(value = "user_id", type = IdType.AUTO)
    private Integer userId;
    
    @TableField("username")
    private String username;
    
    @TableField("password_hash")
    private String passwordHash;
    
    @TableField("real_name")
    private String realName;
    
    @TableField("email")
    private String email;
    
    @TableField("phone")
    private String phone;
    
    @TableField("user_type")
    private UserType userType;
    
    @TableField("status")
    private UserStatus status;
    
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    
    public enum UserType {
        STUDENT("student"), TEACHER("teacher"), ADMIN("admin");
        
        private final String value;
        UserType(String value) { this.value = value; }
        public String getValue() { return value; }
    }
    
    public enum UserStatus {
        ACTIVE("active"), INACTIVE("inactive");
        
        private final String value;
        UserStatus(String value) { this.value = value; }
        public String getValue() { return value; }
    }
}