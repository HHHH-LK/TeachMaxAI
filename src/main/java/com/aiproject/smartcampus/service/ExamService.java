package com.aiproject.smartcampus.service;

import com.aiproject.smartcampus.commons.client.Result;
import com.aiproject.smartcampus.pojo.dto.*;

import java.util.List;

public interface ExamService {
    // 创建试卷
    Result<ExamCreateResponseDTO> createExam(ExamDTO examDTO);

//    // 发布试卷
//    Result<ExamCreateResponseDTO> publishExam(Long examId);
//
//    // 修改试卷
//    Result updateExam(Long examId, ExamDTO examDTO);
//
//    // 获取试卷详情
//    Result<ExamDTO> getExamDetail(Long examId);

    // 删除试卷
    Result deleteExam(Long examId);

    // 获取题目
    Result<List<ExamQuestionDTO>> getQuestions(String questionType, String keyword);

    // 存档试卷
    Result archiveExam(Long examId);


}
