package com.aiproject.smartcampus.controller;

import com.aiproject.smartcampus.commons.client.Result;
import com.aiproject.smartcampus.pojo.po.Task;
import com.aiproject.smartcampus.service.TaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @program: TeacherMaxAI
 * @description: 塔层任务管理
 * @author: lk_hhh
 * @create: 2025-08-06 11:27
 **/

@RestController
@RequestMapping("/task")
@Slf4j
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    /**
     * 查看当前塔层任务
     */
    public Result<Task> getTowerFloorTask(@RequestParam(value = "towerFloorId") String towerFloorId) {

        return taskService.getTowerFloorTask(towerFloorId);

    }

    /**
     * 获取塔层任务中关联的知识点Id列表
     */
    public Result<List<Integer>> getTaskPointIds(@RequestParam(value = "towerFloorId") String towerFloorId) {

        return taskService.getTaskPointIds(towerFloorId);

    }



}