package com.aiproject.smartcampus.mapper;

import com.aiproject.smartcampus.pojo.bo.classprase.Course;
import com.aiproject.smartcampus.pojo.po.Exam;
import com.aiproject.smartcampus.pojo.po.ExamScore;
import com.aiproject.smartcampus.pojo.po.LearningStatistics;
import com.aiproject.smartcampus.pojo.po.Teacher;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * @author lk_hhh
 */ // 14. 学习统计Mapper
@Repository
@Mapper
public interface LearningStatisticsMapper extends BaseMapper<LearningStatistics> {
    
    /**
     * 查询用户学习统计
     */
    @Select("SELECT * FROM learning_statistics WHERE user_id = #{userId} AND date BETWEEN #{startDate} AND #{endDate}")
    List<LearningStatistics> findByUserAndDateRange(@Param("userId") Integer userId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
    
    /**
     * 查询今日学习统计
     */
    @Select("SELECT * FROM learning_statistics WHERE user_id = #{userId} AND date = #{date}")
    LearningStatistics findByUserAndDate(@Param("userId") Integer userId, @Param("date") LocalDate date);
    
    /**
     * 学习时长统计
     */
    @Select("SELECT " +
            "SUM(study_duration_minutes) as total_minutes, " +
            "AVG(study_duration_minutes) as avg_minutes, " +
            "COUNT(*) as study_days " +
            "FROM learning_statistics " +
            "WHERE user_id = #{userId} AND date BETWEEN #{startDate} AND #{endDate}")
    Map<String, Object> getStudyTimeStatistics(@Param("userId") Integer userId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
    
    /**
     * 班级学习统计排行
     */
    @Select("SELECT u.real_name, SUM(ls.study_duration_minutes) as total_minutes " +
            "FROM learning_statistics ls " +
            "LEFT JOIN users u ON ls.user_id = u.user_id " +
            "LEFT JOIN students s ON u.user_id = s.user_id " +
            "WHERE s.class_name = #{className} AND ls.date BETWEEN #{startDate} AND #{endDate} " +
            "GROUP BY ls.user_id ORDER BY total_minutes DESC LIMIT #{limit}")
    List<Map<String, Object>> getClassStudyRanking(@Param("className") String className, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate, @Param("limit") Integer limit);
}