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


//    @PutMapping("/status")
//    @Operation(summary = "更新教师状态", description = "修改教师的在职状态（如：在职、离职等）")
//    public Result updateTeacherStatus(@RequestBody TeacherStatusDTO statusDTO) {
//        return teacherService.updateTeacherStatus(statusDTO);
//    }

}
