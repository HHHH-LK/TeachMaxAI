package com.aiproject.smartcampus.controller;

import com.aiproject.smartcampus.commons.client.Result;
import com.aiproject.smartcampus.pojo.bo.StudentWrongKnowledgeBO;
import com.aiproject.smartcampus.pojo.dto.TeacherGetSituationDTO;
import com.aiproject.smartcampus.pojo.dto.TeacherGetStudentDTO;
import com.aiproject.smartcampus.pojo.dto.TeacherQueryDTO;

//import com.aiproject.smartcampus.pojo.dto.TeacherUpdateDTO;
import com.aiproject.smartcampus.pojo.po.Course;
import com.aiproject.smartcampus.service.TeacherService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    public Result getAllClassInfo(@RequestParam("couresId") String couresId) {

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
    public Result<List<Course>> GetAllCourse(@PathVariable Integer teacherId) {
        return teacherService.GetAllCourse(teacherId);
    }

    //获取对应课程的整体成绩情况
    @GetMapping("/getAllSituation/{courseId}")
    @Operation(summary = "获取课程整体情况", description = "根据课程ID获取该课程的整体情况")
    public Result<TeacherGetSituationDTO> GetAllSituation(@PathVariable Integer courseId) {
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
    @Operation(summary = "查询试卷", description = "根据教师ID查询试卷信息")
    public Result getPaper(@RequestParam("teacherId") Integer teacherId) {
        return teacherService.getPaper(teacherId);
    }

}
