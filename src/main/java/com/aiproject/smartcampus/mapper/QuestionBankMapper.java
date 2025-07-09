package com.aiproject.smartcampus.mapper;

import com.aiproject.smartcampus.pojo.po.Exam;
import com.aiproject.smartcampus.pojo.po.ExamScore;
import com.aiproject.smartcampus.pojo.po.QuestionBank;
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
}