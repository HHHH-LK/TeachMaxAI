package com.aiproject.smartcampus.pojo.vo;

import lombok.Data;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 教师信息VO类（纯数据传输对象）
 */
@Data
public class TeacherInfoVO {

    private static final Map<String, List<String>> COURSE_TAGS_MAP = new ConcurrentHashMap<>();

    static {
        // Java程序设计
        COURSE_TAGS_MAP.put("Java程序设计", Arrays.asList("编程基础", "面向对象", "后端开发", "企业级"));

        // 数据结构与算法
        COURSE_TAGS_MAP.put("数据结构与算法", Arrays.asList("算法思维", "编程核心", "逻辑思维", "面试必备"));

        // 高等数学A
        COURSE_TAGS_MAP.put("高等数学A", Arrays.asList("数学基础", "理论课程", "逻辑推理", "必修课"));

        // 数据库原理
        COURSE_TAGS_MAP.put("数据库原理", Arrays.asList("数据管理", "SQL语言", "后端技术", "企业应用"));

        // 操作系统
        COURSE_TAGS_MAP.put("操作系统", Arrays.asList("系统原理", "底层技术", "进程管理", "核心课程"));

        // 计算机网络
        COURSE_TAGS_MAP.put("计算机网络", Arrays.asList("网络协议", "通信技术", "互联网", "基础设施"));

        // 软件工程
        COURSE_TAGS_MAP.put("软件工程", Arrays.asList("项目管理", "开发流程", "团队协作", "工程实践"));

        // 人工智能基础
        COURSE_TAGS_MAP.put("人工智能基础", Arrays.asList("AI入门", "前沿技术", "智能算法", "未来趋势"));

        // 机器学习
        COURSE_TAGS_MAP.put("机器学习", Arrays.asList("数据挖掘", "预测模型", "AI核心", "实战应用"));

        // Web开发技术
        COURSE_TAGS_MAP.put("Web开发技术", Arrays.asList("前端开发", "网页设计", "用户交互", "实用技能"));

        // 移动应用开发
        COURSE_TAGS_MAP.put("移动应用开发", Arrays.asList("移动端", "APP开发", "跨平台", "用户体验"));

        // 大数据处理
        COURSE_TAGS_MAP.put("大数据处理", Arrays.asList("数据分析", "分布式", "云计算", "热门领域"));
    }

    
    /**
     * 教师ID
     */
    private Integer teacherId;

    /**
     * 老师姓名
     */
    private String teacherName;

    /**
     * 所交课程名字
     * */
    private String courseName;

    /**
     * 老师部门
     */
    private String department;

    /**
     * 老师状态
     */
    private String status;

    /**
     * 老师课程标签
     * */
    private List<String> tags;

    /**
     * 排序权重
     * */
    private Integer t;

    /**
     * 获取排序权重
     * */
    public Integer setTBystatus(){
        if(status.equals("active")){
            return 10;
        }else{
            return 0;
        }
    }

    /**
     * 获取课程标签
     * @param courseName 课程名称
     * @return 标签列表
     */
    public List<String> getCourseTags(String courseName) {
        return COURSE_TAGS_MAP.getOrDefault(courseName, Collections.emptyList());
    }
}