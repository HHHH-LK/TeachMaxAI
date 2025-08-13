package com.aiproject.smartcampus.service.impl;

import com.aiproject.smartcampus.commons.client.Result;
import com.aiproject.smartcampus.mapper.BossMapper;
import com.aiproject.smartcampus.pojo.po.Monster;
import com.aiproject.smartcampus.service.MonsterService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * @program: TeacherMaxAI
 * @description:
 * @author: lk_hhh
 * @create: 2025-08-12 10:47
 **/

@Service
@Slf4j
@RequiredArgsConstructor
public class MonsterServiceImpl implements MonsterService {

    private final BossMapper bossMapper;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public Result<Monster> getBossInfo(String floorId) {

        LambdaQueryWrapper<Monster> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Monster::getFloorId, floorId);
        Monster monster = bossMapper.selectOne(queryWrapper);
        if (monster == null) {
            log.info("该层没有boss");
            throw new RuntimeException("该层没有boss");
        }

        return Result.success(monster);

    }
}