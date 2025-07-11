package com.aiproject.smartcampus.mapper;


import com.aiproject.smartcampus.pojo.po.Course;
import com.aiproject.smartcampus.pojo.vo.CourseVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.annotations.*;
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
     * 查询教师的课程列表2
     */
    @Select("SELECT c.* FROM courses c " +  // 移除了无效的别名
            "WHERE c.teacher_id = #{teacherId} AND c.status = 'active'")
    List<Course> findCourseByTeacherId(@Param("teacherId") Integer teacherId);

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


    @Select("select c.*," +
            "       t.employee_number,t.department,t.user_id," +
            "       u.username,u.real_name,u.email,u.user_type,u.phone,u.status as user_status,u.created_at as user_created_at" +
            " from courses c" +
            "         left join teachers t using(teacher_id)" +
            "         left join users u using(user_id)")
    @Results({
            @Result(property = "teacher.teacherId", column = "teacher_id"),
            @Result(property = "teacher.employeeNumber", column = "employee_number"),
            @Result(property = "teacher.department", column = "department"),
            @Result(property = "teacher.userId", column = "user_id"),
            @Result(property = "teacher.user.userId", column = "user_id"),
            @Result(property = "teacher.user.username", column = "username"),
            @Result(property = "teacher.user.realName", column = "real_name"),
            @Result(property = "teacher.user.email", column = "email"),
            @Result(property = "teacher.user.phone", column = "phone"),
            @Result(property = "teacher.user.userType", column = "user_type"),
            @Result(property = "teacher.user.status", column = "user_status"),
            @Result(property = "teacher.user.createdAt", column = "user_created_at")
    })
    List<Course> findAllCourse();


    @Select("SELECT DISTINCT\n" +
            "    c.course_name\n" +
            "FROM courses c\n" +
            "INNER JOIN course_enrollments ce ON c.course_id = ce.course_id\n" +
            "WHERE c.semester = #{date}     -- 学期参数\n" +
            "  AND ce.student_id = #{studentId} -- 学生ID参数\n" +
            "  AND c.status = 'active'\n" +
            "ORDER BY c.course_name;")
    List<CourseVO> findAllCourseByDate(@Param(value = "date") String date, @Param(value = "studentId") String studentId);


    @Select("SELECT DISTINCT\n" +
            "    c.course_name\n" +
            "FROM courses c\n" +
            "INNER JOIN course_enrollments ce ON c.course_id = ce.course_id\n" +
            "WHERE " +
            " ce.student_id = #{studentId} -- 学生ID参数\n" +
            "  AND c.status = 'active'\n" +
            "ORDER BY c.course_name;")
    List<CourseVO> findAllCourseByByStudent(@Param(value = "studentId") String studentId);


    @Select("select courses.course_name\n" +
            "from courses\n" +
            "where course_id=#{courseId}")
    String getCourseByid(@Param(value = "courseId") Integer courseId);


    /**
     * 基于学生ID查询所有学期（去重）
     */
    @Select("SELECT DISTINCT c.semester " +
            "FROM course_enrollments ce " +
            "INNER JOIN courses c ON ce.course_id = c.course_id " +
            "WHERE ce.student_id = #{studentId} " +
            "AND c.semester IS NOT NULL " +
            "ORDER BY c.semester")
    List<String> getStudentSemesters(@Param("studentId") Integer studentId);


    @Select("select courses.course_name\n" +
            "from courses\n" +
            "where course_id=#{courseId}")
    String findCourseNameByid(String courseId);
}