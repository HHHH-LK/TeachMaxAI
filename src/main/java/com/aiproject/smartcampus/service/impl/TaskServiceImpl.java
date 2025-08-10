package com.aiproject.smartcampus.service.impl;

import com.aiproject.smartcampus.commons.client.Result;
import com.aiproject.smartcampus.mapper.TaskMapper;
import com.aiproject.smartcampus.pojo.po.Task;
import com.aiproject.smartcampus.service.TaskService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @program: TeacherMaxAI
 * @description:
 * @author: lk_hhh
 * @create: 2025-08-09 10:32
 **/

@Service
@RequiredArgsConstructor
@Slf4j
public class TaskServiceImpl implements TaskService {

    private final TaskMapper taskMapper;

    @Override
    public Result<Task> getTowerFloorTask(String towerFloorId) {

        LambdaQueryWrapper<Task> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Task::getFloorId, towerFloorId);
        Task task = taskMapper.selectOne(queryWrapper);

        if (task == null) {
            log.error("任务为空");
            return Result.error("任务为空");
        }

        return Result.success(task);
    }

    @Override
    public Result<List<Integer>> getTaskPointIds(String towerFloorId) {

        LambdaQueryWrapper<Task> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Task::getFloorId, towerFloorId);
        Task task = taskMapper.selectOne(queryWrapper);
        if (task == null) {
            log.error("该层没有任务");
            return Result.error("该层任务为空");
        }

        String pointIds = task.getPointIds();
        if (pointIds == null) {
            log.error("该层不含任何知识点");
            return Result.error("该层不含任何知识点");
        }

        List<Integer> points = parsePoints(pointIds);

        return Result.success(points);
    }


    /**
     * 解析数据库中的知识点集合
     */
    public List<Integer> parsePoints(String pointIds) {

        // 去掉前后的方括号和空格
        String trimmed = pointIds.replaceAll("\\[|\\]", "").trim();

        // 按逗号分割并转成 Integer
        return Arrays.stream(trimmed.split(","))
                // 去掉每个数字的多余空格
                .map(String::trim)
                .map(Integer::valueOf)
                .collect(Collectors.toList());


    }
}