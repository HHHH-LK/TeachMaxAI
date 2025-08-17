package com.aiproject.smartcampus.mapper;

import com.aiproject.smartcampus.pojo.dto.TeacherGetStudentDTO;
import com.aiproject.smartcampus.pojo.po.CourseEnrollment;
import com.aiproject.smartcampus.pojo.po.Student;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author lk_hhh
 */ // 5. 选课Mapper
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

    /**
     * 仅查询成绩
     */
    @Select("select score\n" +
            "from exam_scores\n" +
            "where exam_id=#{examId}")
    List<Double> getStudentScores(@Param("examId") Integer examId);

    /**
     * 查询学生全部信息
     */
    @Select("SELECT " +
            "ce.course_id AS courseId, " +
            "c.course_name AS courseName, " + // 课程名称需要从其他表获取，暂时设为空字符串
            "u.real_name AS realName, " +
            "s.student_number AS studentNumber, " +
            "u.email AS Email, " +
            "u.phone AS Phone, " +
            "s.class_name AS className, " +
            "ce.final_grade AS score " +
            "FROM course_enrollments ce " +
            "JOIN students s ON ce.student_id = s.student_id " +
            "JOIN users u ON s.user_id = u.user_id " +
            "JOIN courses c ON ce.course_id = c.course_id " +
            "WHERE ce.course_id = #{courseId}")
    List<TeacherGetStudentDTO> getStudentInfo(@Param("courseId") Integer courseId);

    @Select("SELECT COUNT(*) FROM course_enrollments WHERE course_id = #{courseId}")
    Integer countEnrollmentsByCourseId(@Param("courseId") String courseId);

    @Select("SELECT COUNT(*) FROM course_enrollments WHERE course_id = #{courseId} AND status = 'active'")
    Integer countActiveEnrollmentsByCourseId(@Param("courseId") String courseId);

    // 根据课程ID查询所有选课学生
    @Select("SELECT s.* FROM students s " +
            "JOIN course_enrollments ce ON s.student_id = ce.student_id " +
            "WHERE ce.course_id = #{courseId}")
    List<Student> findStudentsByCourseId(@Param("courseId") String courseId);

    @Delete("DELETE FROM course_enrollments WHERE course_id = #{courseId}")
    int deleteBycourseId(Integer courseId);

    @Select("SELECT student_id FROM course_enrollments WHERE course_id = #{courseId}")
    List<String> findStudentIdsByCourseId(String courseId);
}