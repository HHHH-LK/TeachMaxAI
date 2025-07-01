package com.aiproject.smartcampus.pojo.po;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * @author lk_hhh
 */
// 6. 作业表
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("assignments")
public class Assignment implements Serializable {

    @TableId(value = "assignment_id", type = IdType.AUTO)
    private Integer assignmentId;

    @TableField("course_id")
    private Integer courseId;

    @TableField("title")
    private String title;

    @TableField("description")
    private String description;

    @TableField("due_date")
    private LocalDateTime dueDate;

    @TableField("max_score")
    private BigDecimal maxScore;

    @TableField("status")
    private AssignmentStatus status;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    // 关联信息
    @TableField(exist = false)
    private Course course;

    @TableField(exist = false)
    private List<AssignmentSubmission> submissions;

    /**
     * 作业状态枚举
     */
    public enum AssignmentStatus {
        DRAFT("draft", "草稿"),
        PUBLISHED("published", "已发布"),
        CLOSED("closed", "已关闭");

        @EnumValue  // 告诉MyBatis-Plus使用这个字段的值与数据库交互
        @JsonValue  // JSON序列化时返回这个值
        private final String value;
        private final String description;

        AssignmentStatus(String value, String description) {
            this.value = value;
            this.description = description;
        }

        public String getValue() {
            return value;
        }

        public String getDescription() {
            return description;
        }

        /**
         * 根据数据库值获取枚举
         */
        public static AssignmentStatus fromValue(String value) {
            for (AssignmentStatus status : AssignmentStatus.values()) {
                if (status.getValue().equals(value)) {
                    return status;
                }
            }
            return AssignmentStatus.DRAFT; // 默认返回草稿状态
        }
    }

}