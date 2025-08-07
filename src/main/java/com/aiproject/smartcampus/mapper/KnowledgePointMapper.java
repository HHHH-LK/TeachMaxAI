package com.aiproject.smartcampus.mapper;

import com.aiproject.smartcampus.pojo.bo.SimpleKnowledgeAnalysisBO;
import com.aiproject.smartcampus.pojo.bo.StudentKnowBO;
import com.aiproject.smartcampus.pojo.bo.StudentWrongKnowledgeBO;
import com.aiproject.smartcampus.pojo.po.KnowledgePoint;

import com.aiproject.smartcampus.pojo.vo.KnowledgePointSimpleVO;
import com.aiproject.smartcampus.pojo.vo.KnowledgePointVO;
import com.aiproject.smartcampus.pojo.vo.StudentKnowledgePointVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
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
            "    sa.student_id,\n" +
            "    COUNT(CASE WHEN sa.is_correct = 0 THEN 1 END) as wrong_answer_count,\n" +
            "    COUNT(*) as total_answer_count,\n" +
            "    ROUND(AVG(CASE WHEN sa.is_correct = 1 THEN 1 ELSE 0 END) * 100, 2) as accuracy_rate\n" +
            "FROM student_answers sa\n" +
            "INNER JOIN question_bank qb ON sa.question_id = qb.question_id\n" +
            "INNER JOIN knowledge_points kp ON qb.point_id = kp.point_id\n" +
            "INNER JOIN courses c ON kp.course_id = c.course_id\n" +
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
            "    -- 章节ID\n" +
            "    ckp.chapter_id,\n" +
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
            "LEFT JOIN chapter_knowledge_points ckp ON kp.point_id = ckp.point_id\n" +
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
            "    WHERE sa.student_id = #{studentId}\n" +
            "    GROUP BY qb.point_id\n" +
            ") answer_stats ON kp.point_id = answer_stats.point_id\n" +
            "\n" +
            "WHERE kp.point_id = #{pointId}")
    KnowledgePointSimpleVO getKnowledgeInformationByPointId(@Param(value = "pointId") String pointId, @Param(value = "studentId") String studentId);

    List<SimpleKnowledgeAnalysisBO> getSimpleKnowledgeAnalysis(@Param(value = "pointIds") List<String> pointIds, @Param(value = "studentId") String studentId);


    @Select("SELECT\n" +
            "    kp.point_id,\n" +
            "    kp.point_name,\n" +
            "    kp.description,\n" +
            "    kp.difficulty_level,\n" +
            "    kp.keywords,\n" +
            "    c.course_id,\n" +
            "    c.course_name,\n" +
            "    -- 学生掌握情况\n" +
            "    COALESCE(skm.mastery_level, 'not_learned') AS mastery_level,\n" +
            "    COALESCE(skm.practice_score, 0) AS practice_score,\n" +
            "    COALESCE(skm.practice_count, 0) AS practice_count,\n" +
            "    skm.last_updated AS mastery_last_updated,\n" +
            "    -- 父级知识点信息\n" +
            "    parent_kp.point_name AS parent_point_name,\n" +
            "    parent_kp.point_id AS parent_point_id,\n" +
            "    -- 章节信息\n" +
            "    ch.chapter_id,\n" +
            "    ch.chapter_name,\n" +
            "    ch.chapter_order,\n" +
            "    ch.difficulty_level AS chapter_difficulty,\n" +
            "    ckp.point_order AS point_order_in_chapter,\n" +
            "    ckp.is_core AS is_core_point,\n" +
            "    -- 章节学习进度\n" +
            "    scp.progress_rate AS chapter_progress_rate,\n" +
            "    scp.mastery_level AS chapter_mastery_level,\n" +
            "    scp.study_time AS chapter_study_time,\n" +
            "    scp.last_study_at AS chapter_last_study_at\n" +
            "FROM knowledge_points kp\n" +
            "JOIN courses c ON kp.course_id = c.course_id\n" +
            "-- 学生选课信息\n" +
            "JOIN course_enrollments ce ON c.course_id = ce.course_id\n" +
            "-- 左连接学生知识掌握表\n" +
            "LEFT JOIN student_knowledge_mastery skm ON (kp.point_id = skm.point_id AND ce.student_id = skm.student_id)\n" +
            "-- 左连接父级知识点\n" +
            "LEFT JOIN knowledge_points parent_kp ON kp.parent_point_id = parent_kp.point_id\n" +
            "-- 左连接章节知识点关系\n" +
            "LEFT JOIN chapter_knowledge_points ckp ON kp.point_id = ckp.point_id\n" +
            "-- 左连接章节信息\n" +
            "LEFT JOIN chapters ch ON ckp.chapter_id = ch.chapter_id\n" +
            "-- 左连接学生章节进度\n" +
            "LEFT JOIN student_chapter_progress scp ON (ch.chapter_id = scp.chapter_id AND ce.student_id = scp.student_id)\n" +
            "WHERE ce.student_id = #{studentId} AND c.course_id = #{courseId}\n" +
            "ORDER BY c.course_id, ch.chapter_order, ckp.point_order, kp.point_id;")
    List<StudentKnowledgePointVO> selectKnowledgePointByStudentIdAndCourseId(
            @Param("studentId") String studentId,
            @Param("courseId") String courseId
    );

    // 方式2：courseId可选（如果你需要兼容原有方法）
    @Select("<script>" +
            "SELECT\n" +
            "    kp.point_id,\n" +
            "    kp.point_name,\n" +
            "    kp.description,\n" +
            "    kp.difficulty_level,\n" +
            "    kp.keywords,\n" +
            "    c.course_id,\n" +
            "    c.course_name,\n" +
            "    -- 学生掌握情况\n" +
            "    COALESCE(skm.mastery_level, 'not_learned') AS mastery_level,\n" +
            "    COALESCE(skm.practice_score, 0) AS practice_score,\n" +
            "    COALESCE(skm.practice_count, 0) AS practice_count,\n" +
            "    skm.last_updated AS mastery_last_updated,\n" +
            "    -- 父级知识点信息\n" +
            "    parent_kp.point_name AS parent_point_name,\n" +
            "    parent_kp.point_id AS parent_point_id,\n" +
            "    -- 章节信息\n" +
            "    ch.chapter_id,\n" +
            "    ch.chapter_name,\n" +
            "    ch.chapter_order,\n" +
            "    ch.difficulty_level AS chapter_difficulty,\n" +
            "    ckp.point_order AS point_order_in_chapter,\n" +
            "    ckp.is_core AS is_core_point,\n" +
            "    -- 章节学习进度\n" +
            "    scp.progress_rate AS chapter_progress_rate,\n" +
            "    scp.mastery_level AS chapter_mastery_level,\n" +
            "    scp.study_time AS chapter_study_time,\n" +
            "    scp.last_study_at AS chapter_last_study_at\n" +
            "FROM knowledge_points kp\n" +
            "JOIN courses c ON kp.course_id = c.course_id\n" +
            "-- 学生选课信息\n" +
            "JOIN course_enrollments ce ON c.course_id = ce.course_id\n" +
            "-- 左连接学生知识掌握表\n" +
            "LEFT JOIN student_knowledge_mastery skm ON (kp.point_id = skm.point_id AND ce.student_id = skm.student_id)\n" +
            "-- 左连接父级知识点\n" +
            "LEFT JOIN knowledge_points parent_kp ON kp.parent_point_id = parent_kp.point_id\n" +
            "-- 左连接章节知识点关系\n" +
            "LEFT JOIN chapter_knowledge_points ckp ON kp.point_id = ckp.point_id\n" +
            "-- 左连接章节信息\n" +
            "LEFT JOIN chapters ch ON ckp.chapter_id = ch.chapter_id\n" +
            "-- 左连接学生章节进度\n" +
            "LEFT JOIN student_chapter_progress scp ON (ch.chapter_id = scp.chapter_id AND ce.student_id = scp.student_id)\n" +
            "WHERE ce.student_id = #{studentId}\n" +
            "<if test='courseId != null and courseId != \"\"'>\n" +
            "    AND c.course_id = #{courseId}\n" +
            "</if>\n" +
            "ORDER BY c.course_id, ch.chapter_order, ckp.point_order, kp.point_id;" +
            "</script>")
    List<StudentKnowledgePointVO> selectKnowledgePointByStudentIdOptionalCourseId(
            @Param("studentId") String studentId,
            @Param("courseId") String courseId
    );

    /**
     * 更新学生知识掌握情况
     *
     * @param studentId     学生ID
     * @param pointId       知识点ID
     * @param masteryLevel  掌握程度 (not_learned: 未学习, learning: 学习中, mastered: 已掌握)
     * @param practiceScore 练习得分/准确率
     * @param practiceCount 练习次数增量
     */
    void updateStudentKnowledgeMastery(@Param("studentId") int studentId,
                                       @Param("pointId") Integer pointId,
                                       @Param("masteryLevel") String masteryLevel,
                                       @Param("practiceScore") BigDecimal practiceScore,
                                       @Param("practiceCount") int practiceCount);


    /**
     * 获取课程知识点列表（用于智能试卷创建）
     */
    @Select("SELECT point_id, point_name, description, difficulty_level, keywords " +
            "FROM knowledge_points WHERE course_id = #{courseId} ORDER BY point_id")
    List<KnowledgePointVO> getCourseKnowledgePoints(@Param("courseId") Integer courseId);

    /**
     * 根据名称和课程查找知识点
     */
    @Select("SELECT point_id FROM knowledge_points " +
            "WHERE point_name = #{pointName} AND course_id = #{courseId} LIMIT 1")
    Integer findByNameAndCourse(@Param("pointName") String pointName,
                                @Param("courseId") Integer courseId);

    /**
     * 创建新知识点（AI生成题目时可能需要）
     */
    @Insert("INSERT INTO knowledge_points (course_id, point_name, description, difficulty_level, keywords) " +
            "VALUES (#{courseId}, #{pointName}, #{description}, 'medium', #{keywords})")
    @Options(useGeneratedKeys = true, keyProperty = "pointId")
    int createKnowledgePoint(@Param("pointName") String pointName,
                             @Param("courseId") Integer courseId,
                             @Param("description") String description,
                             @Param("keywords") String keywords);


    @Select("select knowledge_points.point_name from knowledge_points where point_id =#{pointId}")
    String getPonintNameById(String pointId);

    @Select("SELECT * FROM knowledge_points WHERE course_id = #{courseId}")
    List<KnowledgePoint> findByCourseId(@Param("courseId") String courseId);
}
