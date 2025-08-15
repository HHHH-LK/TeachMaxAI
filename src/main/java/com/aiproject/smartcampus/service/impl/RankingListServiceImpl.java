package com.aiproject.smartcampus.service.impl;

import com.aiproject.smartcampus.commons.client.Result;
import com.aiproject.smartcampus.commons.redis.RedisSort;
import com.aiproject.smartcampus.mapper.GameUserMapper;
import com.aiproject.smartcampus.pojo.po.GameUser;
import com.aiproject.smartcampus.service.RankingListService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * @program: TeacherMaxAI
 * @description:
 * @author: lk_hhh
 * @create: 2025-08-15 10:40
 **/

@Service
@Slf4j
@RequiredArgsConstructor
public class RankingListServiceImpl implements RankingListService {

    private final RedisSort redisSort;
    private final GameUserMapper gameUserMapper;


    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SortUserInfo {

        String GameUserName;
        //排名
        String SortNumber;
        //层数
        String Grade;

    }

    @Override
    public Result<SortUserInfo> getUserTowerInfo(String studentId) {
        // 查询学生总排行榜信息
        RedisSort.StudentRankInfo studentTotalRank = redisSort.getStudentTotalRank(studentId);

        // 如果没找到，返回一个空的 SortUserInfo
        if (!studentTotalRank.isFound()) {
            return Result.success(new SortUserInfo(studentId, "未上榜", "0"));
        }

        SortUserInfo sortUserInfo = getGameNames(studentId, studentTotalRank);

        return Result.success(sortUserInfo);
    }

    @Override
    public Result<SortUserInfo> getUserTowerSortUserInfo(String studentId, String towerId) {

        RedisSort.StudentRankInfo studentCourseRank = redisSort.getStudentCourseRank(studentId, towerId);

        // 如果没找到，返回一个空的 SortUserInfo
        if (!studentCourseRank.isFound()) {
            return Result.success(new SortUserInfo(studentId, "未上榜", "0"));
        }

        SortUserInfo sortUserInfo = getGameNames(studentId, studentCourseRank);

        return Result.success(sortUserInfo);
    }

    private SortUserInfo getGameNames(String studentId, RedisSort.StudentRankInfo studentTotalRank) {
        //查询玩家信息
        LambdaQueryWrapper<GameUser> queryWrapper = new LambdaQueryWrapper<GameUser>();
        queryWrapper.eq(GameUser::getStudentId, studentId);
        GameUser gameUser = gameUserMapper.selectOne(queryWrapper);
        String gameName = gameUser == null ? "游客" : gameUser.getGameName();
        // 封装 SortUserInfo
        SortUserInfo sortUserInfo = new SortUserInfo();
        sortUserInfo.setGameUserName(gameName);
        sortUserInfo.setSortNumber(String.valueOf(studentTotalRank.getRank()));
        sortUserInfo.setGrade(String.valueOf((int) studentTotalRank.getScore()));

        log.info("查询到的排行榜上的用户信息为【{}】", sortUserInfo);

        return sortUserInfo;

    }


}