package com.aiproject.smartcampus.mapper;

import com.aiproject.smartcampus.pojo.bo.classprase.Course;
import com.aiproject.smartcampus.pojo.po.Exam;
import com.aiproject.smartcampus.pojo.po.ExamScore;
import com.aiproject.smartcampus.pojo.po.Student;
import com.aiproject.smartcampus.pojo.po.Teacher;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * @author lk_hhh
 */ // 2. 学生Mapper
@Repository
@Mapper
public interface StudentMapper extends BaseMapper<Student> {
    
    /**
     * 根据学号查询学生
     */
    @Select("SELECT s.*, u.* FROM students s LEFT JOIN users u ON s.user_id = u.user_id WHERE s.student_number = #{studentNumber}")
    @Results({
        @Result(property = "studentId", column = "student_id"),
        @Result(property = "userId", column = "user_id"),
        @Result(property = "studentNumber", column = "student_number"),
        @Result(property = "grade", column = "grade"),
        @Result(property = "className", column = "class_name"),
        @Result(property = "user.userId", column = "user_id"),
        @Result(property = "user.username", column = "username"),
        @Result(property = "user.realName", column = "real_name"),
        @Result(property = "user.email", column = "email"),
        @Result(property = "user.phone", column = "phone")
    })
    Student findByStudentNumber(@Param("studentNumber") String studentNumber);
    
    /**
     * 查询班级学生列表
     */
    @Select("SELECT s.*, u.real_name, u.email FROM students s LEFT JOIN users u ON s.user_id = u.user_id WHERE s.class_name = #{className}")
    List<Student> findByClassName(@Param("className") String className);
    
    /**
     * 查询课程选课学生列表
     */
    @Select("SELECT s.*, u.real_name, u.email FROM students s " +
            "LEFT JOIN users u ON s.user_id = u.user_id " +
            "LEFT JOIN course_enrollments ce ON s.student_id = ce.student_id " +
            "WHERE ce.course_id = #{courseId}")
    List<Student> findByCourseId(@Param("courseId") Integer courseId);
}