package com.aiproject.smartcampus.controller;

import com.aiproject.smartcampus.commons.client.Result;
import com.aiproject.smartcampus.pojo.dto.*;
import com.aiproject.smartcampus.service.ExamService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/exam")
@RequiredArgsConstructor
public class ExamController {
    private final ExamService examService;

    @PostMapping("/create")
    @Operation(summary = "创建新试卷", description = "创建新的试卷信息")
    public Result<ExamCreateResponseDTO> createExam(@RequestBody ExamDTO teacherExamDTO) throws Exception {
        return examService.createExam(teacherExamDTO);
    }

//    //发布试卷
//    @PostMapping("/publish/{examId}")
//    @Operation(summary = "发布试卷", description = "将创建的试卷发布到系统中")
//    public Result<ExamCreateResponseDTO> publishExam(@PathVariable("examId") Long examId, @RequestBody ExamPublishDTO examPublishDTO) throws Exception {
//        return examService.publishExam(examId, examPublishDTO);
//    }

//    //修改试卷
//    @PutMapping("/update/{examId}")
//    @Operation(summary = "修改试卷", description = "修改已创建的试卷内容")
//    public Result updateExam(@PathVariable("examId") Long examId, @RequestBody ExamDTO examDTO) {
//        return examService.updateExam(examId, examDTO);
//    }

//    //获取教师创建的试卷列表
//    @GetMapping("/list")
//    @Operation(summary = "获取试卷列表", description = "根据查询条件获取教师创建的试卷列表")
//    public Result<List<ExamDTO>> getExamsByQuery(@RequestBody ExamQueryDTO queryDTO) {
//        return examService.getExamsByQuery(queryDTO);
//    }

//    //获取试卷详情
//    @GetMapping("/detail/{examId}")
//    @Operation(summary = "获取试卷详情", description = "获取指定试卷的详细信息")
//    public Result<ExamDTO> getExamDetail(@PathVariable("examId") Long examId) {
//        return examService.getExamDetail(examId);
//    }

//    //删除试卷
//    @DeleteMapping("/delete/{examId}")
//    @Operation(summary = "删除试卷", description = "删除指定的试卷")
//    public Result deleteExam(@PathVariable("examId") Long examId) {
//        return examService.deleteExam(examId);
//    }

    //获取题目
    @GetMapping("/questions")
    @Operation(summary = "获取题目", description = "根据题目类型和关键词获取题目列表")
    public Result<List<ExamQuestionDTO>> getQuestions(@RequestParam("questionType") String questionType, @RequestParam("keyword") String keyword) {
        return examService.getQuestions(questionType, keyword);
    }

    //存档试卷
    @PostMapping("/archive/{examId}")
    @Operation(summary = "存档试卷", description = "将指定的试卷存档")
    public Result archiveExam(@PathVariable("examId") Long examId) {
        return examService.archiveExam(examId);
    }
}
