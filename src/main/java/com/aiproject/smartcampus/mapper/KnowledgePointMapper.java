package com.aiproject.smartcampus.mapper;

import com.aiproject.smartcampus.pojo.bo.SimpleKnowledgeAnalysisBO;
import com.aiproject.smartcampus.pojo.bo.StudentWrongKnowledgeBO;
import com.aiproject.smartcampus.pojo.po.KnowledgePoint;

import com.aiproject.smartcampus.pojo.vo.KnowledgePointSimpleVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import dev.langchain4j.agent.tool.P;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author lk_hhh
 */
@Repository
@Mapper
public interface KnowledgePointMapper extends BaseMapper<KnowledgePoint> {

    @Select("SELECT\n" +
            "    kp.point_id,\n" +
            "    kp.point_name,\n" +
            "    kp.description,\n" +
            "    kp.difficulty_level,\n" +
            "    kp.keywords,\n" +
            "    c.course_name,\n" +
            "    COUNT(sa.answer_id) as wrong_answer_count,\n" +
            "    ROUND(AVG(CASE WHEN sa.is_correct = 1 THEN 1 ELSE 0 END) * 100, 2) as accuracy_rate\n" +
            "FROM student_answers sa\n" +
            "INNER JOIN question_bank qb ON sa.question_id = qb.question_id\n" +
            "INNER JOIN knowledge_points kp ON qb.point_id = kp.point_id\n" +
            "INNER JOIN courses c ON kp.course_id = c.course_id\n" +
            "WHERE sa.student_id =#{studentId}\n" +
            "  AND sa.is_correct = 0\n" +
            "GROUP BY kp.point_id, kp.point_name, kp.description, kp.difficulty_level, kp.keywords, c.course_name\n" +
            "ORDER BY wrong_answer_count DESC, kp.difficulty_level DESC;")
    List<StudentWrongKnowledgeBO> getStudentWrongKnowledgeByStudentId(@Param(value = "studentId") String studentId);


    @Select("SELECT\n" +
            "    -- 基础信息\n" +
            "    kp.point_id,\n" +
            "    kp.point_name,\n" +
            "    kp.description,\n" +
            "    kp.difficulty_level,\n" +
            "    kp.keywords,\n" +
            "\n" +
            "    -- 课程信息\n" +
            "    c.course_id,\n" +
            "    c.course_name,\n" +
            "    c.semester,\n" +
            "\n" +
            "    -- 学生掌握情况\n" +
            "    COALESCE(skm.mastery_level, 'not_learned') as mastery_level,\n" +
            "    COALESCE(skm.practice_score, 0.0) as practice_score,\n" +
            "    COALESCE(skm.practice_count, 0) as practice_count,\n" +
            "    skm.last_updated as mastery_last_updated,\n" +
            "\n" +
            "    -- 答题统计\n" +
            "    COALESCE(answer_stats.total_answered, 0) as total_answered,\n" +
            "    COALESCE(answer_stats.correct_count, 0) as correct_count,\n" +
            "    COALESCE(answer_stats.wrong_count, 0) as wrong_count,\n" +
            "    COALESCE(answer_stats.accuracy_rate, 0.0) as accuracy_rate\n" +
            "\n" +
            "FROM knowledge_points kp\n" +
            "INNER JOIN courses c ON kp.course_id = c.course_id\n" +
            "LEFT JOIN student_knowledge_mastery skm ON kp.point_id = skm.point_id\n" +
            "    AND skm.student_id = #{studentId}\n" +
            "LEFT JOIN (\n" +
            "    SELECT\n" +
            "        qb.point_id,\n" +
            "        COUNT(*) as total_answered,\n" +
            "        SUM(CASE WHEN sa.is_correct = 1 THEN 1 ELSE 0 END) as correct_count,\n" +
            "        SUM(CASE WHEN sa.is_correct = 0 THEN 1 ELSE 0 END) as wrong_count,\n" +
            "        ROUND(AVG(CASE WHEN sa.is_correct = 1 THEN 1 ELSE 0 END) * 100, 2) as accuracy_rate\n" +
            "    FROM student_answers sa\n" +
            "    INNER JOIN question_bank qb ON sa.question_id = qb.question_id\n" +
            "    WHERE sa.student_id =  #{studentId}\n" +
            "    GROUP BY qb.point_id\n" +
            ") answer_stats ON kp.point_id = answer_stats.point_id\n" +
            "\n" +
            "WHERE kp.point_id =#{pointId}")
    KnowledgePointSimpleVO getKnowledgeInformationByPointId(@Param(value = "pointId") String pointId, @Param(value = "studentId") String studentId);


    List<SimpleKnowledgeAnalysisBO> getSimpleKnowledgeAnalysis(@Param(value = "pointIds") List<String> pointIds, @Param(value = "studentId") String studentId);

}