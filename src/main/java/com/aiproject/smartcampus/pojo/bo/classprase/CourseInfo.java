package com.aiproject.smartcampus.pojo.bo.classprase;

import lombok.Data;

// 课程信息实体类
    @Data
    public  class CourseInfo {
        public String courseName;      // 课程名称
        public String teacher;         // 教师
        public String dayOfWeek;       // 星期几
        public String timeSlot;        // 时间段
        public String location;        // 地点
        public String weeks;           // 周次
        public String examInfo;        // 考试信息
        public String courseType;      // 课程类型
        public String credits;         // 学分


}