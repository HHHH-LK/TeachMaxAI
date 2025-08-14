package com.aiproject.smartcampus.service.impl;

import com.aiproject.smartcampus.commons.client.Result;
import com.aiproject.smartcampus.mapper.GameUserMapper;
import com.aiproject.smartcampus.pojo.po.GameUser;
import com.aiproject.smartcampus.service.GameUserService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.formula.functions.T;
import org.springframework.stereotype.Service;

/**
 * @program: TeacherMaxAI
 * @description:
 * @author: lk_hhh
 * @create: 2025-08-12 10:20
 **/

@Service
@RequiredArgsConstructor
@Slf4j
public class GameUserServiceImpl implements GameUserService {

    private final GameUserMapper gameUserMapper;

    @Override
    public Result<GameUser> getGamePlayerInfo(String studentId) {

        GameUser gameUserForDB = getGameUserForDB(studentId);

        return Result.success(gameUserForDB);
    }

    private GameUser getGameUserForDB(String studentId) {
        LambdaQueryWrapper<GameUser> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(GameUser::getStudentId, studentId);
        GameUser gameUser = gameUserMapper.selectOne(queryWrapper);
        if (gameUser == null) {
            log.error("游戏用户不存在");
            throw new RuntimeException("游戏用户不存在");
        }

        return gameUser;
    }

    @Override
    public Result<T> setGamePlayerInfo(GameUser gameUser) {

        int insert = gameUserMapper.insert(gameUser);

        if (insert <= 0) {
            log.error("新建游戏用户失败");
            throw new RuntimeException("新建游戏用户失败");
        }

        return Result.success();
    }

    @Override
    public Result<Integer> getGamePlayerLevel(String studentId) {

        GameUser gameUserForDB = getGameUserForDB(studentId);

        return Result.success(gameUserForDB.getLevel());

    }

    @Override
    public Result<T> updateGamePlayerName(String name, String gameUserId) {

        LambdaUpdateWrapper<GameUser> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(GameUser::getUserId, gameUserId);
        updateWrapper.set(GameUser::getGameName, name);

        int update = gameUserMapper.update(updateWrapper);
        if (update <= 0) {
            log.error("修改名称失败");
            throw new RuntimeException("修改名称失败");
        }

        return Result.success();
    }

    /**
     * 根据等级计算用户血量
     */
    public static Integer calculateGameUserHP(Integer userLevel) {
        if (userLevel == null || userLevel <= 0) {
            return 150;
        }
        int baseHP = 150;
        int linearIncrease = 25;
        double scalingFactor = Math.sqrt(userLevel);

        return (int) (baseHP + linearIncrease * (userLevel - 1) * scalingFactor);
    }


}