package com.aiproject.smartcampus.mapper;

import com.aiproject.smartcampus.pojo.po.Student;
import com.aiproject.smartcampus.pojo.vo.StudentSelectAllVO;
import com.aiproject.smartcampus.pojo.vo.StudentWrongQuestionVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

import java.util.List;

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
            @Result(property = "user.phone", column = "phone"),
            @Result(property = "user.status", column = "status"),
            @Result(property = "user.createdAt", column = "created_at"),
    })
    Student findByStudentNumber(@Param("studentNumber") String studentNumber);
    
    /**
     * 查询班级学生列表
     */
    @Select("SELECT * FROM students s LEFT JOIN users u ON s.user_id = u.user_id WHERE s.class_name = #{className}")
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
            @Result(property = "user.phone", column = "phone"),
            @Result(property = "user.status", column = "status"),
            @Result(property = "user.createdAt", column = "created_at")
    })
    List<Student> findByClassName(@Param("className") String className);
    
    /**
     * 查询课程选课学生列表
     */
    @Select("SELECT s.*, u.real_name, u.email FROM students s " +
            "LEFT JOIN users u ON s.user_id = u.user_id " +
            "LEFT JOIN course_enrollments ce ON s.student_id = ce.student_id " +
            "WHERE ce.course_id = #{courseId}")
    List<Student> findByCourseId(@Param("courseId") Integer courseId);

    @Select("select * from students left join users using(user_id)")
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
            @Result(property = "user.phone", column = "phone"),
            @Result(property = "user.status", column = "status"),
            @Result(property = "user.createdAt", column = "created_at"),
    })
    List<StudentSelectAllVO> selectAllStudents();

    /**
     * 查询学生错误题目的描述
     * */

    @Select("SELECT\n" +
            "    sa.answer_id,\n" +
            "    sa.exam_id,\n" +
            "    sa.student_id,\n" +
            "    sa.question_id,\n" +
            "    sa.student_answer,\n" +
            "    sa.score_earned,\n" +
            "    qb.question_content,\n" +
            "    qb.question_type,\n" +
            "    qb.correct_answer,\n" +
            "    qb.explanation,\n" +
            "    qb.difficulty_level,\n" +
            "    qb.score_points,\n" +
            "    e.title AS exam_title,\n" +
            "    e.exam_date,\n" +
            "    c.course_name,\n" +
            "    u.real_name AS student_name\n" +
            "FROM student_answers sa\n" +
            "JOIN question_bank qb ON sa.question_id = qb.question_id\n" +
            "JOIN exams e ON sa.exam_id = e.exam_id\n" +
            "JOIN courses c ON e.course_id = c.course_id\n" +
            "JOIN students s ON sa.student_id = s.student_id\n" +
            "JOIN users u ON s.user_id = u.user_id\n" +
            "WHERE sa.student_id = #{studentId}\n" +
            "  AND c.course_id = #{courseId}\n" +  // 新添加的课程查询条件
            "  AND sa.is_correct = 0  -- 错误题目\n" +
            "ORDER BY sa.exam_id DESC, sa.question_id;")
    List<StudentWrongQuestionVO> selectWrongQuestion(@Param("studentId") String studentId, @Param("courseId") String courseId);


    @Select("select students.user_id from students where student_id=#{studentId}")
    String selectUserIdByStudentIdString(@Param("studentId") String studentId);


}