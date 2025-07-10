package com.aiproject.smartcampus.service.impl;

import com.aiproject.smartcampus.commons.client.Result;
import com.aiproject.smartcampus.commons.utils.UserToTypeUtils;
import com.aiproject.smartcampus.mapper.KnowledgePointMapper;
import com.aiproject.smartcampus.mapper.StudentMapper;
import com.aiproject.smartcampus.mapper.TeacherMapper;
import com.aiproject.smartcampus.pojo.bo.StudentWrongKnowledgeBO;
import com.aiproject.smartcampus.pojo.dto.TeacherQueryDTO;
import com.aiproject.smartcampus.pojo.dto.TeacherUpdateDTO;
import com.aiproject.smartcampus.pojo.po.Teacher;
import com.aiproject.smartcampus.pojo.vo.StudentKnowledgePointVO;
import com.aiproject.smartcampus.service.TeacherService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
    private final KnowledgePointMapper knowledgePointMapper;
    private final UserToTypeUtils userToTypeUtils;

    @Override
    public Result<Teacher> queryTeachersById(TeacherQueryDTO queryDTO) {

        try {
            Teacher teacher = teacherMapper.findByUserID(queryDTO.getUserId());
            if (teacher == null) {
                return Result.error("找不到ID为 " + queryDTO.getUserId() + " 的用户");
            } else {
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
            } else {
                teacherMapper.updateById(teacher); // 使用MapStruct更新字段
                return Result.success("教师信息更新成功");
            }

        } catch (Exception e) {
            log.error("更新教师信息失败", e);
            return Result.error("更新教师信息失败: " + e.getMessage());
        }
    }

    @Override
    public Result getAllClassInfo(String courseId) {
        int pointSize = 124000;

        // 初始化准确率累加 Map 和计数 List
        Map<Integer, Double> pointMap = new ConcurrentHashMap<>(pointSize);
        List<Long> pointNumList = new ArrayList<>(Collections.nCopies(pointSize, 0L));

        for (int i = 0; i < pointSize; i++) {
            pointMap.put(i, 0.0);
        }

        // 查询当前老师 ID（暂用写死）
        String teacherId = userToTypeUtils.change();

        // 查询该课程下所有学生 ID
        List<Integer> studentIdList = teacherMapper.selectAllClassStudentInfo(teacherId, courseId);

        if (studentIdList == null || studentIdList.isEmpty()) {
            return Result.success(Collections.emptyMap());
        }

        // 统计学生错误知识点
        for (Integer studentId : studentIdList) {
            List<StudentWrongKnowledgeBO> wrongList = knowledgePointMapper.getStudentWrongKnowledgeByStudentId(String.valueOf(studentId));
            if (wrongList == null) {
                continue;
            }

            for (StudentWrongKnowledgeBO bo : wrongList) {
                Integer pointId = bo.getPointId();
                Double accuracyRate = bo.getAccuracyRate();

                if (pointId == null || pointId < 0 || pointId >= pointSize || accuracyRate == null) {
                    continue;
                }

                // 累加准确率与统计数量
                pointMap.merge(pointId, accuracyRate, Double::sum);
                pointNumList.set(pointId, pointNumList.get(pointId) + 1);
            }
        }

        // 计算平均准确率（过滤无数据的项）
        Map<Integer, Double> avgMap = new HashMap<>();
        for (int i = 0; i < pointSize; i++) {
            long count = pointNumList.get(i);
            if (count > 0) {
                avgMap.put(i, pointMap.get(i) / count);
            }
        }

        return Result.success(avgMap);
    }

    @Override
    public Result getTheMaxUncorrectPoint(String couresId) {

        Integer pointSize = 124000;
        Map<Integer, Long> pointMap = new ConcurrentHashMap<>(pointSize);
        Map<Integer, StudentWrongKnowledgeBO> studentWrongKnowledgeBOMap = new ConcurrentHashMap<>();
        for (int i = 0; i <pointSize ; i++) {
            pointMap.put(i, 0L);
        }

        /*String teacherId = userToTypeUtils.change();*/
        String teacherId = "1";
        // 查询该课程下所有学生 ID
        List<Integer> studentIdList = teacherMapper.selectAllClassStudentInfo(teacherId, couresId);

        for (Integer studentId : studentIdList) {

            List<StudentWrongKnowledgeBO> studentWrongKnowledgeByStudentId = knowledgePointMapper.getStudentWrongKnowledgeByStudentId(String.valueOf(studentId));
            if (studentWrongKnowledgeByStudentId == null || studentWrongKnowledgeByStudentId.isEmpty()) {
                continue;
            }

            //统计每个知识点的错误次数
            for (StudentWrongKnowledgeBO bo : studentWrongKnowledgeByStudentId) {
                Integer pointId = bo.getPointId();
                Integer wrongAnswerCount = bo.getWrongAnswerCount();
                pointMap.put(pointId, pointMap.get(pointId) + wrongAnswerCount);
                studentWrongKnowledgeBOMap.put(pointId, bo);

            }

        }

        List<StudentWrongKnowledgeBO> list = pointMap.entrySet().stream().sorted(
                        (e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                .limit(6)
                .map(a -> {
                            return studentWrongKnowledgeBOMap.get(a.getKey());
                        }
                ).toList();


        return Result.success(list);
    }


}
