package com.aiproject.smartcampus.pojo.po;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * @author lk_hhh
 */
// 4. 课程信息表
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("courses")
public class Course implements Serializable {

    @TableId(value = "course_id", type = IdType.AUTO)
    private Integer courseId;

    @TableField("course_name")
    private String courseName;

    @TableField("teacher_id")
    private Integer teacherId;

    @TableField("semester")
    private String semester;

    @TableField("status")
    private CourseStatus status;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    // 关联信息
    @TableField(exist = false)
    private Teacher teacher;

    @TableField(exist = false)
    private List<CourseEnrollment> enrollments;

    @TableField(exist = false)
    private List<Assignment> assignments;

    @TableField(exist = false)
    private List<Exam> exams;

    @TableField(exist = false)
    private List<KnowledgePoint> knowledgePoints;

    /**
     * 课程状态枚举
     */
    public enum CourseStatus {
        ACTIVE("active", "活跃"),
        INACTIVE("inactive", "非活跃");

        @EnumValue  // 告诉MyBatis-Plus使用这个字段的值与数据库交互
        @JsonValue  // JSON序列化时返回这个值
        private final String value;
        private final String description;

        CourseStatus(String value, String description) {
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
        public static CourseStatus fromValue(String value) {
            for (CourseStatus status : CourseStatus.values()) {
                if (status.getValue().equals(value)) {
                    return status;
                }
            }
            return CourseStatus.ACTIVE; // 默认返回活跃状态
        }
    }


}