package com.aiproject.smartcampus.pojo.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @program: ss
 * @description: 课程信息返回
 * @author: lk_hhh
 * @create: 2025-07-07 16:50
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CourseVO {

    /**
     * 课程ID
     */
    private Integer courseId;

    /**
     * 课程名称
     */
    private String courseName;

    /**
     * 课程描述
     */
    private String courseDescription;

    /**
     * 教师姓名
     */
    private String teacherName;

    private static Map<String, String> courseDescriptionMap = new ConcurrentHashMap<>();

    static {
        courseDescriptionMap.put("Java程序设计",
                "本课程系统介绍Java编程语言的基础语法、面向对象编程思想、异常处理、集合框架、多线程编程等核心概念。通过理论学习与实践编程相结合，培养学生使用Java进行软件开发的能力。");

        courseDescriptionMap.put("数据结构与算法",
                "本课程讲授线性表、栈、队列、树、图等基本数据结构，以及排序、查找、动态规划、贪心算法等经典算法。培养学生分析问题、设计高效算法和优化程序性能的能力。");

        courseDescriptionMap.put("高等数学A",
                "本课程涵盖极限、导数、积分、微分方程、无穷级数等数学基础知识。为计算机专业学生提供必要的数学理论基础，培养逻辑思维和数学建模能力。");

        courseDescriptionMap.put("数据库原理",
                "本课程介绍数据库系统的基本概念、关系数据模型、SQL语言、数据库设计理论、事务处理、并发控制等核心内容。培养学生设计和管理数据库系统的实践能力。");

        courseDescriptionMap.put("操作系统",
                "本课程深入讲解操作系统的基本原理，包括进程管理、内存管理、文件系统、设备管理、死锁处理等核心概念。通过理论学习和实验操作，帮助学生理解计算机系统的底层运行机制。");

        courseDescriptionMap.put("计算机网络",
                "本课程系统介绍计算机网络的体系结构、TCP/IP协议族、网络安全、无线网络等内容。培养学生网络应用开发、网络配置管理和网络故障诊断的实际技能。");

        courseDescriptionMap.put("软件工程",
                "本课程讲授软件开发生命周期、需求分析、系统设计、编码实现、测试维护等软件工程方法论。培养学生团队协作开发大型软件项目的工程实践能力。");

        courseDescriptionMap.put("人工智能基础",
                "本课程介绍人工智能的基本概念、搜索算法、知识表示、专家系统、模式识别等核心内容。为学生进入AI领域奠定理论基础，培养智能系统设计思维。");

        courseDescriptionMap.put("机器学习",
                "本课程深入讲解监督学习、无监督学习、强化学习等机器学习算法，包括线性回归、决策树、神经网络、深度学习等前沿技术。培养学生数据分析和智能模型构建能力。");

        courseDescriptionMap.put("Web开发技术",
                "本课程涵盖HTML、CSS、JavaScript前端技术，以及后端开发框架、数据库交互、RESTful API设计等全栈开发技能。培养学生开发现代化Web应用的综合能力。");

        courseDescriptionMap.put("移动应用开发",
                "本课程介绍Android和iOS移动应用开发技术，包括用户界面设计、数据存储、网络通信、设备功能调用等核心内容。培养学生开发跨平台移动应用的实践技能。");

        courseDescriptionMap.put("大数据处理",
                "本课程讲授大数据处理的基本概念、分布式计算框架（如Hadoop、Spark）、数据挖掘技术、数据可视化等内容。培养学生处理海量数据和提取有价值信息的能力。");
    }

    /**
     * 根据课程名称获取课程描述
     * @param courseName 课程名称
     * @return 课程描述，如果未找到则返回默认描述
     */
    public static String getCourseDescription(String courseName) {
        return courseDescriptionMap.getOrDefault(courseName, "暂无课程描述");
    }

    /**
     * 添加新的课程描述
     * @param courseName 课程名称
     * @param description 课程描述
     */
    public static void addCourseDescription(String courseName, String description) {
        courseDescriptionMap.put(courseName, description);
    }

    /**
     * 获取所有课程描述映射
     * @return 课程描述映射表
     */
    public static Map<String, String> getAllCourseDescriptions() {
        return new ConcurrentHashMap<>(courseDescriptionMap);
    }

    /**
     * 构造方法：支持从课程名称自动获取描述
     */
    public CourseVO(Integer courseId, String courseName, String teacherName) {
        this.courseId = courseId;
        this.courseName = courseName;
        this.teacherName = teacherName;
        this.courseDescription = getCourseDescription(courseName);
    }

    /**
     * 构造方法：支持从课程名称自动获取描述（不包含教师）
     */
    public CourseVO(Integer courseId, String courseName) {
        this.courseId = courseId;
        this.courseName = courseName;
        this.courseDescription = getCourseDescription(courseName);
    }

    /**
     * 构造方法：只有课程名称
     */
    public CourseVO(String courseName) {
        this.courseName = courseName;
        this.courseDescription = getCourseDescription(courseName);
    }

    /**
     * 自动设置课程描述（基于课程名称）
     */
    public void autoSetDescription() {
        if (this.courseName != null) {
            this.courseDescription = getCourseDescription(this.courseName);
        }
    }

    /**
     * 获取课程完整信息的字符串表示
     */
    public String getFullCourseInfo() {
        StringBuilder info = new StringBuilder();
        if (courseId != null) {
            info.append("课程ID: ").append(courseId).append(", ");
        }
        if (courseName != null) {
            info.append("课程名称: ").append(courseName).append(", ");
        }
        if (teacherName != null) {
            info.append("授课教师: ").append(teacherName);
        }
        return info.toString();
    }

    /**
     * 重写toString方法，使用中文描述
     */
    @Override
    public String toString() {
        return "课程信息{" +
                "课程ID=" + courseId +
                ", 课程名称='" + courseName + '\'' +
                ", 授课教师='" + teacherName + '\'' +
                ", 课程描述='" + (courseDescription != null && courseDescription.length() > 30 ?
                courseDescription.substring(0, 30) + "..." : courseDescription) + '\'' +
                '}';
    }

    // Lombok会自动生成getter和setter方法，但为了确保兼容性，可以保留手动的getter/setter

    public Integer getCourseId() {
        return courseId;
    }

    public void setCourseId(Integer courseId) {
        this.courseId = courseId;
    }

    public String getCourseName() {
        return courseName;
    }

    public void setCourseName(String courseName) {
        this.courseName = courseName;
        // 自动更新课程描述
        if (courseName != null && (courseDescription == null || "暂无课程描述".equals(courseDescription))) {
            this.courseDescription = getCourseDescription(courseName);
        }
    }

    public String getCourseDescription() {
        return courseDescription;
    }

    public void setCourseDescription(String courseDescription) {
        this.courseDescription = courseDescription;
    }

    public String getTeacherName() {
        return teacherName;
    }

    public void setTeacherName(String teacherName) {
        this.teacherName = teacherName;
    }
}