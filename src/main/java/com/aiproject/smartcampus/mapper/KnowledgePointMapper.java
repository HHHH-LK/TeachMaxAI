package com.aiproject.smartcampus.mapper;

import com.aiproject.smartcampus.pojo.bo.classprase.Course;
import com.aiproject.smartcampus.pojo.po.Exam;
import com.aiproject.smartcampus.pojo.po.ExamScore;
import com.aiproject.smartcampus.pojo.po.KnowledgePoint;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;


// 10. 知识点Mapper
@Repository
@Mapper
public interface KnowledgePointMapper extends BaseMapper<KnowledgePoint> {
    
    /**
     * 查询课程知识点树
     */
    @Select("SELECT * FROM knowledge_points WHERE course_id = #{courseId} ORDER BY parent_point_id, point_id")
    List<KnowledgePoint> findByCourseId(@Param("courseId") Integer courseId);
    
    /**
     * 查询根级知识点
     */
    @Select("SELECT * FROM knowledge_points WHERE course_id = #{courseId} AND parent_point_id IS NULL")
    List<KnowledgePoint> findRootPoints(@Param("courseId") Integer courseId);
    
    /**
     * 查询子知识点
     */
    @Select("SELECT * FROM knowledge_points WHERE parent_point_id = #{parentId}")
    List<KnowledgePoint> findChildPoints(@Param("parentId") Integer parentId);
    
    /**
     * 根据难度查询知识点
     */
    @Select("SELECT * FROM knowledge_points WHERE course_id = #{courseId} AND difficulty_level = #{difficultyLevel}")
    List<KnowledgePoint> findByDifficulty(@Param("courseId") Integer courseId, @Param("difficultyLevel") String difficultyLevel);
}
