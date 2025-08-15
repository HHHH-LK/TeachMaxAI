package com.aiproject.smartcampus.controller;

import com.aiproject.smartcampus.commons.client.Result;
import com.aiproject.smartcampus.pojo.bo.StudentWrongKnowledgeBO;
import com.aiproject.smartcampus.pojo.dto.TeacherGetSituationDTO;
import com.aiproject.smartcampus.pojo.dto.TeacherGetStudentDTO;
import com.aiproject.smartcampus.pojo.dto.TeacherQueryDTO;

//import com.aiproject.smartcampus.pojo.dto.TeacherUpdateDTO;
import com.aiproject.smartcampus.pojo.po.Course;
import com.aiproject.smartcampus.pojo.vo.*;
import com.aiproject.smartcampus.service.TeacherService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * @program: SmartCampus
 * @description: 教师控制层
 * @author: lk
 * @create: 2025-05-19 16:50
 **/

@RestController
@RequestMapping("/teacher")
@RequiredArgsConstructor
public class TeacherController {

    private final TeacherService teacherService;

    @GetMapping("/query/{userId}")
    @Operation(summary = "查询教师", description = "查询教师信息")
    public Result<TeacherQueryDTO> queryTeachers(@PathVariable Integer userId) throws Exception {
        return teacherService.queryTeachersById(userId);
    }

    @PutMapping("/update/{userId}")
    @Operation(summary = "更新教师信息", description = "修改教师的基本信息")
    public Result updateTeacherInfo(@PathVariable Integer userId, @RequestBody TeacherQueryDTO updateDTO) {
        return teacherService.updateTeacherInfo(userId, updateDTO);
    }

    /**
     * 查询指定班级课程对应的学生整体知识点掌握情况
     */
    @GetMapping("/getAllClassNotCorrectInfo")
    public Result<Map<Integer, Double>> getAllClassInfo(@RequestParam("couresId") String couresId) {

        return teacherService.getAllClassInfo(couresId);
    }

    /**
     * 查询特定班级的知识点的高频错误知识点信息
     */
    @GetMapping("/getTheMaxUncorrectPoint")
    public Result<List<StudentWrongKnowledgeBO>> getTheMaxUncorrectPoint(@RequestParam("couresId") String couresId) {

        return teacherService.getTheMaxUncorrectPoint(couresId);
    }

    @GetMapping("/getClass/{teacherId}")
    @Operation(summary = "获取教师所教授的课程", description = "根据教师ID获取其所教授的课程列表")
    public Result<List<Course>> getAllCourse(@PathVariable Integer teacherId) {
        return teacherService.GetAllCourse(teacherId);
    }

    //获取对应课程的整体成绩情况
    @GetMapping("/getAllSituation/{courseId}")
    @Operation(summary = "获取课程整体情况", description = "根据课程ID获取该课程的整体情况")
    public Result<TeacherGetSituationDTO> getAllSituation(@PathVariable Integer courseId) {
        return teacherService.GetAllSituation(courseId);
    }

    //获取对应学生信息
    @GetMapping("/getStudentInfo/{courseId}")
    @Operation(summary = "获取课程学生信息", description = "根据课程ID获取该课程的学生信息")
    public Result<List<TeacherGetStudentDTO>> getStudentInfo(@PathVariable Integer courseId) {
        return teacherService.getStudentInfo(courseId);
    }

    //查询试卷
    @GetMapping("/addPaper")
    @Operation(summary = "查询试卷", description = "根据课程ID查询试卷信息")
    public Result<List<ExamInfoVO>> getPaper(@RequestParam("courseId") Integer courseId) {
        return teacherService.getPaper(courseId);
    }

    /**
     * 查询作业
     */
    @GetMapping("/getHomework")
    @Operation(summary = "查询作业", description = "根据课程ID查询作业信息")
    public Result<List<ChapterQuestionDetailTeacherVO>> getHomework(@RequestParam("courseId") String courseId, @RequestParam("chapterId") String chapterId) {
        return teacherService.getHomework(courseId, chapterId);
    }

    /**
     * 根据学生获取所有作业信息
     */
    @GetMapping("/getHomeworkByStudent")
    @Operation(summary = "根据学生获取所有作业信息", description = "根据学生ID查询其所有作业信息")
    public Result<List<ChapterQuestionDetailVO>> getHomeworkByStudent(@RequestParam("studentId") String studentId, @RequestParam("courseId") String courseId, @RequestParam("chapterId") String chapterId) {
        return teacherService.getHomeworkByStudent(studentId, courseId, chapterId);
    }

    /**
     * 获取教师所教授的考试的学生情况
     */
    @GetMapping("/getExamStudentInfo/{examId}")
    @Operation(summary = "获取考试学生信息", description = "根据考试ID获取该考试的学生信息")
    public Result<List<ExamStudentVO>> getExamStudentInfo(@PathVariable String examId) {
        return teacherService.getExamStudentInfo(examId);
    }

    /**
     * 教师发布试卷
     */
    @PostMapping("/releasePaper")
    @Operation(summary = "发布试卷", description = "教师发布试卷")
    public Result releasePaper(@RequestParam("examId") String examId) {
        return teacherService.releasePaper(examId);
    }

    /**
     * 考试状态更新
     */
    @PostMapping("/updateExamStatus")
    @Operation(summary = "更新考试状态", description = "教师更新考试状态")
    public Result updateExamStatus(@RequestParam("examId") String examId) {
        return teacherService.updateExamStatusById(examId);
    }

    /**
     * 删除考试
     */
    @DeleteMapping("/deleteExam/{examId}")
    @Operation(summary = "删除考试", description = "教师删除考试")
    public Result deleteExamById(@PathVariable String examId) {

        return teacherService.deleteExamById(examId);
    }


    /**
     * 根据teacherId获取userId
     * */
    @GetMapping("/getUserIdByteacher")
    public Result<Integer> getUserIdByteacher(@RequestParam("teacherId") Integer teacherId) {
        return teacherService.getUserIdByteacher(teacherId);
    }

    /**
     * 获取学生的作业情况
     */
    @GetMapping("/getStudentHomework")
    @Operation(summary = "获取课程作业情况", description = "根据课程ID获取其作业情况")
    public Result<CourseSummaryVO> getStudentHomework(@RequestParam("courseId") String courseId) {
        return teacherService.getStudentHomework(courseId);
    }

    /**
     * 获取学生的考试情况
     */
    @GetMapping("/getStudentExam")
    @Operation(summary = "获取课程考试情况", description = "根据课程ID获取其考试情况")
    public Result<ExamSummaryVO> getStudentExam(@RequestParam("courseId") String courseId) {
        return teacherService.getStudentExam(courseId);
    }

    /**
     * 根据课程Id查询知识点掌握
     */
    @GetMapping("/getKnowledgePointMastery")
    @Operation(summary = "获取知识点掌握情况", description = "根据课程ID获取知识点掌握情况")
    public Result<Map<String, KnowledgePointMasteryVO>> getKnowledgePointMastery(@RequestParam("courseId") String courseId) {
        return teacherService.getKnowledgePointMastery(courseId);
    }
}
