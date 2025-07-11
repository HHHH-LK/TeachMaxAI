package com.aiproject.smartcampus.pojo.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 教案管理实体类
 * @author lk_hhh
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("lesson_plans")
public class LessonPlan implements Serializable {

    @TableId(value = "plan_id", type = IdType.AUTO)
    private Long planId;

    @TableField("course_id")
    private Integer courseId;

    @TableField("chapter_id")
    private Integer chapterId;

    @TableField("teacher_id")
    private Integer teacherId;

    @TableField("plan_title")
    private String planTitle;

    @TableField("plan_content")
    private String planContent;

    @TableField("audit_status")
    private String auditStatus;

    @TableField("audit_admin_id")
    private Integer auditAdminId;

    @TableField("audit_time")
    private LocalDateTime auditTime;

    @TableField("audit_comments")
    private String auditComments;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;

    // 关联信息（不存储在数据库）
    @TableField(exist = false)
    private String courseName;

    @TableField(exist = false)
    private String chapterName;

    @TableField(exist = false)
    private String teacherName;

    @TableField(exist = false)
    private String adminName;

    /**
     * 中文toString方法
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("教案信息{");
        sb.append("教案编号=").append(planId);
        sb.append(", 教案标题='").append(planTitle != null ? planTitle : "未设置").append('\'');
        sb.append(", 课程名称='").append(courseName != null ? courseName : "未知课程").append('\'');
        sb.append(", 章节名称='").append(chapterName != null ? chapterName : "整体课程").append('\'');
        sb.append(", 创建教师='").append(teacherName != null ? teacherName : "未知教师").append('\'');
        sb.append(", 审核状态='").append(getAuditStatusText()).append('\'');
        
        if (auditAdminId != null) {
            sb.append(", 审核管理员='").append(adminName != null ? adminName : "未知管理员").append('\'');
        }
        
        if (auditTime != null) {
            sb.append(", 审核时间=").append(auditTime);
        }
        
        sb.append(", 创建时间=").append(createdAt);
        sb.append('}');
        return sb.toString();
    }

    /**
     * 获取审核状态中文描述
     */
    public String getAuditStatusText() {
        if (auditStatus == null) return "未知状态";
        
        return switch (auditStatus) {
            case "draft" -> "草稿";
            case "pending" -> "待审核";
            case "approved" -> "已通过";
            case "rejected" -> "已拒绝";
            default -> auditStatus;
        };
    }

    /**
     * 检查是否可以提交审核
     */
    public boolean canSubmitForAudit() {
        return "draft".equals(auditStatus) || "rejected".equals(auditStatus);
    }

    /**
     * 检查是否可以审核
     */
    public boolean canBeAudited() {
        return "pending".equals(auditStatus);
    }

    /**
     * 检查是否已通过审核
     */
    public boolean isApproved() {
        return "approved".equals(auditStatus);
    }
}