package com.aiproject.smartcampus.mapper;

import com.aiproject.smartcampus.pojo.bo.classprase.Course;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/**
 * @author lk_hhh
 */ // 4. 课程Mapper
@Repository
@Mapper
public interface CourseMapper extends BaseMapper<Course> {
    
    /**
     * 查询教师的课程列表
     */
    @Select("SELECT c.*, t.employee_number, u.real_name as teacher_name FROM courses c " +
            "LEFT JOIN teachers t ON c.teacher_id = t.teacher_id " +
            "LEFT JOIN users u ON t.user_id = u.user_id " +
            "WHERE c.teacher_id = #{teacherId} AND c.status = 'active'")
    List<Course> findByTeacherId(@Param("teacherId") Integer teacherId);
    
    /**
     * 查询学生选修的课程列表
     */
    @Select("SELECT c.*, t.employee_number, u.real_name as teacher_name FROM courses c " +
            "LEFT JOIN teachers t ON c.teacher_id = t.teacher_id " +
            "LEFT JOIN users u ON t.user_id = u.user_id " +
            "LEFT JOIN course_enrollments ce ON c.course_id = ce.course_id " +
            "WHERE ce.student_id = #{studentId}")
    List<Course> findByStudentId(@Param("studentId") Integer studentId);
    
    /**
     * 根据学期查询课程
     */
    @Select("SELECT * FROM courses WHERE semester = #{semester} AND status = 'active'")
    List<Course> findBySemester(@Param("semester") String semester);
    
    /**
     * 课程统计信息
     */
    @Select("SELECT c.course_id, c.course_name, COUNT(ce.student_id) as student_count " +
            "FROM courses c LEFT JOIN course_enrollments ce ON c.course_id = ce.course_id " +
            "WHERE c.teacher_id = #{teacherId} GROUP BY c.course_id")
    List<Map<String, Object>> getCourseStatistics(@Param("teacherId") Integer teacherId);
}