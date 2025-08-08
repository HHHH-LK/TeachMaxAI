package com.aiproject.smartcampus.commons.sort;

import com.aiproject.smartcampus.pojo.po.Tower;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

/**
 * @program: TeacherMaxAI
 * @description: 实时排序
 * @author: lk_hhh
 * @create: 2025-08-08 22:36
 **/

@Component
@RequiredArgsConstructor
@Slf4j
public class RedisSort {

    private final StringRedisTemplate stringRedisTemplate;

    private static final String LEADERBOARD_KEY = "game:sort:courseId:";
    private static final String TOTLE_SORT_KEY = "game:sort:total";

    /**
     * 新加数据到排行榜中，或者进行更新
     */
    public void addToSortedList(String towerId, String studentId, String towerMaxNum) {

        stringRedisTemplate.opsForZSet().add(LEADERBOARD_KEY + towerId, studentId, Double.parseDouble(towerMaxNum));

    }

    public void addToTotalSortedList(String studentId, String towerMaxNum) {

        stringRedisTemplate.opsForZSet().add(TOTLE_SORT_KEY, studentId, Double.parseDouble(towerMaxNum));

    }

    /**
     * 获取前十强
     * */
    public Set<ZSetOperations.TypedTuple<String>> getSortedList(String courseId) {

        return stringRedisTemplate.opsForZSet().reverseRangeWithScores(LEADERBOARD_KEY + courseId, 0, 9);
    }

    public Set<ZSetOperations.TypedTuple<String>> getTotleSortedList() {

        return stringRedisTemplate.opsForZSet().reverseRangeWithScores(TOTLE_SORT_KEY, 0, 9);
    }


}