package com.aiproject.smartcampus.mapper;

import com.aiproject.smartcampus.pojo.po.ChapterQuestion;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.util.List;

// 3. 章节题目关系表Mapper
@Mapper
@Repository
public interface ChapterQuestionMapper extends BaseMapper<ChapterQuestion> {
    
    /**
     * 根据章节ID查询所有题目关系
     */
    @Select("SELECT * FROM chapter_questions WHERE chapter_id = #{chapterId} ORDER BY created_at")
    List<ChapterQuestion> getQuestionsByChapterId(@Param("chapterId") Integer chapterId);
    
    /**
     * 根据题目ID查询所属章节
     */
    @Select("SELECT * FROM chapter_questions WHERE question_id = #{questionId}")
    List<ChapterQuestion> getChaptersByQuestionId(@Param("questionId") Integer questionId);
    
    /**
     * 根据章节ID和题目类型查询题目
     */
    @Select("SELECT * FROM chapter_questions WHERE chapter_id = #{chapterId} AND question_type = #{questionType}")
    List<ChapterQuestion> getQuestionsByChapterAndType(@Param("chapterId") Integer chapterId, 
                                                      @Param("questionType") String questionType);
    
    /**
     * 获取章节的题目数量
     */
    @Select("SELECT COUNT(*) FROM chapter_questions WHERE chapter_id = #{chapterId}")
    Integer getQuestionCountByChapterId(@Param("chapterId") Integer chapterId);
    
    /**
     * 获取章节指定类型的题目数量
     */
    @Select("SELECT COUNT(*) FROM chapter_questions WHERE chapter_id = #{chapterId} AND question_type = #{questionType}")
    Integer getQuestionCountByChapterAndType(@Param("chapterId") Integer chapterId, 
                                           @Param("questionType") String questionType);
    
    /**
     * 批量插入章节题目关系
     */
    Integer insertBatch(@Param("list") List<ChapterQuestion> list);
    
    /**
     * 根据章节ID删除所有题目关系
     */
    Integer deleteByChapterId(@Param("chapterId") Integer chapterId);
    
    /**
     * 查询某课程下所有章节的题目关系
     */
    @Select("SELECT cq.* FROM chapter_questions cq " +
            "INNER JOIN chapters c ON cq.chapter_id = c.chapter_id " +
            "WHERE c.course_id = #{courseId} ORDER BY c.chapter_order, cq.created_at")
    List<ChapterQuestion> getQuestionsByCourseId(@Param("courseId") Integer courseId);
}
