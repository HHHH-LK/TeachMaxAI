package com.aiproject.smartcampus.pojo.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * @program: SmartCampus
 * @description: 资源下载记录表实体类
 * @author: lk
 * @create: 2025-07-01
 **/
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("resource_downloads")
public class ResourceDownload implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 下载ID
     */
    @TableId(value = "download_id", type = IdType.AUTO)
    private Integer downloadId;

    /**
     * 资源ID
     */
    @TableField("resource_id")
    private Integer resourceId;

    /**
     * 下载用户ID
     */
    @TableField("user_id")
    private Integer userId;

    /**
     * 下载时间
     */
    @TableField(value = "download_time", fill = FieldFill.INSERT)
    private LocalDateTime downloadTime;

    // ==================== 关联对象（不映射到数据库）====================

    /**
     * 关联资源信息
     */
    @TableField(exist = false)
    private CoursewareResource resource;

    /**
     * 下载用户信息
     */
    @TableField(exist = false)
    private User user;


}