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

/**
 * @author lk_hhh
 */
// 8. 考试表
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("exams")
public class Exam implements Serializable {

    @TableId(value = "exam_id", type = IdType.AUTO)
    private Integer examId;

    @TableField("course_id")
    private Integer courseId;

    @TableField("title")
    private String title;

    @TableField("exam_date")
    private LocalDateTime examDate;

    @TableField("duration_minutes")
    private Integer durationMinutes;

    @TableField("max_score")
    private BigDecimal maxScore;

    @TableField("status")
    private ExamStatus status;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    // 关联课程信息
    @TableField(exist = false)
    private Course course;

    /**
     * 考试状态枚举
     */
    public enum ExamStatus {
        SCHEDULED("scheduled", "已安排"),
        COMPLETED("completed", "已完成"),
        DRAFT("draft", "草稿");

        @EnumValue  // 告诉MyBatis-Plus使用这个字段的值与数据库交互
        @JsonValue  // JSON序列化时返回这个值
        private final String value;
        private final String description;

        ExamStatus(String value, String description) {
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
        public static ExamStatus fromValue(String value) {
            for (ExamStatus status : ExamStatus.values()) {
                if (status.getValue().equals(value)) {
                    return status;
                }
            }
            return ExamStatus.SCHEDULED; // 默认返回已安排
        }
    }


}