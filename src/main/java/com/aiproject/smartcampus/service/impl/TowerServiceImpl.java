package com.aiproject.smartcampus.service.impl;

import com.aiproject.smartcampus.commons.client.Result;
import com.aiproject.smartcampus.mapper.CourseEnrollmentMapper;
import com.aiproject.smartcampus.model.functioncalling.toolutils.TowerCreateToolUtils;
import com.aiproject.smartcampus.pojo.po.CourseEnrollment;
import com.aiproject.smartcampus.service.TowerService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * @program: TeacherMaxAI
 * @description:
 * @author: lk_hhh
 * @create: 2025-08-07 16:54
 **/

@Service
@Slf4j
@RequiredArgsConstructor
public class TowerServiceImpl implements TowerService {

    private final TowerCreateToolUtils towerCreateToolUtils;
    private final CourseEnrollmentMapper courseEnrollmentMapper;


    @Override
    public Result<Boolean> createTowerByAgent(String studentId, String courseId) {

        //首先判断该学生是否选了这门课
        LambdaQueryWrapper<CourseEnrollment> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CourseEnrollment::getStudentId, studentId);
        queryWrapper.eq(CourseEnrollment::getCourseId, courseId);
        CourseEnrollment courseEnrollment = courseEnrollmentMapper.selectOne(queryWrapper);
        if (courseEnrollment == null) {
            return Result.error("学生未选择该课程");
        }

        //智能生成
        Boolean towerByStudentIdAndCourseId = towerCreateToolUtils.createTowerByStudentIdAndCourseId(studentId, courseId);

        if (!towerByStudentIdAndCourseId) {
            log.error("创建塔失败");
            return Result.error("创建个性化塔失败");
        }


        return Result.success();
    }
}