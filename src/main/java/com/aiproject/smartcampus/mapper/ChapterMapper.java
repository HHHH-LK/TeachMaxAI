package com.aiproject.smartcampus.mapper;

import com.aiproject.smartcampus.pojo.po.*;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
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
}