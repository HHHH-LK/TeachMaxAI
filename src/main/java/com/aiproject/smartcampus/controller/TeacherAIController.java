package com.aiproject.smartcampus.controller;

import com.aiproject.smartcampus.commons.client.Result;
import com.aiproject.smartcampus.pojo.vo.ExamCreationResult;
import com.aiproject.smartcampus.service.TeacherAIservice;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @program: ss
 * @description: 教师ai控制层
 * @author: lk_hhh
 * @create: 2025-07-11 04:21
 **/
@RestController
@RequestMapping("teacher/ai")
@RequiredArgsConstructor
public class TeacherAIController {

    private final TeacherAIservice teacherAIservice;

    /**
     * ai自动判卷
     */
    @PostMapping("/aiMarkingExam")
    public Result aiMarkingExam(@RequestParam("studentId") String studentId, @RequestParam("examId") String examId) {

        return teacherAIservice.aiMarkingExam(studentId, examId);

    }

    /**
     * 智能创建试卷
     */
    @PostMapping("/createExamByai")

    public Result<ExamCreationResult> createIntelligentExam(@RequestParam String content, @RequestParam String courseId) {


        ExamCreationResult result = teacherAIservice.createIntelligentExam(content, courseId);

        if (result.getSuccess()) {
            return Result.success(result);
        } else {
            return Result.error(result.getErrorMessage());
        }


    }


}