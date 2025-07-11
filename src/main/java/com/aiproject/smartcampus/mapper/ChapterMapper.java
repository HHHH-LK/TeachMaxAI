package com.aiproject.smartcampus.mapper;

import com.aiproject.smartcampus.pojo.dto.ChapterKnowledgePointDTO;
import com.aiproject.smartcampus.pojo.po.*;
import com.aiproject.smartcampus.pojo.vo.*;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import dev.langchain4j.agent.tool.P;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.util.List;

// 1. 章节表Mapper
@Mapper
@Repository
public interface ChapterMapper extends BaseMapper<Chapter> {

    /**
     * 根据课程ID查询所有章节，按顺序排列
     */
    @Select("SELECT * FROM chapters WHERE course_id = #{courseId} AND status = 'active' ORDER BY chapter_order")
    List<Chapter> getChaptersByCourseId(@Param("courseId") Integer courseId);

    /**
     * 根据课程ID获取章节数量
     */
    @Select("SELECT COUNT(*) FROM chapters WHERE course_id = #{courseId}")
    Integer getChapterCountByCourseId(@Param("courseId") Integer courseId);

    /**
     * 获取指定课程的下一个章节顺序号
     */
    @Select("SELECT COALESCE(MAX(chapter_order), 0) + 1 FROM chapters WHERE course_id = #{courseId}")
    Integer getNextChapterOrder(@Param("courseId") Integer courseId);

    /**
     * 根据难度等级查询章节
     */
    @Select("SELECT * FROM chapters WHERE difficulty_level = #{difficultyLevel} ORDER BY course_id, chapter_order")
    List<Chapter> getChaptersByDifficulty(@Param("difficultyLevel") String difficultyLevel);

    /**
     * 查询某课程指定顺序范围的章节
     */
    @Select("SELECT * FROM chapters WHERE course_id = #{courseId} AND chapter_order BETWEEN #{startOrder} AND #{endOrder} ORDER BY chapter_order")
    List<Chapter> getChaptersByOrderRange(@Param("courseId") Integer courseId,
                                          @Param("startOrder") Integer startOrder,
                                          @Param("endOrder") Integer endOrder);


    @Select("SELECT\n" +
            "    ch.chapter_id,\n" +
            "    ch.course_id,\n" +
            "    ch.chapter_name,\n" +
            "    ch.chapter_order,\n" +
            "    ch.description,\n" +
            "    ch.difficulty_level,\n" +
            "    ch.created_at,\n" +
            "    ch.updated_at,\n" +
            "    -- 课程信息\n" +
            "    c.course_name,\n" +
            "    c.semester,\n" +
            "    -- 学生学习进度信息\n" +
            "    COALESCE(scp.progress_rate, 0.00) AS progress_rate,\n" +
            "    COALESCE(scp.mastery_level, 'not_started') AS student_mastery_level,\n" +
            "    COALESCE(scp.study_time, 0) AS student_study_time,\n" +
            "    scp.last_study_at AS student_last_study_at,\n" +
            "    scp.current_material_id,\n" +
            "    scp.completed_materials,\n" +
            "    -- 章节统计\n" +
            "    COUNT(DISTINCT ckp.point_id) AS total_knowledge_points,\n" +
            "    COUNT(DISTINCT cm.material_id) AS total_materials,\n" +
            "    COUNT(CASE WHEN ckp.is_core = 1 THEN 1 END) AS core_knowledge_points\n" +
            "FROM chapters ch\n" +
            "JOIN courses c ON ch.course_id = c.course_id\n" +
            "LEFT JOIN student_chapter_progress scp ON (ch.chapter_id = scp.chapter_id AND scp.student_id = #{studentId})\n" +
            "LEFT JOIN chapter_knowledge_points ckp ON ch.chapter_id = ckp.chapter_id\n" +
            "LEFT JOIN course_materials cm ON ch.chapter_id = cm.chapter_id\n" +
            "WHERE ch.course_id = #{courseId}\n" +
            "GROUP BY ch.chapter_id, ch.course_id, ch.chapter_name, ch.chapter_order,\n" +
            "         ch.description, ch.difficulty_level, ch.created_at, ch.updated_at,\n" +
            "         c.course_name, c.semester, scp.progress_rate, scp.mastery_level,\n" +
            "         scp.study_time, scp.last_study_at, scp.current_material_id, scp.completed_materials\n" +
            "ORDER BY ch.chapter_order ASC;")
    List<CourseChapterVO> getAllChapterByCourseId(@Param(value = "courseId") String courseId, @Param(value = "studentId") String studentId);


