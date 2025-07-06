package com.aiproject.smartcampus.mapper;

import com.aiproject.smartcampus.pojo.po.Exam;
import com.aiproject.smartcampus.pojo.po.ExamScore;
import com.aiproject.smartcampus.pojo.po.StudentAnswer;
import com.aiproject.smartcampus.pojo.po.Teacher;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
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
}