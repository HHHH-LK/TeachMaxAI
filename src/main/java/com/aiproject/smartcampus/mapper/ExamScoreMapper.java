package com.aiproject.smartcampus.mapper;

import com.aiproject.smartcampus.pojo.bo.classprase.Course;
import com.aiproject.smartcampus.pojo.po.Exam;
import com.aiproject.smartcampus.pojo.po.ExamScore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;



// 9. 考试成绩Mapper
@Repository
@Mapper
public interface ExamScoreMapper extends BaseMapper<ExamScore> {
    
    /**
     * 查询考试成绩列表
     */
    @Select("SELECT es.*, s.student_number, u.real_name as student_name FROM exam_scores es " +
            "LEFT JOIN students s ON es.student_id = s.student_id " +
            "LEFT JOIN users u ON s.user_id = u.user_id " +
            "WHERE es.exam_id = #{examId}")
    List<ExamScore> findByExamId(@Param("examId") Integer examId);
    
    /**
     * 查询学生考试成绩
     */
    @Select("SELECT es.*, e.title, e.max_score, c.course_name FROM exam_scores es " +
            "LEFT JOIN exams e ON es.exam_id = e.exam_id " +
            "LEFT JOIN courses c ON e.course_id = c.course_id " +
            "WHERE es.student_id = #{studentId}")
    List<ExamScore> findByStudentId(@Param("studentId") Integer studentId);
    
    /**
     * 考试成绩统计
     */
    @Select("SELECT " +
            "COUNT(*) as total_count, " +
            "AVG(score) as avg_score, " +
            "MAX(score) as max_score, " +
            "MIN(score) as min_score, " +
            "COUNT(CASE WHEN score >= #{passScore} THEN 1 END) as pass_count " +
            "FROM exam_scores WHERE exam_id = #{examId}")
    Map<String, Object> getExamStatistics(@Param("examId") Integer examId, @Param("passScore") BigDecimal passScore);
}