    @Select("SELECT\n" +
            "    kp.point_id,\n" +
            "    kp.point_name,\n" +
            "    kp.description,\n" +
            "    kp.difficulty_level,\n" +
            "    kp.keywords,\n" +
            "    ckp.point_order,\n" +
            "    ckp.is_core,\n" +
            "    ckp.created_at as relation_created_at\n" +
            "FROM knowledge_points kp\n" +
            "INNER JOIN chapter_knowledge_points ckp ON kp.point_id = ckp.point_id\n" +
            "WHERE ckp.chapter_id = #{chaptId} " +
            "ORDER BY ckp.point_order ASC;")
    List<ChapterKnowledgePointVO> getAllKnowledgeByChapterId(@Param(value = "chaptId") String chaptId);

    @Select("-- 查询章节ID、学生ID和课程ID对应的所有课程资料\n" +
            "SELECT \n" +
            "    cm.material_id,\n" +
            "    cm.material_title,\n" +
            "    cm.material_description,\n" +
            "    cm.material_type,\n" +
            "    CASE \n" +
            "        WHEN cm.material_type = 'courseware' THEN '课件'\n" +
            "        WHEN cm.material_type = 'video' THEN '视频'\n" +
            "        WHEN cm.material_type = 'document' THEN '文档'\n" +
            "        WHEN cm.material_type = 'link' THEN '链接'\n" +
            "        WHEN cm.material_type = 'exercise' THEN '练习'\n" +
            "        WHEN cm.material_type = 'reference' THEN '参考资料'\n" +
            "        WHEN cm.material_type = 'supplement' THEN '补充资料'\n" +
            "        ELSE cm.material_type\n" +
            "    END as material_type_cn\n" +
            "FROM course_materials cm\n" +
            "INNER JOIN chapters ch ON cm.chapter_id = ch.chapter_id\n" +
            "INNER JOIN courses c ON ch.course_id = c.course_id\n" +
            "INNER JOIN course_enrollments ce ON c.course_id = ce.course_id\n" +
            "WHERE ch.chapter_id = #{chapterId}\n" +
            "  AND ce.student_id = #{studentId}\n" +
            "  AND c.course_id = #{courseId}\n" +
            "  AND cm.status = 'active'\n" +
            "ORDER BY cm.material_type, cm.created_at DESC;")
    List<KnowledgePointMaterialSimpleVO> selectMaterialsByChapterStudentCourse(
            @Param("chapterId") String chapterId,
            @Param("studentId") String studentId,
            @Param("courseId") String courseId
    );


