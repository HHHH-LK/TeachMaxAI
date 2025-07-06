package com.aiproject.smartcampus.mapper;


import com.aiproject.smartcampus.pojo.po.Exam;
import com.aiproject.smartcampus.pojo.po.ExamScore;
import com.aiproject.smartcampus.pojo.po.PaperQuestion;
import com.aiproject.smartcampus.pojo.po.Teacher;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * @author lk_hhh
 */
@Repository
@Mapper
public interface PaperQuestionMapper extends BaseMapper<PaperQuestion> {
    
    /**
     * 查询试卷题目列表
     */
    @Select("SELECT pq.*, qb.question_content, qb.question_type, qb.question_options FROM paper_questions pq " +
            "LEFT JOIN question_bank qb ON pq.question_id = qb.question_id " +
            "WHERE pq.paper_id = #{paperId} ORDER BY pq.question_order")
    List<PaperQuestion> findByPaperId(@Param("paperId") Integer paperId);
    
    /**
     * 批量插入试卷题目
     */
    @Insert("<script>" +
            "INSERT INTO paper_questions (paper_id, question_id, question_order, custom_score) VALUES " +
            "<foreach collection='paperQuestions' item='item' separator=','>" +
            "(#{item.paperId}, #{item.questionId}, #{item.questionOrder}, #{item.customScore})" +
            "</foreach>" +
            "</script>")
    int batchInsert(@Param("paperQuestions") List<PaperQuestion> paperQuestions);
}