package com.aiproject.smartcampus.mapper;

import com.aiproject.smartcampus.pojo.dto.ChapterMaterialStats;
import com.aiproject.smartcampus.pojo.dto.CourseMaterialStats;
import com.aiproject.smartcampus.pojo.po.ChapterKnowledgePoint;
import com.aiproject.smartcampus.pojo.po.CourseMaterial;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.util.List;

// 2. 章节知识点关系表Mapper
@Mapper
@Repository
public interface ChapterKnowledgePointMapper extends BaseMapper<ChapterKnowledgePoint> {
    
    /**
     * 根据章节ID查询所有知识点关系，按顺序排列
     */
    @Select("SELECT * FROM chapter_knowledge_points WHERE chapter_id = #{chapterId} ORDER BY point_order")
    List<ChapterKnowledgePoint> getKnowledgePointsByChapterId(@Param("chapterId") Integer chapterId);
    
    /**
     * 根据知识点ID查询所属章节
     */
    @Select("SELECT * FROM chapter_knowledge_points WHERE point_id = #{pointId}")
    List<ChapterKnowledgePoint> getChaptersByPointId(@Param("pointId") Integer pointId);
    
    /**
     * 查询章节中的核心知识点
     */
    @Select("SELECT * FROM chapter_knowledge_points WHERE chapter_id = #{chapterId} AND is_core = 1 ORDER BY point_order")
    List<ChapterKnowledgePoint> getCoreKnowledgePointsByChapterId(@Param("chapterId") Integer chapterId);
    
    /**
     * 获取章节的知识点数量
     */
    @Select("SELECT COUNT(*) FROM chapter_knowledge_points WHERE chapter_id = #{chapterId}")
    Integer getKnowledgePointCountByChapterId(@Param("chapterId") Integer chapterId);
    
    /**
     * 获取知识点在章节中的下一个顺序号
     */
    @Select("SELECT COALESCE(MAX(point_order), 0) + 1 FROM chapter_knowledge_points WHERE chapter_id = #{chapterId}")
    Integer getNextPointOrder(@Param("chapterId") Integer chapterId);
    
    /**
     * 批量插入章节知识点关系
     */
    Integer insertBatch(@Param("list") List<ChapterKnowledgePoint> list);
    
    /**
     * 根据章节ID删除所有知识点关系
     */
    Integer deleteByChapterId(@Param("chapterId") Integer chapterId);
    
    /**
     * 查询某课程下所有章节的知识点关系
     */
    @Select("SELECT ckp.* FROM chapter_knowledge_points ckp " +
            "INNER JOIN chapters c ON ckp.chapter_id = c.chapter_id " +
            "WHERE c.course_id = #{courseId} ORDER BY c.chapter_order, ckp.point_order")
    List<ChapterKnowledgePoint> getKnowledgePointsByCourseId(@Param("courseId") Integer courseId);
}