    @Select("-- 方法1：基础权限控制（推荐）- 保持原有字段结构\n" +
            "-- 确保学生已选修该课程才能查看资料\n" +
            "SELECT\n" +
            "    cm.material_id,\n" +
            "    cm.material_title,\n" +
            "    cm.material_description,\n" +
            "    cm.material_type,\n" +
            "    cm.external_resource_url,\n" +
            "    cm.estimated_time,\n" +
            "    cm.difficulty_level,\n" +
            "    cm.is_downloadable,\n" +
            "    cm.tags,\n" +
            "    cm.material_source,\n" +
            "    cm.created_at,\n" +
            "    cm.updated_at,\n" +
            "    -- 课程信息\n" +
            "    c.course_id,\n" +
            "    c.course_name,\n" +
            "    c.semester,\n" +
            "    -- 章节信息\n" +
            "    ch.chapter_id,\n" +
            "    ch.chapter_name,\n" +
            "    ch.chapter_order,\n" +
            "    -- 课件资源信息\n" +
            "    cr.resource_id as courseware_resource_id,\n" +
            "    cr.file_name,\n" +
            "    cr.file_url,\n" +
            "    cr.file_size_mb,\n" +
            "    cr.resource_type as file_type,\n" +
            "    cr.download_count,\n" +
            "    cr.is_public,\n" +
            "    -- 创建者信息\n" +
            "    u.real_name as creator_name\n" +
            "FROM course_materials cm\n" +
            "INNER JOIN courses c ON cm.course_id = c.course_id\n" +
            "INNER JOIN course_enrollments ce ON c.course_id = ce.course_id  -- 确保学生已选课\n" +
            "LEFT JOIN chapters ch ON cm.chapter_id = ch.chapter_id\n" +
            "LEFT JOIN courseware_resources cr ON cm.courseware_resource_id = cr.resource_id\n" +
            "LEFT JOIN users u ON cm.created_by = u.user_id\n" +
            "WHERE cm.material_id = #{materialId}-- 资料ID参数\n" +
            "  AND ce.student_id = #{studentId}   -- 学生ID参数\n" +
            "  AND cm.status = 'active';")
    MaterialDetailRawVO selectChapterMaterialById(@Param(value = "materialId") String materialId, @Param(value = "studentId") String studentId);

    @Select("")
    List<ChapterQuestionDetailVO> getAllTextByChapterId(@Param(value = "chapterId") String chapterId, @Param(value = "studentId") String studentId);

    @Select("-- 1. 基础查询：获取学生对应章节的所有课程资源\n" +
            "SELECT\n" +
            "    cm.material_id,\n" +
            "    cm.material_title,\n" +
            "    cm.material_description,\n" +
            "    cm.material_type,\n" +
            "    CASE\n" +
            "        WHEN cm.material_type = 'courseware' THEN '课件'\n" +
            "        WHEN cm.material_type = 'video' THEN '视频'\n" +
            "        WHEN cm.material_type = 'document' THEN '文档'\n" +
            "        WHEN cm.material_type = 'link' THEN '链接'\n" +
            "        WHEN cm.material_type = 'exercise' THEN '练习'\n" +
            "        WHEN cm.material_type = 'reference' THEN '参考资料'\n" +
            "        WHEN cm.material_type = 'supplement' THEN '补充资料'\n" +
            "        ELSE cm.material_type\n" +
            "    END as material_type_cn,\n" +
            "    cm.external_resource_url,\n" +
            "    cm.estimated_time,\n" +
            "    cm.difficulty_level,\n" +
            "    CASE\n" +
            "        WHEN cm.difficulty_level = 'easy' THEN '简单'\n" +
            "        WHEN cm.difficulty_level = 'medium' THEN '中等'\n" +
            "        WHEN cm.difficulty_level = 'hard' THEN '困难'\n" +
            "        ELSE cm.difficulty_level\n" +
            "    END as difficulty_level_cn,\n" +
            "    cm.is_downloadable,\n" +
            "    cm.tags,\n" +
            "    cm.created_at,\n" +
            "    -- 课件文件信息\n" +
            "    cr.file_name,\n" +
            "    cr.file_url,\n" +
            "    cr.file_size_mb,\n" +
            "    cr.resource_type as file_type,\n" +
            "    -- 课程和章节信息\n" +
            "    c.course_id,\n" +
            "    c.course_name,\n" +
            "    ch.chapter_id,\n" +
            "    ch.chapter_name,\n" +
            "    ch.chapter_order,\n" +
            "    -- 学生学习进度信息\n" +
            "    CASE\n" +
            "        WHEN JSON_CONTAINS(scp.completed_materials, JSON_QUOTE(CAST(cm.material_id AS CHAR))) THEN 1\n" +
            "        ELSE 0\n" +
            "    END as is_completed,\n" +
            "    scp.current_material_id,\n" +
            "    CASE\n" +
            "        WHEN scp.current_material_id = cm.material_id THEN 1\n" +
            "        ELSE 0\n" +
            "    END as is_current_learning\n" +
            "FROM course_materials cm\n" +
            "INNER JOIN chapters ch ON cm.chapter_id = ch.chapter_id\n" +
            "INNER JOIN courses c ON ch.course_id = c.course_id\n" +
            "INNER JOIN course_enrollments ce ON c.course_id = ce.course_id\n" +
            "LEFT JOIN courseware_resources cr ON cm.courseware_resource_id = cr.resource_id\n" +
            "LEFT JOIN student_chapter_progress scp ON ch.chapter_id = scp.chapter_id AND ce.student_id = scp.student_id\n" +
            "WHERE ce.student_id = #{studentId}      -- 学生ID参数\n" +
            "  AND ch.chapter_id = #{chapterId}      -- 章节ID参数\n" +
            "  AND c.course_id = #{courseId}        -- 课程ID参数\n" +
            "  AND cm.status = 'active'\n" +
            "ORDER BY cm.material_type, cm.created_at DESC;")
    List<StudentChapterResourceVO> getALlFinishMasterial(@Param(value = "studentId") String studentId, @Param(value = "chapterId") String chapter, @Param(value = "courseId") String courseId);


