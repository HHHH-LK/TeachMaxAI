package com.aiproject.smartcampus.pojo.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * @program: SmartCampus
 * @description: 课程安排实体类
 * @author: lk_hhh
 * @create: 2025-06-11 10:44
 **/
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("course_schedule")
public class CourseSchedule implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 课程安排ID
     */
    @TableId(value = "schedule_id", type = IdType.AUTO)
    private Integer scheduleId;

    /**
     * 课程ID
     */
    @TableField("course_id")
    private Integer courseId;

    /**
     * 授课教师ID
     */
    @TableField("teacher_id")
    private Integer teacherId;

    /**
     * 教室
     */
    @TableField("classroom")
    private String classroom;

    /**
     * 课程开始日期
     */
    @TableField("start_date")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    /**
     * 课程结束日期
     */
    @TableField("end_date")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    /**
     * 上课星期（1-7，代表周一至周日）
     */
    @TableField("day_of_week")
    private Integer dayOfWeek;

    /**
     * 上课开始时间
     */
    @TableField("start_time")
    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime startTime;

    /**
     * 上课结束时间
     */
    @TableField("end_time")
    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime endTime;

    /**
     * 考试时间
     */
    @TableField("exam_time")
    private String examTime;

    /**
     * 星期枚举类型
     */
    public enum DayOfWeek {
        MONDAY(1, "周一"),
        TUESDAY(2, "周二"),
        WEDNESDAY(3, "周三"),
        THURSDAY(4, "周四"),
        FRIDAY(5, "周五"),
        SATURDAY(6, "周六"),
        SUNDAY(7, "周日");

        private final Integer code;
        private final String description;

        DayOfWeek(Integer code, String description) {
            this.code = code;
            this.description = description;
        }

        public Integer getCode() {
            return code;
        }

        public String getDescription() {
            return description;
        }

        /**
         * 根据code获取枚举
         */
        public static DayOfWeek getByCode(Integer code) {
            for (DayOfWeek day : DayOfWeek.values()) {
                if (day.getCode().equals(code)) {
                    return day;
                }
            }
            return null;
        }
    }

    /**
     * 获取星期描述
     */
    public String getDayOfWeekDescription() {
        DayOfWeek day = DayOfWeek.getByCode(this.dayOfWeek);
        return day != null ? day.getDescription() : "未知";
    }

    /**
     * 获取时间段描述
     */
    public String getTimeSlot() {
        if (startTime != null && endTime != null) {
            return startTime.toString() + " - " + endTime.toString();
        }
        return "";
    }

    /**
     * 判断是否为工作日
     */
    public boolean isWeekday() {
        return dayOfWeek != null && dayOfWeek >= 1 && dayOfWeek <= 5;
    }

    /**
     * 判断是否为周末
     */
    public boolean isWeekend() {
        return dayOfWeek != null && (dayOfWeek == 6 || dayOfWeek == 7);
    }
}