package com.aiproject.smartcampus.controller;

import com.aiproject.smartcampus.commons.client.Result;
import com.aiproject.smartcampus.pojo.po.Tower;
import com.aiproject.smartcampus.pojo.po.TowerFloor;
import com.aiproject.smartcampus.service.TowerService;
import com.aiproject.smartcampus.service.impl.TaskServiceImpl;
import com.aiproject.smartcampus.service.impl.TowerServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

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


    /**
     * 根据学生和课程定制化生成战斗塔
     */
    @PostMapping("/createByAgent")
    public Result<Boolean> createTowerByAgent(@RequestParam(value = "studentId") String studentId, @RequestParam(value = "courseId") String courseId) {

        return towerService.createTowerByAgent(studentId, courseId);
    }

    /**
     * 所有塔列表展示
     */
    @GetMapping("/getTowerBystudentId")
    public Result<List<Tower>> getTowerByStudentId(@RequestParam(value = "studentId") String studentId) {

        return towerService.getTowerByStudentId(studentId);
    }

    /**
     * 获取对应塔下的所有塔层信息
     */
    @GetMapping("/getTowerInfoByTowerId")
    public Result<List<TowerFloor>> getTowerFloorByTowerId(@RequestParam(value = "towerId") String towerId) {

        return towerService.getTowerFloorByTowerId(towerId);

    }

    /**
     * 分榜排行榜（只有用户到指定课程的塔时才进行修改层数）
     */
    @GetMapping("/getTowerSortByCourseId")
    public Result<List<TowerServiceImpl.SortedUser>> getUserSortByOneTowerId(@RequestParam(value = "courseId") String courseId) {

        return towerService.getUserSortByOneTowerId(courseId);
    }

    /**
     * 总榜排行榜（后续修改塔层时用户每个塔都进行修改层数）
     */
    @GetMapping("/getTotleSort")
    public Result<List<TowerServiceImpl.SortedUser>> getTotleSorted() {

        return towerService.getTotleSorted();

    }

    /**
     * 查询主塔的背景故事
     */
    @GetMapping("/getTowerStoryByTowerId")
    public Result<String> getTowerStory(@RequestParam(value = "towerId") String towerId) {

        return towerService.getTowerStoryByTowerId(towerId);
    }

    /**
     * 查询塔层的故事情节
     */
    @GetMapping("/getTowerFloorStoryByTowerFloorId")
    public Result<String> getTowerFloorStory(@RequestParam(value = "towerFloorId") String towerFloorId) {

        return towerService.getTowerFloorStoryByTowerFloorId(towerFloorId);

    }


    /**
     * 进入学生对应的塔层(根据tower_id 查询塔层信息)
     */
    @GetMapping("/getTowerFloorInfoByFloorId")
    public Result<TowerFloor> getTowerFloorInfo(@RequestParam(value = "towerFloorId") String towerFloorId) {

        return towerService.getTowerFloorInfoByFloorId(towerFloorId);
    }


    /**
     * 修改塔层状态(是否通过）
     */
    @PostMapping("/setIsPass")
    public Result setIsPass(@RequestParam(value = "towerFloorId") String towerFloorId, Integer isPass) {

        return towerService.setIsPass(towerFloorId, isPass);
    }


    /**
     * 进入塔层时异步加载对应的题目（存入redis中）并定时同步
     * */
    @PostMapping("/load")
    public Result loadTest(@RequestParam(value = "towerId") String towerId,@RequestParam(value = "floorId")String floorId,
                           @RequestParam(value = "courseId")String courseId,@RequestParam(value = "studentId")String studentId) {

        return towerService.loadTest(towerId,floorId,courseId,studentId);
    }


}