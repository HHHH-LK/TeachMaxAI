package com.aiproject.smartcampus.controller;

import com.aiproject.smartcampus.commons.client.Result;
import com.aiproject.smartcampus.pojo.po.Monster;
import com.aiproject.smartcampus.service.MonsterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @program: TeacherMaxAI
 * @description: boss管理
 * @author: lk_hhh
 * @create: 2025-08-06 12:25
 **/

@RestController
@RequestMapping("/boss")
@Slf4j
@RequiredArgsConstructor
public class BossController {

    private final MonsterService monsterService;

    /**
     * 获取boss信息
     * */
    @GetMapping("/get/bossInfo")
    public Result<Monster> getBossInfo(@RequestParam(value = "floorId") String floorId){

        return monsterService.getBossInfo(floorId);

    }


}