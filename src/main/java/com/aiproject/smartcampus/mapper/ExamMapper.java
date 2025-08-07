// ==================== 修复ExamMapper中的SQL查询 ====================

package com.aiproject.smartcampus.mapper;

import com.aiproject.smartcampus.pojo.po.Exam;
import com.aiproject.smartcampus.pojo.po.ExamPaper;
import com.aiproject.smartcampus.pojo.po.PaperQuestion;
import com.aiproject.smartcampus.pojo.vo.ExamQuestionVO;
import com.aiproject.smartcampus.pojo.vo.ExamScoreVO;
import com.aiproject.smartcampus.pojo.vo.ExamStudentVO;
import com.aiproject.smartcampus.pojo.vo.StudentExamAnswerVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * @author lk_hhh
 */
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
     */
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
            "WHERE e.exam_id = #{examId}\n" +
            "ORDER BY pq.question_order ASC;")
    List<ExamQuestionVO> getAllExamQuestions(@Param("examId") String examId);

    /**
     * 查询课程考试中所有的错误知识点
     */
    @Select("SELECT DISTINCT qb.point_id\n" +
            "FROM courses c\n" +
            "INNER JOIN exams e ON c.course_id = e.course_id\n" +
            "INNER JOIN exam_papers ep ON e.exam_id = ep.exam_id\n" +
            "INNER JOIN paper_questions pq ON ep.paper_id = pq.paper_id\n" +
            "INNER JOIN question_bank qb ON pq.question_id = qb.question_id\n" +
            "INNER JOIN student_answers sa ON qb.question_id = sa.question_id AND e.exam_id = sa.exam_id\n" +
            "WHERE c.course_id = #{courseId}\n" +
            "  AND sa.is_correct = 0\n" +
            "  AND qb.point_id IS NOT NULL\n" +
            "ORDER BY qb.point_id;")
    List<Integer> getAllPointIdByExamCourseId(@Param("courseId") String courseId);

    /**
     * 查询学生考试作答情况 - 修复硬编码问题
     */
    @Select("SELECT\n" +
            "    sa.answer_id,\n" +
            "    pq.question_order,\n" +
            "    qb.question_id,\n" +
            "    qb.question_type,\n" +
            "    qb.question_content,\n" +
            "    qb.question_options,\n" +
            "    qb.correct_answer,\n" +
            "    sa.student_answer,\n" +
            "    sa.is_correct,\n" +
            "    sa.score_earned,\n" +
            "    COALESCE(pq.custom_score, qb.score_points) as max_score,\n" +
            "    qb.explanation,\n" +
            "    qb.difficulty_level,\n" +
            "    kp.point_name as knowledge_point_name\n" +
            "FROM student_answers sa\n" +
            "INNER JOIN question_bank qb ON sa.question_id = qb.question_id\n" +
            "INNER JOIN paper_questions pq ON qb.question_id = pq.question_id\n" +
            "INNER JOIN exam_papers ep ON pq.paper_id = ep.paper_id\n" +
            "LEFT JOIN knowledge_points kp ON qb.point_id = kp.point_id\n" +
            "WHERE sa.exam_id = #{examId}\n" +  // 修复：使用参数而不是硬编码1
            "  AND sa.student_id = #{studentId}\n" +  // 修复：使用参数而不是硬编码1
            "  AND ep.exam_id = sa.exam_id\n" +
            "ORDER BY pq.question_order;")
    List<StudentExamAnswerVO> getAllStudentAnwer(@Param("examId") String examId, @Param("studentId") String studentId);

    /**
     * 更新学生答案的评分结果
     *
     * @param examId      考试ID
     * @param studentId   学生ID
     * @param questionId  题目ID
     * @param isCorrect   是否正确 (true: 正确, false: 错误)
     * @param scoreEarned 获得分数
     * @return 更新的记录数
     */
    int updateStudentAnswer(@Param("examId") Integer examId,
                            @Param("studentId") Integer studentId,
                            @Param("questionId") Integer questionId,
                            @Param("isCorrect") Boolean isCorrect,
                            @Param("scoreEarned") BigDecimal scoreEarned);

    /**
     * 更新考试总成绩
     *
     * @param examId      考试ID
     * @param studentId   学生ID
     * @param score       总得分
     * @param submittedAt 提交/评卷完成时间
     * @return 更新的记录数
     */
    int updateExamScore(@Param("examId") Integer examId,
                        @Param("studentId") Integer studentId,
                        @Param("score") BigDecimal score,
                        @Param("submittedAt") LocalDateTime submittedAt);

    /**
     * 获取考试的所有学生ID（用于批量评卷）
     */
    @Select("SELECT DISTINCT es.student_id\n" +
            "FROM exam_scores es\n" +
            "WHERE es.exam_id = #{examId}\n" +
            "ORDER BY es.student_id")
    List<Integer> getExamStudents(@Param("examId") String examId);

    /**
     * 查询考试成绩是否存在
     */
    @Select("SELECT COUNT(*) > 0\n" +
            "FROM exam_scores\n" +
            "WHERE exam_id = #{examId} AND student_id = #{studentId}")
    boolean existsExamScore(@Param("examId") Integer examId,
                            @Param("studentId") Integer studentId);

    /**
     * 插入考试成绩记录
     */
    int insertExamScore(@Param("examId") Integer examId,
                        @Param("studentId") Integer studentId,
                        @Param("score") BigDecimal score,
                        @Param("submittedAt") LocalDateTime submittedAt);

    /**
     * 插入或更新考试成绩
     */
    int insertOrUpdateExamScore(@Param("examId") Integer examId,
                                @Param("studentId") Integer studentId,
                                @Param("score") BigDecimal score,
                                @Param("submittedAt") LocalDateTime submittedAt);

    /**
     * 插入未作答题目记录
     *
     * @param examId        考试ID
     * @param studentId     学生ID
     * @param questionId    题目ID
     * @param studentAnswer 学生答案（空字符串）
     * @param isCorrect     是否正确（false）
     * @param scoreEarned   获得分数（0）
     * @return 插入的记录数
     */
    int insertUnansweredQuestion(@Param("examId") Integer examId,
                                 @Param("studentId") Integer studentId,
                                 @Param("questionId") Integer questionId,
                                 @Param("studentAnswer") String studentAnswer,
                                 @Param("isCorrect") Boolean isCorrect,
                                 @Param("scoreEarned") BigDecimal scoreEarned);

    /**
     * 批量插入未作答题目记录
     */
    int batchInsertUnansweredQuestions(@Param("examId") Integer examId,
                                       @Param("studentId") Integer studentId,
                                       @Param("questionIds") List<Integer> questionIds);


    /**
     * 更新学生课程最终成绩到course_enrollments表
     * @param courseId 课程ID
     * @param studentId 学生ID
     * @param finalGrade 最终成绩
     * @return 更新的记录数
     */
    int updateStudentFinalGrade(@Param("courseId") Integer courseId,
                                @Param("studentId") Integer studentId,
                                @Param("finalGrade") BigDecimal finalGrade);

    /**
     * 根据考试ID获取课程ID
     * @param examId 考试ID
     * @return 课程ID
     */
    Integer getCourseIdByExamId(@Param("examId") Integer examId);

    /**
     * 计算学生在某课程的平均考试成绩
     * @param courseId 课程ID
     * @param studentId 学生ID
     * @return 平均成绩
     */
    BigDecimal calculateStudentAverageScore(@Param("courseId") Integer courseId,
                                            @Param("studentId") Integer studentId);

    /**
     * 获取学生在某课程的所有考试成绩
     * @param courseId 课程ID
     * @param studentId 学生ID
     * @return 考试成绩列表
     */
    List<ExamScoreVO> getStudentExamScores(@Param("courseId") Integer courseId,
                                           @Param("studentId") Integer studentId);

    /**
     * 插入试卷记录
     */
    @Insert("INSERT INTO exam_papers (exam_id, paper_title, total_score, question_count) " +
            "VALUES (#{examId}, #{paperTitle}, #{totalScore}, #{questionCount})")
    @Options(useGeneratedKeys = true, keyProperty = "paperId")
    int insertExamPaper(ExamPaper examPaper);

    /**
     * 插入试卷题目关联
     */
    @Insert("INSERT INTO paper_questions (paper_id, question_id, question_order, custom_score) " +
            "VALUES (#{paperId}, #{questionId}, #{questionOrder}, #{customScore})")
    @Options(useGeneratedKeys = true, keyProperty = "paperQuestionId")
    int insertPaperQuestion(PaperQuestion paperQuestion);

    /**
     * 批量插入试卷题目关联
     */
    int batchInsertPaperQuestions(@Param("paperId") Integer paperId,
                                  @Param("questionList") List<PaperQuestion> questionList);

    /**
     * 获取课程的所有学生ID（用于批量操作）
     */
    @Select("SELECT DISTINCT student_id FROM course_enrollments WHERE course_id = #{courseId}")
    List<Integer> getCourseStudents(@Param("courseId") Integer courseId);


    @Select("SELECT " +
            "e.exam_id AS examId, " +
            "e.title AS examName, " +  
            "c.course_id AS courseId, " +
            "c.course_name AS courseName, " +
            "s.student_id AS studentId, " +
            "u.real_name AS studentName, " +
            "s.student_number AS studentNumber, " +
            "es.score AS score " +
            "FROM exams e " +
            "JOIN courses c ON e.course_id = c.course_id " +
            "JOIN course_enrollments ce ON c.course_id = ce.course_id " +
            "JOIN students s ON ce.student_id = s.student_id " +
            "JOIN users u ON s.user_id = u.user_id " +
            "LEFT JOIN exam_scores es ON e.exam_id = es.exam_id AND s.student_id = es.student_id " +
            "WHERE e.exam_id = #{examId} " +
            "AND c.teacher_id = #{teacherId} " +
            "ORDER BY s.class_name, s.student_number")
    List<ExamStudentVO> getExamStudentInfo(
            @Param("examId") String examId,
            @Param("teacherId") String teacherId);

    @Select("SELECT * FROM exams WHERE course_id = #{courseId} AND status != 'draft'")
    List<Exam> findByCourseIdAndStatusNotDraft(@Param("courseId") String courseId);

}