    /**
     * 查询章节学习进度率（Double类型）
     */
    @Select("SELECT scp.progress_rate " +
            "FROM student_chapter_progress scp " +
            "INNER JOIN chapters ch ON scp.chapter_id = ch.chapter_id " +
            "INNER JOIN courses c ON ch.course_id = c.course_id " +
            "INNER JOIN course_enrollments ce ON c.course_id = ce.course_id AND scp.student_id = ce.student_id " +
            "WHERE scp.chapter_id = #{chapterId} " +
            "  AND scp.student_id = #{studentId} " +
            "  AND c.course_id = #{courseId}")
    Double getChapterProgressRateAsDouble(@Param("chapterId") String chapterId, @Param("studentId") String studentId, @Param("courseId") String courseId);


    @Select("-- 简化版错题查询 - 只返回核心信息\n" +
            "SELECT\n" +
            "    sa.student_answer,                    -- 用户答案\n" +
            "    qb.question_content,                  -- 题目内容\n" +
            "    qb.correct_answer,                    -- 正确答案\n" +
            "    qb.explanation,                       -- 题目解析\n" +
            "    kp.point_name,                        -- 知识点名称\n" +
            "    ch.chapter_name,                      -- 章节名称\n" +
            "    qb.difficulty_level,                  -- 题目难度\n" +
            "    sa.answer_id                          -- 用于排序的答题ID\n" +
            "FROM student_answers sa\n" +
            "INNER JOIN question_bank qb ON sa.question_id = qb.question_id\n" +
            "INNER JOIN courses c ON qb.course_id = c.course_id\n" +
            "INNER JOIN course_enrollments ce ON c.course_id = ce.course_id AND sa.student_id = ce.student_id\n" +
            "LEFT JOIN chapter_questions cq ON qb.question_id = cq.question_id\n" +
            "LEFT JOIN chapters ch ON cq.chapter_id = ch.chapter_id\n" +
            "LEFT JOIN knowledge_points kp ON qb.point_id = kp.point_id\n" +
            "WHERE c.course_id = #{courseId}        -- 课程ID参数\n" +
            "  AND sa.student_id = #{studentId}      -- 学生ID参数\n" +
            "  AND sa.is_correct = 0      -- 只查询错误的题目\n" +
            "ORDER BY sa.answer_id DESC;  -- 按答题时间倒序")
    List<WrongQuestionVO> getAllNuCorrectTest(@Param(value = "courseId") String courseId, @Param(value = "studentId") String studentId);


