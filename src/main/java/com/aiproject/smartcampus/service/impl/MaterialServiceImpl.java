package com.aiproject.smartcampus.service.impl;

import com.aiproject.smartcampus.commons.client.Result;
import com.aiproject.smartcampus.commons.utils.UserToTypeUtils;
import com.aiproject.smartcampus.mapper.CourseMaterialMapper;
import com.aiproject.smartcampus.mapper.LessonPlanMapper;
import com.aiproject.smartcampus.pojo.dto.CreateMaterialDTO;
import com.aiproject.smartcampus.pojo.po.CourseMaterial;
import com.aiproject.smartcampus.pojo.po.LessonPlan;
import com.aiproject.smartcampus.service.MaterialService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class MaterialServiceImpl implements MaterialService {
    private final CourseMaterialMapper courseMaterialMapper;
    private final LessonPlanMapper lessonPlanMapper;
    private final UserToTypeUtils userToTypeUtils;

    public Result createMaterial(CreateMaterialDTO createMaterialDTO) {
        try {
            // 1. DTO数据校验
            if (createMaterialDTO.getCourseId() == null || createMaterialDTO.getResourceUrl() == null ||
                    createMaterialDTO.getMaterialTitle() == null) {
                return Result.error("必填字段不能为空");
            }

            // 2. 转换DTO为实体对象
            CourseMaterial material = new CourseMaterial();

            // 设置资源信息（默认为外部链接）
            material.setExternalResourceUrl(createMaterialDTO.getResourceUrl());
            material.setMaterialSource(CourseMaterial.MaterialSource.EXTERNAL_LINK);

            // 自动推断资料类型
            material.setMaterialType(CourseMaterial.MaterialType.LINK);

            // 设置基础信息
            material.setMaterialTitle(createMaterialDTO.getMaterialTitle());
            material.setMaterialDescription(createMaterialDTO.getMaterialDescription());
            material.setEstimatedTime(createMaterialDTO.getEstimatedTime());
            material.setCourseId(Integer.valueOf(createMaterialDTO.getCourseId()));
            material.setChapterId(Integer.valueOf(createMaterialDTO.getChapterId()));
            material.setCreatedAt(LocalDateTime.now());
            material.setUpdatedAt(LocalDateTime.now());

            // 转换难度等级
            if (createMaterialDTO.getDifficultyLevel() != null) {
                material.setDifficultyLevel(
                        CourseMaterial.DifficultyLevel.fromValue(createMaterialDTO.getDifficultyLevel().toLowerCase())
                );
            } else {
                material.setDifficultyLevel(CourseMaterial.DifficultyLevel.MEDIUM);
            }

            // 设置默认值
            material.setStatus(CourseMaterial.MaterialStatus.ACTIVE);
            material.setIsDownloadable(true);
            material.setTags("");

            material.setCreatedBy(createMaterialDTO.getUserId());

            // 4. 保存到数据库
            courseMaterialMapper.insert(material);
            log.info("创建资料成功: {}", material);
            return Result.success();
        } catch (Exception e) {
            log.error("创建资料失败: {}", e.getMessage());
            return Result.error("系统错误: " + e.getMessage());
        }
    }

    @Override
    public Result<List<CourseMaterial>> getMaterialByCourseId(String courseId) {
        try {
            // 1. 参数校验
            if (courseId == null || courseId.isEmpty()) {
                return Result.error("课程ID不能为空");
            }

            // 2. 转换参数类型
            Integer courseIdInt;
            try {
                courseIdInt = Integer.parseInt(courseId);
            } catch (NumberFormatException e) {
                return Result.error("课程ID格式错误");
            }

            // 3. 查询数据库
            List<CourseMaterial> materials = courseMaterialMapper.selectByCourseId(courseIdInt);

            // 4. 处理查询结果（关键修改）
            if (materials == null || materials.isEmpty()) {
                return Result.error("未找到对应的课程资源");
            }

            return Result.success(materials);
        } catch (Exception e) {
            // 改进日志记录
            log.error("获取课程资源失败 - courseId: {}. 错误详情: ", courseId, e);
            return Result.error("系统错误: " + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()));
        }
    }

    @Override
    public Result deleteMaterial(String materialId) {
        try {
            if (materialId == null || materialId.isEmpty()) {
                return Result.error("资源ID不能为空");
            }
            int rows = courseMaterialMapper.deleteMaterialById(materialId);
            if (rows > 0) {
                return Result.success("资源删除成功");
            } else {
                return Result.error("资源不存在或删除失败");
            }
        } catch (Exception e) {
            log.error("删除课程资源失败 - materialId: {}. 错误详情: ", materialId, e);
            return Result.error("系统错误: " + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()));
        }
    }

    @Override
    public Result<List<LessonPlan>> getLessonPlanByCourseId(String courseId) {
        try {
            if (courseId == null || courseId.isEmpty()) {
                return Result.error("课程ID不能为空");
            }

            // 这里可以调用其他服务或数据库查询教案信息
            List<LessonPlan> lessonPlans = lessonPlanMapper.getLessonPlanByCourseId(courseId);

            if (lessonPlans == null || lessonPlans.isEmpty()) {
                return Result.error("未找到对应的教案信息");
            }

            return Result.success(lessonPlans);
        } catch (Exception e) {
            log.error("获取教案信息失败 - courseId: {}. 错误详情: ", courseId, e);
            return Result.error("系统错误: " + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()));
        }
    }

    @Override
    public Result updateLessonPlan(LessonPlan lessonPlan) {
        try {
            if (lessonPlan == null || lessonPlan.getPlanId() == null) {
                return Result.error("教案信息或ID不能为空");
            }

            // 更新教案内容
            lessonPlan.setUpdatedAt(LocalDateTime.now());
            int rows = lessonPlanMapper.updateById(lessonPlan);
            if (rows > 0) {
                return Result.success("教案更新成功");
            } else {
                return Result.error("教案不存在或更新失败");
            }
        } catch (Exception e) {
            log.error("更新教案失败 - lessonPlan: {}. 错误详情: ", lessonPlan, e);
            return Result.error("系统错误: " + e.getMessage());
        }
    }

    @Override
    public Result updateLessonPlanStatus(String lessonPlanId, String status) {
        try {
            // 1. 参数校验
            if (lessonPlanId == null || lessonPlanId.isEmpty() || status == null || status.isEmpty()) {
                return Result.error("教案ID或状态不能为空");
            }

            // 2. 验证状态值
            if (!LessonPlan.isValidStatus(status)) {
                return Result.error("无效的教案状态: " + status);
            }

            // 3. 转换ID类型
            Long planId;
            try {
                planId = Long.parseLong(lessonPlanId);
            } catch (NumberFormatException e) {
                return Result.error("教案ID格式错误");
            }

            // 4. 获取当前管理员ID（需要根据实际项目实现）
            String adminId = userToTypeUtils.change();
//            String adminId = "1";

            // 5. 更新教案状态
            int rows = lessonPlanMapper.updateStatusById(lessonPlanId, status, adminId);

            if (rows > 0) {
                return Result.success("教案状态更新成功");
            } else {
                return Result.error("教案不存在或状态更新失败");
            }
        } catch (Exception e) {
            log.error("更新教案状态失败 - lessonPlanId: {}, status: {}. 错误详情: ",
                    lessonPlanId, status, e);
            return Result.error("系统错误: " + e.getMessage());
        }
    }

    @Override
    public Result updateLessonPlanStatusPending(String lessonPlanId) {
        try {
            if (lessonPlanId == null || lessonPlanId.isEmpty()) {
                return Result.error("教案ID或状态不能为空");
            }
            int rows = lessonPlanMapper.updateStatusToPending(lessonPlanId);
            if (rows > 0) {
                return Result.success("教案状态更新成功");
            } else {
                return Result.error("教案不存在或状态更新失败");
            }
        }catch (Exception e){
            log.error("更新教案状态失败 - lessonPlanId: {}, 错误详情: ",
                    lessonPlanId,  e);
            return Result.error("系统错误: " + e.getMessage());
        }
    }

    @Override
    public Result deletePlan(String lessonPlanId) {
        try {
            if (lessonPlanId == null || lessonPlanId.isEmpty()) {
                return Result.error("教案ID不能为空");
            }
            int rows = lessonPlanMapper.deletePlan(lessonPlanId);
            if (rows > 0) {
                return Result.success("教案删除成功");
            } else {
                return Result.error("教案不存在或删除失败");
            }
        } catch (Exception e) {
            log.error("删除教案失败 - lessonPlanId: {}. 错误详情: ", lessonPlanId, e);
            return Result.error("系统错误: " + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()));
        }
    }
}
