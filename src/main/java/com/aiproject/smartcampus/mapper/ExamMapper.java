package com.aiproject.smartcampus.mapper;

import com.aiproject.smartcampus.pojo.bo.classprase.Course;
import com.aiproject.smartcampus.pojo.po.Exam;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;


// 8. 考试Mapper
@Repository
@Mapper
public interface ExamMapper extends BaseMapper<Exam> {

    /**
     * 查询课程考试列表
     */
    @Select("SELECT * FROM exams WHERE course_id = #{courseId} ORDER BY exam_date DESC")
    List<Exam> findByCourseId(@Param("courseId") Integer courseId);

    /**
     * 查询学生考试列表
     */
    @Select("SELECT e.*, c.course_name FROM exams e " +
            "LEFT JOIN courses c ON e.course_id = c.course_id " +
            "LEFT JOIN course_enrollments ce ON c.course_id = ce.course_id " +
            "WHERE ce.student_id = #{studentId}")
    List<Exam> findByStudentId(@Param("studentId") Integer studentId);

    /**
     * 查询即将开始的考试
     */
    @Select("SELECT e.*, c.course_name FROM exams e " +
            "LEFT JOIN courses c ON e.course_id = c.course_id " +
            "WHERE e.exam_date BETWEEN NOW() AND DATE_ADD(NOW(), INTERVAL 7 DAY) " +
            "AND e.status = 'scheduled'")
    List<Exam> findUpcomingExams();
}
