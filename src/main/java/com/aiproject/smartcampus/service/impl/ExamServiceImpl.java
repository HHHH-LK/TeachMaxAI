package com.aiproject.smartcampus.service.impl;

import com.aiproject.smartcampus.commons.client.Result;
import com.aiproject.smartcampus.pojo.dto.*;
import com.aiproject.smartcampus.service.ExamService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ExamServiceImpl implements ExamService {
    @Override
    public Result<ExamCreateResponseDTO> createExam(ExamDTO examDTO) {
        return null;
    }

    @Override
    public Result<ExamCreateResponseDTO> publishExam(Long examId, ExamPublishDTO publishDTO) {
        return null;
    }

    @Override
    public Result updateExam(Long examId, ExamDTO examDTO) {
        return null;
    }

    @Override
    public Result<List<ExamDTO>> getExamsByQuery(ExamQueryDTO queryDTO) {
        return null;
    }

    @Override
    public Result<ExamDTO> getExamDetail(Long examId) {
        return null;
    }

    @Override
    public Result deleteExam(Long examId) {
        return null;
    }

    @Override
    public Result<List<ExamQuestionDTO>> getQuestions(String questionType, String keyword) {
        return null;
    }

    @Override
    public Result archiveExam(Long examId) {
        return null;
    }
}
