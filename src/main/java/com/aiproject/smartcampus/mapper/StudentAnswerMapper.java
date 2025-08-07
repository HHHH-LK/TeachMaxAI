package com.aiproject.smartcampus.mapper;

import com.aiproject.smartcampus.pojo.bo.StudentKnowBO;
import com.aiproject.smartcampus.pojo.po.StudentAnswer;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/**
 * @author lk_hhh
 */ // 12. 学生答题记录Mapper
@Repository
@Mapper
public interface StudentAnswerMapper extends BaseMapper<StudentAnswer> {
    
    /**
     * 查询学生考试答题记录
     */
    @Select("SELECT * FROM student_answers WHERE exam_id = #{examId} AND student_id = #{studentId}")
    List<StudentAnswer> findByExamAndStudent(@Param("examId") Integer examId, @Param("studentId") Integer studentId);
    
    /**
     * 查询题目答题统计
     */
    @Select("SELECT " +
            "COUNT(*) as total_answers, " +
            "COUNT(CASE WHEN is_correct = 1 THEN 1 END) as correct_count, " +
            "AVG(score_earned) as avg_score " +
            "FROM student_answers WHERE question_id = #{questionId}")
    Map<String, Object> getQuestionAnswerStatistics(@Param("questionId") Integer questionId);
    
    /**
     * 查询学生错题记录
     */
    @Select("SELECT sa.*, qb.question_content, qb.correct_answer FROM student_answers sa " +
            "LEFT JOIN question_bank qb ON sa.question_id = qb.question_id " +
            "WHERE sa.student_id = #{studentId} AND sa.is_correct = 0")
    List<StudentAnswer> findWrongAnswers(@Param("studentId") Integer studentId);

    @Delete("DELETE FROM student_answers WHERE exam_id = #{examId}")
    int deleteByExamId(@Param("examId") int examId);

    @Select("SELECT\n" +
            "    kp.point_id,\n" +
            "    kp.point_name,\n" +
            "    kp.description,\n" +
            "    kp.difficulty_level,\n" +
            "    kp.keywords,\n" +
            "    c.course_name,\n" +
            "    sa.student_id,\n" +
            "    COUNT(sa.answer_id) as wrong_answer_count,\n" +
            "    ROUND(AVG(CASE WHEN sa.is_correct = 1 THEN 1 ELSE 0 END) * 100, 2) as accuracy_rate\n" +
            "FROM student_answers sa\n" +
            "INNER JOIN question_bank qb ON sa.question_id = qb.question_id\n" +
            "INNER JOIN knowledge_points kp ON qb.point_id = kp.point_id\n" +
            "INNER JOIN courses c ON kp.course_id = c.course_id\n" +
            "WHERE sa.is_correct = 0\n" +
            "GROUP BY kp.point_id, kp.point_name, kp.description, kp.difficulty_level, kp.keywords, c.course_name, sa.student_id\n" +
            "ORDER BY kp.point_id, sa.student_id;")
    List<StudentKnowBO> getAllWrongKnowledgeFrequency();

    @Select("SELECT\n" +
            "    kp.point_id,\n" +
            "    kp.point_name,\n" +
            "    kp.description,\n" +
            "    kp.difficulty_level,\n" +
            "    kp.keywords,\n" +
            "    c.course_name,\n" +
            "    sa.student_id,\n" +
            "    sa.is_correct\n" + // 新增：是否答对
            "FROM student_answers sa\n" +
            "INNER JOIN question_bank qb ON sa.question_id = qb.question_id\n" +
            "INNER JOIN knowledge_points kp ON qb.point_id = kp.point_id\n" +
            "INNER JOIN courses c ON kp.course_id = c.course_id\n" +
            "ORDER BY kp.point_id, sa.student_id;")
    List<StudentKnowBO> getAllAnswerKnowledgeFrequency();
}