package com.aiproject.smartcampus.service.impl;

import com.aiproject.smartcampus.commons.client.Result;
import com.aiproject.smartcampus.mapper.TeacherMapper;
import com.aiproject.smartcampus.pojo.dto.TeacherQueryDTO;
import com.aiproject.smartcampus.pojo.dto.TeacherUpdateDTO;
import com.aiproject.smartcampus.pojo.po.Teacher;
import com.aiproject.smartcampus.service.TeacherService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * @program: SmartCampus
 * @description:
 * @author: lk
 * @create: 2025-05-20 09:40
 **/

@Service
@Slf4j
@RequiredArgsConstructor
public class TeacherServiceImpl extends ServiceImpl<TeacherMapper, Teacher> implements TeacherService {

    private final TeacherMapper teacherMapper;

    @Override
    public Result<Teacher> queryTeachersById(TeacherQueryDTO queryDTO) {

        try {
            Teacher teacher = teacherMapper.findByUserID(queryDTO.getUserId());
            if (teacher == null) {
                return Result.error("找不到ID为 " + queryDTO.getUserId() + " 的用户");
            }else {
                return Result.success(teacher);
            }
        } catch (Exception e) {
            // 5. 错误处理
            log.error("查询教师信息失败", e);
            return Result.error("查询教师信息失败: " + e.getMessage());
        }
    }
//
//    @Override
//    public Result updateTeacherStatus(TeacherStatusDTO statusDTO) {
//        return null;
//    }

    @Override
    public Result updateTeacherInfo(TeacherUpdateDTO updateDTO) {
        try {
            // 1. 参数校验
            if (updateDTO.getTeacherId() == null) {
                return Result.error("教师ID不能为空");
            }

            // 2. 检查教师是否存在
            Teacher teacher = teacherMapper.findByUserID(updateDTO.getUserId());
            if (!teacher.getUserId().equals(updateDTO.getTeacherId())) {
                return Result.error("找不到ID为 " + updateDTO.getTeacherId() + " 的教师");
            }else {
                teacherMapper.updateById(teacher); // 使用MapStruct更新字段
                return Result.success("教师信息更新成功");
            }

        } catch (Exception e) {
            log.error("更新教师信息失败", e);
            return Result.error("更新教师信息失败: " + e.getMessage());
        }
    }


}
