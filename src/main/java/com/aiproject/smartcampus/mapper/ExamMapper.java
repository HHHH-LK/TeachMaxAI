package com.aiproject.smartcampus.mapper;

import com.aiproject.smartcampus.pojo.po.Exam;
import com.aiproject.smartcampus.pojo.vo.ExamQuestionVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import dev.langchain4j.agent.tool.P;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;


/**
 * @author lk_hhh
 */ // 8. 考试Mapper
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


   /**
    * 根据考试id查询查询出所有的考试题目，按照题目顺序顺序进行展示
    * */

   @Select("SELECT\n" +
           "    pq.paper_question_id,\n" +
           "    pq.paper_id,\n" +
           "    pq.question_id,\n" +
           "    pq.question_order,\n" +
           "    pq.custom_score,\n" +
           "    -- 题目基本信息\n" +
           "    qb.question_content,\n" +
           "    qb.question_type,\n" +
           "    qb.question_options,\n" +
           "    qb.correct_answer,\n" +
           "    qb.explanation,\n" +
           "    qb.difficulty_level,\n" +
           "    qb.score_points,\n" +
           "    qb.point_id,\n" +
           "    -- 试卷信息\n" +
           "    ep.paper_title,\n" +
           "    ep.total_score AS paper_total_score,\n" +
           "    -- 考试信息\n" +
           "    e.exam_id,\n" +
           "    e.title AS exam_title,\n" +
           "    e.exam_date,\n" +
           "    e.duration_minutes,\n" +
           "    e.max_score AS exam_max_score,\n" +
           "    -- 知识点信息\n" +
           "    kp.point_name,\n" +
           "    kp.description AS point_description\n" +
           "FROM exams e\n" +
           "JOIN exam_papers ep ON e.exam_id = ep.exam_id\n" +
           "JOIN paper_questions pq ON ep.paper_id = pq.paper_id\n" +
           "JOIN question_bank qb ON pq.question_id = qb.question_id\n" +
           "LEFT JOIN knowledge_points kp ON qb.point_id = kp.point_id\n" +
           "WHERE e.exam_id =#{examId}\n" +
           "ORDER BY pq.question_order ASC;")
   List<ExamQuestionVO> getAllExamQuestions(@Param(value = "examId") String examId);



}
