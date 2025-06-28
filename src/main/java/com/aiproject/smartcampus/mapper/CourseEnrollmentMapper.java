package com.aiproject.smartcampus.mapper;

import com.aiproject.smartcampus.pojo.po.CourseEnrollment;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.util.List;

// 5. 选课Mapper
@Repository
@Mapper
public interface CourseEnrollmentMapper extends BaseMapper<CourseEnrollment> {
    
    /**
     * 检查学生是否已选择该课程
     */
    @Select("SELECT COUNT(*) FROM course_enrollments WHERE student_id = #{studentId} AND course_id = #{courseId}")
    int checkEnrollment(@Param("studentId") Integer studentId, @Param("courseId") Integer courseId);
    
    /**
     * 获取课程选课统计
     */
    @Select("SELECT COUNT(*) as enrollment_count FROM course_enrollments WHERE course_id = #{courseId}")
    int getEnrollmentCount(@Param("courseId") Integer courseId);
    
    /**
     * 查询学生成绩单
     */
    @Select("SELECT ce.*, c.course_name, u.real_name as teacher_name FROM course_enrollments ce " +
            "LEFT JOIN courses c ON ce.course_id = c.course_id " +
            "LEFT JOIN teachers t ON c.teacher_id = t.teacher_id " +
            "LEFT JOIN users u ON t.user_id = u.user_id " +
            "WHERE ce.student_id = #{studentId}")
    List<CourseEnrollment> getStudentTranscript(@Param("studentId") Integer studentId);
}