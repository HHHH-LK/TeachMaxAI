package com.aiproject.smartcampus.controller;

import com.aiproject.smartcampus.commons.client.Result;
import com.aiproject.smartcampus.service.TowerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @program: TeacherMaxAI
 * @description: 塔层管理
 * @author: lk_hhh
 * @create: 2025-08-06 11:11
 **/

@Slf4j
@RestController
@RequestMapping("/tower")
@RequiredArgsConstructor
public class TowerController {

    private final TowerService towerService;

    //根据学生和课程定制化生成战斗塔
    @PostMapping("/createByAgent")
    public Result<Boolean> createTowerByAgent(@RequestParam(value = "studentId")String studentId,@RequestParam(value = "courseId")String courseId){

        return towerService.createTowerByAgent(studentId,courseId);
    }

    //所有塔列表展示


    //塔层关卡浏览

    //任务挑战

    //排行榜

    //查询塔的背景故事

    //进入学生对应的塔层

    //修改塔层状态

}