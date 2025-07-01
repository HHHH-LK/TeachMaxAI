package com.aiproject.smartcampus.pojo.po;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * @author lk_hhh
 */
// 1. 用户基础信息表
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

    /**
     * 用户类型枚举
     */
    public enum UserType {
        STUDENT("student"),
        TEACHER("teacher"),
        ADMIN("admin");
        @EnumValue  // 告诉MyBatis-Plus使用这个字段的值与数据库交互
        @JsonValue  // JSON序列化时返回这个值
        private final String value;
        UserType(String value) {
            this.value = value;
        }
        public String getValue() {
            return value;
        }
        /**
         * 根据数据库值获取枚举
         */
        public static UserType fromValue(String value) {
            for (UserType type : UserType.values()) {
                if (type.getValue().equals(value)) {
                    return type;
                }
            }
            throw new IllegalArgumentException("Unknown UserType value: " + value);
        }
    }

    /**
     * 用户状态枚举
     */
    public enum UserStatus {
        ACTIVE("active"),
        INACTIVE("inactive");

        @EnumValue  // 告诉MyBatis-Plus使用这个字段的值与数据库交互
        @JsonValue  // JSON序列化时返回这个值
        private final String value;

        UserStatus(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        /**
         * 根据数据库值获取枚举
         */
        public static UserStatus fromValue(String value) {
            for (UserStatus status : UserStatus.values()) {
                if (status.getValue().equals(value)) {
                    return status;
                }
            }
            return UserStatus.ACTIVE; // 默认返回ACTIVE
        }
    }



}