package com.aiproject.smartcampus.controller;

import com.aiproject.smartcampus.commons.client.Result;
import com.aiproject.smartcampus.pojo.dto.TeacherQueryDTO;

import com.aiproject.smartcampus.pojo.dto.TeacherUpdateDTO;
import com.aiproject.smartcampus.service.TeacherService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Objects;

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
    public Result queryTeachers(@RequestBody TeacherQueryDTO queryDTO) throws Exception{
        return teacherService.queryTeachersById(queryDTO);
    }

    @PutMapping("/update")
    @Operation(summary = "更新教师信息", description = "修改教师的基本信息）")
    public Result updateTeacherInfo(@RequestBody TeacherUpdateDTO updateDTO) {
        return teacherService.updateTeacherInfo(updateDTO);
    }

    /**
     * 查询指定班级课程对应的学生整体知识点掌握情况
     * */

    @GetMapping("/getAllClassNotCorrectInfo")
    public Result getAllClassInfo(@RequestParam("couresId")  String couresId) {

        return teacherService.getAllClassInfo(couresId);
    }

    /**
     * 查询特定班级的知识点的高频错误知识点信息
     * */

    @GetMapping("/getTheMaxUncorrectPoint")
    public Result getTheMaxUncorrectPoint(@RequestParam("couresId")  String couresId) {

        return teacherService.getTheMaxUncorrectPoint(couresId);
    }



}
