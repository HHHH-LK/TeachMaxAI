package com.aiproject.smartcampus.controller;

import com.aiproject.smartcampus.commons.client.Result;
import com.aiproject.smartcampus.pojo.po.GameUser;
import com.aiproject.smartcampus.service.GameUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.formula.functions.T;
import org.springframework.web.bind.annotation.*;

/**
 * @program: TeacherMaxAI
 * @description: 玩家管理
 * @author: lk_hhh
 * @create: 2025-08-06 11:24
 **/

@RestController
@RequestMapping("/player")
@RequiredArgsConstructor
@Slf4j
public class PlayerController {

    private final GameUserService gameUserService;

    /**
     * 查询玩家的基础信息
     */
    @GetMapping("/get/gameplayerInfo")
    public Result<GameUser> getGamePlayerInfo(@RequestParam(value = "studentId") String studentId) {

        return gameUserService.getGamePlayerInfo(studentId);

    }

    /**
     * 新建游戏角色
     */
    @PostMapping("/insert/gameplayerInfo")
    public Result<T> insertGamePlayerInfo(@RequestBody GameUser gameUser) {

        return gameUserService.setGamePlayerInfo(gameUser);
    }

    /**
     * 获取游戏角色等级
     */
    @PostMapping("/insert/gameuserExp")
    public Result<Integer> getGamePlayerLevel(@RequestParam(value = "studentId") String studentId) {

        return gameUserService.getGamePlayerLevel(studentId);

    }


    /**
     * 修改游戏昵称
     * */
    @PostMapping("/update/gameUserName")
    public Result<T> updateGamePlayerName(@RequestParam(value = "name") String name, @RequestParam(value = "gameUserId") String gameUserId) {

        return gameUserService.updateGamePlayerName(name,gameUserId);
    }


}