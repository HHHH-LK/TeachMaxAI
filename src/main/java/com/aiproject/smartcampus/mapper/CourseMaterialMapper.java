package com.aiproject.smartcampus.mapper;

import com.aiproject.smartcampus.pojo.dto.ChapterMaterialStats;
import com.aiproject.smartcampus.pojo.dto.CourseMaterialStats;
import com.aiproject.smartcampus.pojo.po.CourseMaterial;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.util.List;

// 5. 课程资料表Mapper
@Mapper
@Repository
public interface CourseMaterialMapper extends BaseMapper<CourseMaterial> {

    /**
     * 根据课程ID查询所有资料，按显示顺序排列
     */
    @Select("SELECT * FROM course_materials WHERE course_id = #{courseId} AND status = 'active' ORDER BY chapter_id, display_order")
    List<CourseMaterial> getMaterialsByCourseId(@Param("courseId") Integer courseId);

    /**
     * 根据章节ID查询资料
     */
    @Select("SELECT * FROM course_materials WHERE chapter_id = #{chapterId} AND status = 'active' ORDER BY display_order")
    List<CourseMaterial> getMaterialsByChapterId(@Param("chapterId") Integer chapterId);

    /**
     * 根据资料类型查询
     */
    @Select("SELECT * FROM course_materials WHERE course_id = #{courseId} AND material_type = #{materialType} AND status = 'active' ORDER BY chapter_id, display_order")
    List<CourseMaterial> getMaterialsByType(@Param("courseId") Integer courseId,
                                            @Param("materialType") String materialType);

    /**
     * 根据资料来源查询
     */
    @Select("SELECT * FROM course_materials WHERE course_id = #{courseId} AND material_source = #{materialSource} AND status = 'active' ORDER BY chapter_id, display_order")
    List<CourseMaterial> getMaterialsBySource(@Param("courseId") Integer courseId,
                                              @Param("materialSource") String materialSource);

    /**
     * 查询课件类型的资料（关联courseware_resources）
     */
    @Select("SELECT cm.*, cr.file_url, cr.file_size_mb " +
            "FROM course_materials cm " +
            "INNER JOIN courseware_resources cr ON cm.courseware_resource_id = cr.resource_id " +
            "WHERE cm.course_id = #{courseId} AND cm.material_source = 'teacher_upload' AND cm.status = 'active' " +
            "ORDER BY cm.chapter_id, cm.display_order")
    List<CourseMaterial> getCoursewareMaterialsByCourseId(@Param("courseId") Integer courseId);

    /**
     * 查询外部资源类型的资料
     */
    @Select("SELECT * FROM course_materials WHERE course_id = #{courseId} AND material_source = 'external_link' AND status = 'active' ORDER BY chapter_id, display_order")
    List<CourseMaterial> getExternalMaterialsByCourseId(@Param("courseId") Integer courseId);

    /**
     * 获取章节的下一个显示顺序号
     */
    @Select("SELECT COALESCE(MAX(display_order), 0) + 1 FROM course_materials WHERE course_id = #{courseId} AND chapter_id = #{chapterId}")
    Integer getNextDisplayOrder(@Param("courseId") Integer courseId, @Param("chapterId") Integer chapterId);

    /**
     * 根据标签搜索资料
     */
    @Select("SELECT * FROM course_materials WHERE course_id = #{courseId} AND tags LIKE CONCAT('%', #{tag}, '%') AND status = 'active' ORDER BY chapter_id, display_order")
    List<CourseMaterial> getMaterialsByTag(@Param("courseId") Integer courseId, @Param("tag") String tag);

    /**
     * 获取资料统计信息
     */
    @Select("SELECT " +
            "COUNT(*) as total_materials, " +
            "COUNT(CASE WHEN material_source = 'teacher_upload' THEN 1 END) as teacher_materials, " +
            "COUNT(CASE WHEN material_source = 'external_link' THEN 1 END) as external_materials, " +
            "SUM(estimated_time) as total_estimated_time " +
            "FROM course_materials WHERE course_id = #{courseId} AND status = 'active'")
    CourseMaterialStats getMaterialStats(@Param("courseId") Integer courseId);

    /**
     * 根据章节获取资料统计
     */
    @Select("SELECT " +
            "chapter_id, " +
            "COUNT(*) as material_count, " +
            "SUM(estimated_time) as total_time " +
            "FROM course_materials " +
            "WHERE course_id = #{courseId} AND status = 'active' " +
            "GROUP BY chapter_id ORDER BY chapter_id")
    List<ChapterMaterialStats> getMaterialStatsByChapter(@Param("courseId") Integer courseId);
}