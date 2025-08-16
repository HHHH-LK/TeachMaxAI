package com.aiproject.smartcampus.service;

import com.aiproject.smartcampus.commons.client.Result;
import com.aiproject.smartcampus.pojo.dto.CreateMaterialDTO;
import com.aiproject.smartcampus.pojo.po.CourseMaterial;
import com.aiproject.smartcampus.pojo.po.LessonPlan;

import java.util.List;

public interface MaterialService {
    Result createMaterial(CreateMaterialDTO createMaterialDTO);

    Result<List<CourseMaterial>> getMaterialByCourseId(String courseId);

    Result deleteMaterial(String materialId);

    Result<List<LessonPlan>> getLessonPlanByCourseId(String courseId);

    Result updateLessonPlan(LessonPlan lessonPlan);

    Result updateLessonPlanStatus(String lessonPlanId, String status);

    Result updateLessonPlanStatusPending(String lessonPlanId);

    Result deletePlan(String lessonPlanId);
}
