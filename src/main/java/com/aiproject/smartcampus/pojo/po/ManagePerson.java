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
 * 管理员信息表实体类
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("admin")
public class ManagePerson implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 管理员ID，自增主键 */
    @TableId(value = "admin_id", type = IdType.AUTO)
    private Integer adminId;

    /** 管理员账号，唯一 */
    @TableField("account")
    private String account;

    /** 管理员密码（加密存储） */
    @TableField("password")
    private String password;

    /** 管理员姓名 */
    @TableField("name")
    private String name;

    /** 联系电话 */
    @TableField("phone")
    private String phone;
}
