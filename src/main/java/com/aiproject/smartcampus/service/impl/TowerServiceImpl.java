package com.aiproject.smartcampus.service.impl;

import com.aiproject.smartcampus.commons.client.Result;
import com.aiproject.smartcampus.commons.sort.RedisSort;
import com.aiproject.smartcampus.mapper.CourseEnrollmentMapper;
import com.aiproject.smartcampus.mapper.GameUserMapper;
import com.aiproject.smartcampus.mapper.TowerFloorMapper;
import com.aiproject.smartcampus.mapper.TowerMapper;
import com.aiproject.smartcampus.model.functioncalling.toolutils.TowerCreateToolUtils;
import com.aiproject.smartcampus.pojo.po.CourseEnrollment;
import com.aiproject.smartcampus.pojo.po.GameUser;
import com.aiproject.smartcampus.pojo.po.Tower;
import com.aiproject.smartcampus.pojo.po.TowerFloor;
import com.aiproject.smartcampus.service.TowerService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @program: TeacherMaxAI
 * @description:
 * @author: lk_hhh
 * @create: 2025-08-07 16:54
 **/

@Service
@Slf4j
@RequiredArgsConstructor
public class TowerServiceImpl implements TowerService {

    private final TowerCreateToolUtils towerCreateToolUtils;
    private final TowerMapper towerMapper;
    private final CourseEnrollmentMapper courseEnrollmentMapper;
    private final TowerFloorMapper towerFloorMapper;
    private final RedisSort redisSort;
    private final GameUserMapper gameUserMapper;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SortedUser {

        String studentId;
        String studentName;
        Double maxTowerFloorNo;
        Integer studentLevel;

    }


    @Override
    public Result<Boolean> createTowerByAgent(String studentId, String courseId) {

        //首先判断该学生是否选了这门课
        LambdaQueryWrapper<CourseEnrollment> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CourseEnrollment::getStudentId, studentId);
        queryWrapper.eq(CourseEnrollment::getCourseId, courseId);
        CourseEnrollment courseEnrollment = courseEnrollmentMapper.selectOne(queryWrapper);
        if (courseEnrollment == null) {
            return Result.error("学生未选择该课程");
        }

        //智能生成
        Boolean towerByStudentIdAndCourseId = towerCreateToolUtils.createTowerByStudentIdAndCourseId(studentId, courseId);

        if (!towerByStudentIdAndCourseId) {
            log.error("创建塔失败");
            return Result.error("创建个性化塔失败");
        }

        return Result.success();
    }

    @Override
    public Result<List<Tower>> getTowerByStudentId(String studentId) {

        LambdaQueryWrapper<Tower> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Tower::getStudentId, studentId);
        List<Tower> towers = towerMapper.selectList(queryWrapper);

        return Result.success(Objects.requireNonNullElseGet(towers, ArrayList::new));

    }

    @Override
    public Result<List<TowerFloor>> getTowerFloorByTowerId(String towerId) {

        LambdaQueryWrapper<TowerFloor> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TowerFloor::getTowerId, towerId);
        List<TowerFloor> towerFloors = towerFloorMapper.selectList(queryWrapper);

        return Result.success(Objects.requireNonNullElseGet(towerFloors, ArrayList::new));
    }

    @Override
    public Result<List<SortedUser>> getUserSortByOneTowerId(String courseId) {

        Set<ZSetOperations.TypedTuple<String>> sortedList = redisSort.getSortedList(courseId);

        List<SortedUser> sortedUserList = toSortedUserList(sortedList);

        return Result.success(sortedUserList);
    }

    @Override
    public Result<List<SortedUser>> getTotleSorted() {

        Set<ZSetOperations.TypedTuple<String>> totleSortedList = redisSort.getTotleSortedList();

        List<SortedUser> sortedUsers = toSortedUserList(totleSortedList);

        return Result.success(sortedUsers);

    }

    @Override
    public Result<String> getTowerStoryByTowerId(String towerId) {

        LambdaQueryWrapper<Tower> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Tower::getTowerId, towerId);
        Tower tower = towerMapper.selectOne(queryWrapper);
        if (tower == null) {
            log.error("塔层不存在");
            return Result.error("塔层不存在");
        }

        String description = tower.getDescription();

        if (description == null) {
            log.error("故事情节为空");
            return Result.error("故事情节为空");
        }

        return Result.success(description);
    }

    @Override
    public Result<String> getTowerFloorStoryByTowerFloorId(String towerFloorId) {

        LambdaQueryWrapper<TowerFloor> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TowerFloor::getFloorId, towerFloorId);
        TowerFloor towerFloor = towerFloorMapper.selectOne(queryWrapper);
        if (towerFloor == null) {
            log.error("该层不存在");
            return Result.error("塔层不存在");
        }

        String description = towerFloor.getDescription();

        if (description == null) {
            log.error("故事情节为空");
            return Result.error("故事情节为空");
        }

        return Result.success(description);
    }

    @Override
    public Result<TowerFloor> getTowerFloorInfoByFloorId(String towerFloorId) {

        LambdaQueryWrapper<TowerFloor> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TowerFloor::getFloorId, towerFloorId);
        TowerFloor towerFloor = towerFloorMapper.selectOne(queryWrapper);
        if (towerFloor == null) {
            log.error("塔层为空");
            return Result.error("塔层为空");
        }

        return Result.success(towerFloor);
    }

    @Override
    public Result setIsPass(String towerFloorId, Integer isPass) {

        LambdaUpdateWrapper<TowerFloor> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(TowerFloor::getTowerId, towerFloorId);
        updateWrapper.set(TowerFloor::getIsPass, isPass);
        int update = towerFloorMapper.update(updateWrapper);
        if (update == 0) {
            log.error("修改通过状态失败");
            return Result.error("修改通过状态失败");
        }

        return Result.success();
    }


    /**
     * 将获取的排名的studentId 转换成排行榜上的信息
     */
    private List<SortedUser> toSortedUserList(Set<ZSetOperations.TypedTuple<String>> totleSortedList) {

        List<SortedUser> sortedUsers = totleSortedList.stream().map(a -> {
            SortedUser sortedUser = new SortedUser();
            Double maxTowerFloorNo = a.getScore();
            String studentId = a.getValue();
            sortedUser.setMaxTowerFloorNo(maxTowerFloorNo);
            sortedUser.setStudentId(studentId);
            return sortedUser;
        }).toList();

        // 批量查询所有学生信息
        List<String> studentIds = sortedUsers.stream().map(SortedUser::getStudentId).distinct().collect(Collectors.toList());

        LambdaQueryWrapper<GameUser> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(GameUser::getStudentId, studentIds);
        List<GameUser> gameUsers = gameUserMapper.selectList(queryWrapper);

        // 建立 studentId -> GameUser 的映射
        Map<String, GameUser> gameUserMap = gameUsers.stream().collect(Collectors.toMap(g -> String.valueOf(g.getStudentId()), g -> g));

        // 填充名字和等级
        for (SortedUser su : sortedUsers) {
            GameUser gu = gameUserMap.get(su.getStudentId());
            if (gu != null) {
                su.setStudentName(gu.getGameName());
                su.setStudentLevel(gu.getLevel());
            } else {
                log.error("用户不存在，studentId={}", su.getStudentId());
                throw new RuntimeException("用户不存在");
            }
        }

        return sortedUsers;
    }


}