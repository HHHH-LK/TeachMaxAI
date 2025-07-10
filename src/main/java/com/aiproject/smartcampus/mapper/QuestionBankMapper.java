package com.aiproject.smartcampus.mapper;

import com.aiproject.smartcampus.pojo.po.Exam;
import com.aiproject.smartcampus.pojo.po.ExamScore;
import com.aiproject.smartcampus.pojo.po.QuestionBank;
import com.aiproject.smartcampus.pojo.vo.QuestionBankStatisticsVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * @author lk_hhh
 */ // 11. 题库Mapper
@Repository
@Mapper
public interface QuestionBankMapper extends BaseMapper<QuestionBank> {

    /**
     * 查询指定创建者的最大题目ID
     */
    @Select("SELECT MAX(question_id) FROM question_bank WHERE created_by = #{createdBy}")
    Integer selectMaxQuestionIdByCreator(@Param("createdBy") Integer createdBy);


    /**
     * 根据课程查询题目
     */
    @Select("SELECT * FROM question_bank WHERE course_id = #{courseId}")
    List<QuestionBank> findByCourseId(@Param("courseId") Integer courseId);
    
    /**
     * 根据知识点查询题目
     */
    @Select("SELECT * FROM question_bank WHERE point_id = #{pointId}")
    List<QuestionBank> findByPointId(@Param("pointId") Integer pointId);
    
    /**
     * 根据题型查询题目
     */
    @Select("SELECT * FROM question_bank WHERE course_id = #{courseId} AND question_type = #{questionType}")
    List<QuestionBank> findByTypeAndCourse(@Param("courseId") Integer courseId, @Param("questionType") String questionType);
    
    /**
     * 根据难度查询题目
     */
    @Select("SELECT * FROM question_bank WHERE course_id = #{courseId} AND difficulty_level = #{difficultyLevel}")
    List<QuestionBank> findByDifficultyAndCourse(@Param("courseId") Integer courseId, @Param("difficultyLevel") String difficultyLevel);
    
    /**
     * 随机获取题目
     */
    @Select("SELECT * FROM question_bank WHERE course_id = #{courseId} AND question_type = #{questionType} ORDER BY RAND() LIMIT #{count}")
    List<QuestionBank> findRandomQuestions(@Param("courseId") Integer courseId, @Param("questionType") String questionType, @Param("count") Integer count);
    
    /**
     * 题库统计
     */
    @Select("SELECT question_type, difficulty_level, COUNT(*) as count FROM question_bank " +
            "WHERE course_id = #{courseId} GROUP BY question_type, difficulty_level")
    List<Map<String, Object>> getQuestionStatistics(@Param("courseId") Integer courseId);

    /**
     * 获取题库详细统计信息（用于智能试卷创建）
     */
    @Select("SELECT " +
            "COUNT(*) as total_questions, " +
            "COUNT(CASE WHEN question_type = 'single_choice' THEN 1 END) as single_choice_count, " +
            "COUNT(CASE WHEN question_type = 'multiple_choice' THEN 1 END) as multiple_choice_count, " +
            "COUNT(CASE WHEN question_type = 'true_false' THEN 1 END) as true_false_count, " +
            "COUNT(CASE WHEN question_type = 'fill_blank' THEN 1 END) as fill_blank_count, " +
            "COUNT(CASE WHEN question_type = 'short_answer' THEN 1 END) as short_answer_count, " +
            "COUNT(CASE WHEN difficulty_level = 'easy' THEN 1 END) as easy_count, " +
            "COUNT(CASE WHEN difficulty_level = 'medium' THEN 1 END) as medium_count, " +
            "COUNT(CASE WHEN difficulty_level = 'hard' THEN 1 END) as hard_count " +
            "FROM question_bank WHERE course_id = #{courseId}")
    QuestionBankStatisticsVO getQuestionBankStatistics(@Param("courseId") Integer courseId);

    /**
     * 根据多个条件查询题目（用于智能选题）
     */
    List<QuestionBank> findQuestionsByCondition(@Param("courseId") Integer courseId,
                                                @Param("questionType") String questionType,
                                                @Param("difficultyLevel") String difficultyLevel,
                                                @Param("pointIds") List<Integer> pointIds,
                                                @Param("limit") Integer limit);

    /**
     * 智能选择题目（综合考虑难度分布和知识点覆盖）
     */
    List<QuestionBank> selectSmartQuestions(@Param("courseId") Integer courseId,
                                            @Param("questionType") String questionType,
                                            @Param("count") Integer count,
                                            @Param("easyRatio") Double easyRatio,
                                            @Param("mediumRatio") Double mediumRatio,
                                            @Param("hardRatio") Double hardRatio);

}