    @Select("SELECT \n" +
            "    qb.question_id,\n" +
            "    qb.question_type,\n" +
            "    -- 题目类型中文描述\n" +
            "    CASE qb.question_type\n" +
            "        WHEN 'single_choice' THEN '单选题'\n" +
            "        WHEN 'multiple_choice' THEN '多选题'\n" +
            "        WHEN 'true_false' THEN '判断题'\n" +
            "        WHEN 'fill_blank' THEN '填空题'\n" +
            "        WHEN 'short_answer' THEN '简答题'\n" +
            "        ELSE '未知类型'\n" +
            "    END as question_type_cn,\n" +
            "    qb.question_content,\n" +
            "    qb.question_options,\n" +
            "    qb.correct_answer,\n" +
            "    qb.explanation,\n" +
            "    qb.difficulty_level,\n" +
            "    -- 难度等级中文描述\n" +
            "    CASE qb.difficulty_level\n" +
            "        WHEN 'easy' THEN '简单'\n" +
            "        WHEN 'medium' THEN '中等'\n" +
            "        WHEN 'hard' THEN '困难'\n" +
            "        ELSE '未设置'\n" +
            "    END as difficulty_level_cn,\n" +
            "    qb.score_points,\n" +
            "    qb.created_at,\n" +
            "    cq.question_type as chapter_question_type,\n" +
            "    -- 章节题目类型中文描述\n" +
            "    CASE cq.question_type\n" +
            "        WHEN 'practice' THEN '练习题'\n" +
            "        WHEN 'test' THEN '测试题'\n" +
            "        WHEN 'exam' THEN '考试题'\n" +
            "        ELSE '未知类型'\n" +
            "    END as chapter_question_type_cn,\n" +
            "    -- 权重字段（可根据实际需求调整计算逻辑）\n" +
            "    CASE qb.difficulty_level\n" +
            "        WHEN 'easy' THEN 1\n" +
            "        WHEN 'medium' THEN 2\n" +
            "        WHEN 'hard' THEN 3\n" +
            "        ELSE 1\n" +
            "    END as t\n" +
            "FROM education_system.question_bank qb\n" +
            "INNER JOIN education_system.chapter_questions cq ON qb.question_id = cq.question_id\n" +
            "WHERE cq.chapter_id = #{chapterId}          -- 章节ID参数\n" +
            "    AND qb.course_id = #{courseId}        -- 课程ID参数\n" +
            "    AND cq.question_type = #{type}  -- 只查询练习题\n" +
            "ORDER BY qb.difficulty_level,   -- 按难度排序\n" +
            "         qb.question_id;        -- 按题目ID排序")
    List<ChapterQuestionDetailVO> selectTestByType(@Param("chapterId") String chapterId
            , @Param("courseId") String courseId
            , @Param("type") String type);


    @Select("     SELECT\n" +
            "        c.course_id,\n" +
            "        c.chapter_id,\n" +
            "         c.chapter_name,\n" +
            "         c.chapter_order,\n" +
            "         c.difficulty_level as chapter_difficulty,\n" +
            "         kp.point_id,\n" +
            "         kp.point_name,\n" +
            "         kp.description as point_description,\n" +
            "         kp.difficulty_level as point_difficulty,\n" +
            "         kp.keywords,\n" +
            "         ckp.point_order,\n" +
            "         ckp.is_core,\n" +
            "         CASE WHEN ckp.is_core = 1 THEN '重点' ELSE '一般' END as point_importance,\n" +
            "         ckp.created_at as relation_created_at\n" +
            "     FROM chapters c\n" +
            "     INNER JOIN chapter_knowledge_points ckp ON c.chapter_id = ckp.chapter_id\n" +
            "     INNER JOIN knowledge_points kp ON ckp.point_id = kp.point_id\n" +
            "     WHERE c.course_id = #{courseId}\n" +
            "       AND c.chapter_id = #{chapterId}\n" +
            "     ORDER BY ckp.is_core DESC, ckp.point_order ASC, kp.point_name ASC;")
    List<ChapterKnowledgePointDTO> getallChapterKnowleageByIscore(@Param("courseId") String courseId, @Param("chapterId") String chapterId);

    @Select("select chapter_name\n" +
            "from chapters\n" +
            "where chapter_id=#{chapterId}")
    String getChapterNameById(String chapterId);
}