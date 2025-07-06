package com.aiproject.smartcampus.mapper;


import com.aiproject.smartcampus.pojo.po.Exam;
import com.aiproject.smartcampus.pojo.po.ExamPaper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;


/**
 * @author lk_hhh
 */ // 16. 试卷相关Mapper
@Repository
@Mapper
public interface ExamPaperMapper extends BaseMapper<ExamPaper> {
    
    /**
     * 查询考试试卷
     */
    @Select("SELECT * FROM exam_papers WHERE exam_id = #{examId}")
    List<ExamPaper> findByExamId(@Param("examId") Integer examId);
}

