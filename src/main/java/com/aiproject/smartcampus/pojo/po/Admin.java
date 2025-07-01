package com.aiproject.smartcampus.pojo.po;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * @program: SmartCampus
 * @description: 管理员信息表
 * @author: lk
 * @create: 2025-06-28
 **/
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("admins")
public class Admin implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 管理员ID
     */
    @TableId(value = "admin_id", type = IdType.AUTO)
    private Integer adminId;

    /**
     * 关联用户ID
     */
    @TableField("user_id")
    private Integer userId;

    /**
     * 管理员工号
     */
    @TableField("admin_number")
    private String adminNumber;

    /**
     * 所属部门
     */
    @TableField("department")
    private String department;

    /**
     * 职位
     */
    @TableField("position")
    private String position;

    /**
     * 管理员等级
     */
    @TableField("admin_level")
    private AdminLevel adminLevel;

    /**
     * 权限配置(JSON格式)
     */
    @TableField("permissions")
    private String permissions;

    /**
     * 最后登录时间
     */
    @TableField("last_login_time")
    private LocalDateTime lastLoginTime;

    /**
     * 最后登录IP
     */
    @TableField("last_login_ip")
    private String lastLoginIp;

    /**
     * 状态
     */
    @TableField("status")
    private AdminStatus status;

    /**
     * 创建时间
     */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    // 关联用户信息（不映射到数据库）
    @TableField(exist = false)
    private User user;

    /**
     * 管理员等级枚举
     */
    public enum AdminLevel {
        SUPER_ADMIN("super_admin", "超级管理员"),
        ADMIN("admin", "系统管理员"),
        OPERATOR("operator", "操作员");

        @EnumValue  // 告诉MyBatis-Plus使用这个字段的值与数据库交互
        @JsonValue  // JSON序列化时返回这个值
        private final String value;
        private final String description;

        AdminLevel(String value, String description) {
            this.value = value;
            this.description = description;
        }

        public String getValue() {
            return value;
        }

        public String getDescription() {
            return description;
        }

        public static AdminLevel fromValue(String value) {
            for (AdminLevel level : AdminLevel.values()) {
                if (level.getValue().equals(value)) {
                    return level;
                }
            }
            return ADMIN; // 默认值
        }
    }

    /**
     * 管理员状态枚举
     */
    public enum AdminStatus {
        ACTIVE("active", "激活"),
        INACTIVE("inactive", "禁用");

        @EnumValue  // 告诉MyBatis-Plus使用这个字段的值与数据库交互
        @JsonValue  // JSON序列化时返回这个值
        private final String value;
        private final String description;

        AdminStatus(String value, String description) {
            this.value = value;
            this.description = description;
        }

        public String getValue() {
            return value;
        }

        public String getDescription() {
            return description;
        }

        public static AdminStatus fromValue(String value) {
            for (AdminStatus status : AdminStatus.values()) {
                if (status.getValue().equals(value)) {
                    return status;
                }
            }
            return ACTIVE; // 默认值
        }
    }


}