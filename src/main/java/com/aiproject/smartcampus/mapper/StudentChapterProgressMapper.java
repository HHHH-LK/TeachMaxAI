package com.aiproject.smartcampus.mapper;

import com.aiproject.smartcampus.pojo.dto.ChapterMaterialStats;
import com.aiproject.smartcampus.pojo.dto.CourseMaterialStats;
import com.aiproject.smartcampus.pojo.dto.StudentChapterProgressStats;
import com.aiproject.smartcampus.pojo.po.CourseMaterial;
import com.aiproject.smartcampus.pojo.po.StudentChapterProgress;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.util.List;

// 4. 学生章节学习记录表Mapper
@Mapper
@Repository
public interface StudentChapterProgressMapper extends BaseMapper<StudentChapterProgress> {

    /**
     * 根据学生ID查询所有章节学习进度
     */
    @Select("SELECT scp.*, c.chapter_name, c.chapter_order " +
            "FROM student_chapter_progress scp " +
            "INNER JOIN chapters c ON scp.chapter_id = c.chapter_id " +
            "WHERE scp.student_id = #{studentId} " +
            "ORDER BY c.chapter_order")
    List<StudentChapterProgress> getProgressByStudentId(@Param("studentId") Integer studentId);

    /**
     * 根据学生ID和课程ID查询章节学习进度
     */
    @Select("SELECT scp.*, c.chapter_name, c.chapter_order " +
            "FROM student_chapter_progress scp " +
            "INNER JOIN chapters c ON scp.chapter_id = c.chapter_id " +
            "WHERE scp.student_id = #{studentId} AND c.course_id = #{courseId} " +
            "ORDER BY c.chapter_order")
    List<StudentChapterProgress> getProgressByStudentAndCourse(@Param("studentId") Integer studentId,
                                                               @Param("courseId") Integer courseId);

    /**
     * 根据章节ID查询所有学生的学习进度
     */
    @Select("SELECT * FROM student_chapter_progress WHERE chapter_id = #{chapterId} ORDER BY progress_rate DESC")
    List<StudentChapterProgress> getProgressByChapterId(@Param("chapterId") Integer chapterId);

    /**
     * 根据掌握程度查询学生进度
     */
    @Select("SELECT scp.*, c.chapter_name " +
            "FROM student_chapter_progress scp " +
            "INNER JOIN chapters c ON scp.chapter_id = c.chapter_id " +
            "WHERE scp.student_id = #{studentId} AND scp.mastery_level = #{masteryLevel} " +
            "ORDER BY c.chapter_order")
    List<StudentChapterProgress> getProgressByMasteryLevel(@Param("studentId") Integer studentId,
                                                           @Param("masteryLevel") String masteryLevel);

    /**
     * 获取学生的学习统计信息
     */
    @Select("SELECT " +
            "COUNT(*) as total_chapters, " +
            "COUNT(CASE WHEN mastery_level = 'completed' OR mastery_level = 'mastered' THEN 1 END) as completed_chapters, " +
            "AVG(progress_rate) as avg_progress, " +
            "SUM(study_time) as total_study_time " +
            "FROM student_chapter_progress scp " +
            "INNER JOIN chapters c ON scp.chapter_id = c.chapter_id " +
            "WHERE scp.student_id = #{studentId} AND c.course_id = #{courseId}")
    StudentChapterProgressStats getStudentStats(@Param("studentId") Integer studentId,
                                                @Param("courseId") Integer courseId);

    /**
     * 更新学习进度
     */
    @Select("UPDATE student_chapter_progress SET " +
            "progress_rate = #{progressRate}, " +
            "mastery_level = #{masteryLevel}, " +
            "study_time = study_time + #{additionalTime}, " +
            "last_study_at = NOW(), " +
            "updated_at = NOW() " +
            "WHERE student_id = #{studentId} AND chapter_id = #{chapterId}")
    Integer updateProgress(@Param("studentId") Integer studentId,
                           @Param("chapterId") Integer chapterId,
                           @Param("progressRate") Double progressRate,
                           @Param("masteryLevel") String masteryLevel,
                           @Param("additionalTime") Integer additionalTime);

    /**
     * 更新当前学习资料
     */
    @Select("UPDATE student_chapter_progress SET " +
            "current_material_id = #{materialId}, " +
            "last_study_at = NOW(), " +
            "updated_at = NOW() " +
            "WHERE student_id = #{studentId} AND chapter_id = #{chapterId}")
    Integer updateCurrentMaterial(@Param("studentId") Integer studentId,
                                  @Param("chapterId") Integer chapterId,
                                  @Param("materialId") Integer materialId);
}