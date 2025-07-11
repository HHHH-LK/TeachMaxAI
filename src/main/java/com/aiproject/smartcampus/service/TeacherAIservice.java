package com.aiproject.smartcampus.service;

import com.aiproject.smartcampus.commons.client.Result;
import com.aiproject.smartcampus.pojo.vo.ExamCreationResult;

/**
 * @program: ss
 * @description: 教师ai交互层
 * @author: lk_hhh
 * @create: 2025-07-11 04:22
 **/
public interface TeacherAIservice {

    Result aiMarkingExam(String studentId, String examId);

    ExamCreationResult createIntelligentExam(String chapter, String courseId);

    Result<String> aiclassAiayaisc(String courseId);

    Result<String> TeacherTextCreate(String content, String courseId, String chapterId);
}