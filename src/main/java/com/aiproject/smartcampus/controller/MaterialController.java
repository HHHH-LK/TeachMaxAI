package com.aiproject.smartcampus.controller;


import com.aiproject.smartcampus.commons.client.Result;
import com.aiproject.smartcampus.pojo.dto.CreateMaterialDTO;
import com.aiproject.smartcampus.pojo.po.CourseMaterial;
import com.aiproject.smartcampus.pojo.po.LessonPlan;
import com.aiproject.smartcampus.service.MaterialService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/material")
@RequiredArgsConstructor
public class MaterialController {

    private final MaterialService materialService;
    /**
     * 新建课程资源
     */
    @PostMapping("/create")
    @Operation(summary = "创建课程资源", description = "创建新的课程资源")
    public Result createMaterial(@RequestBody CreateMaterialDTO createMaterialDTO) {
        // 调用服务层方法创建课程资源
        return materialService.createMaterial(createMaterialDTO);
    }

    /**
     * 获取课程资源
     */
    @GetMapping("/get")
    @Operation(summary = "获取课程资源", description = "根据课程ID获取课程资源")
    public Result<List<CourseMaterial>> getMaterialByCourseId(@RequestParam("courseId") String courseId) {
        return materialService.getMaterialByCourseId(courseId);
    }

    /**
     * 删除课程资源
     */
    @DeleteMapping("/delete")
    @Operation(summary = "删除课程资源", description = "根据资源ID删除课程资源")
    public Result deleteMaterial(@RequestParam("materialId") String materialId) {
        return materialService.deleteMaterial(materialId);
    }

    /**
     * 获取教案信息
     */
    @GetMapping("/getLessonPlan")
    @Operation(summary = "获取教案信息", description = "根据课程ID获取教案信息")
    public Result<List<LessonPlan>> getLessonPlanByCourseId(@RequestParam("courseId") String courseId) {
        return materialService.getLessonPlanByCourseId(courseId);
    }

    /**
     * 更新教案内容
     */
    @PutMapping("/updateLessonPlan")
    @Operation(summary = "更新教案内容", description = "根据教案ID更新教案内容")
    public Result updateLessonPlan(@RequestBody LessonPlan lessonPlan) {
        return materialService.updateLessonPlan(lessonPlan);
    }

    /**
     * 更新教案状态
     */
    @GetMapping("/updateLessonPlanStatus")
    @Operation(summary = "更新教案状态", description = "根据教案ID更新教案状态")
    public Result updateLessonPlanStatus(@RequestParam("lessonPlanId") String lessonPlanId,
                                         @RequestParam("status") String status) {
        return materialService.updateLessonPlanStatus(lessonPlanId, status);
    }

    /**
     * 更新教案状态为pending
     */
    @PutMapping("/updateLessonPlanToPending")
    @Operation(summary = "更新教案状态为待审核", description = "将教案状态更新为待审核")
    public Result updateLessonPlanToPending(@RequestParam("lessonPlanId") String lessonPlanId) {
        return materialService.updateLessonPlanStatusPending(lessonPlanId);
    }

    /**
     * 删除教案
     */
    @DeleteMapping("/deleteLessonPlan")
    @Operation(summary = "删除教案", description = "根据教案ID删除教案")
    public Result deleteLessonPlan(@RequestParam("lessonPlanId") String lessonPlanId) {
        return materialService.deletePlan(lessonPlanId);
    }

}
