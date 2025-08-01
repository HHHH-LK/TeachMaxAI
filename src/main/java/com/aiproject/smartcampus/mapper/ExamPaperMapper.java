package com.aiproject.smartcampus.mapper;


import com.aiproject.smartcampus.pojo.po.Exam;
import com.aiproject.smartcampus.pojo.po.ExamPaper;
import com.aiproject.smartcampus.pojo.vo.ExamQuestionDetailVO;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;


/**
 * @author lk_hhh
 */ // 16. 试卷相关Mapper
@Repository
@Mapper
public interface ExamPaperMapper extends BaseMapper<ExamPaper> {

    /**
     * 查询考试试卷
     */
    @Select("SELECT * FROM exam_papers WHERE exam_id = #{examId}")
    List<ExamPaper> findByExamId(@Param("examId") Integer examId);


    @Select("SELECT " +
            "qb.question_id AS questionId, " +
            "pq.question_order AS questionOrder, " +
            "COALESCE(pq.custom_score, qb.score_points) AS score, " +
            "qb.score_points AS scorePoints, " +
            "kp.point_name AS pointName, " +
            "c.chapter_id AS chapterId, " +
            "c.chapter_name AS chapterName, " +
            "qb.question_type AS questionType, " +
            "qb.question_content AS questionContent, " +
            "qb.question_options AS questionOptions, " +
            "qb.correct_answer AS correctAnswer, " +
            "qb.explanation AS explanation, " +
            "qb.difficulty_level AS difficultyLevel, " +
            "sa.student_answer AS studentAnswer, " +
            "sa.is_correct AS isCorrect, " +
            "sa.score_earned AS scoreEarned, " +
            "sa.answer_id AS answerId, " +
            "qb.created_at AS createdAt " +
            "FROM paper_questions pq " +
            "INNER JOIN question_bank qb ON pq.question_id = qb.question_id " +
            "LEFT JOIN knowledge_points kp ON qb.point_id = kp.point_id " +
            "LEFT JOIN chapter_knowledge_points ckp ON kp.point_id = ckp.point_id " +
            "LEFT JOIN chapters c ON ckp.chapter_id = c.chapter_id " +
            "LEFT JOIN student_answers sa ON sa.question_id = pq.question_id " +
            "   AND sa.exam_id = #{examId} " +
            "   AND sa.student_id = #{studentId} " +
            "WHERE pq.paper_id = #{paperId} " +
            "ORDER BY pq.question_order")
    List<ExamQuestionDetailVO> getExamTestQuestions(
            @Param("paperId") Integer paperId,
            @Param("studentId") Integer studentId,
            @Param("examId") Integer examId
    );

    // ExamPaperMapper
    @Delete("DELETE FROM exam_papers WHERE exam_id = #{examId}")
    int deleteByExamId(@Param("examId") int examId);
}

