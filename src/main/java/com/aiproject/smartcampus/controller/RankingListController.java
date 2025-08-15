package com.aiproject.smartcampus.controller;

import com.aiproject.smartcampus.commons.client.Result;
import com.aiproject.smartcampus.service.RankingListService;
import com.aiproject.smartcampus.service.impl.RankingListServiceImpl;
import com.aiproject.smartcampus.service.impl.TowerServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.formula.functions.T;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @program: TeacherMaxAI
 * @description: 排行榜
 * @author: lk_hhh
 * @create: 2025-08-06 11:28
 **/
@RestController
@RequestMapping("/rankinglist")
@Slf4j
@RequiredArgsConstructor
public class RankingListController {

    private final TowerServiceImpl towerService;
    private final RankingListService rankingListService;

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
     * 查看玩家的总榜中的详细信息
     */
    @GetMapping("/getUserTotalTowerInfo")
    public Result<RankingListServiceImpl.SortUserInfo> getUserTotalTowerInfo(@RequestParam String studentId) {

        return rankingListService.getUserTowerInfo(studentId);
    }

    /**
     * 查看玩家对应塔的排行榜
     * */
    @GetMapping("/getUserTowerSortUserInfo")
    public Result<RankingListServiceImpl.SortUserInfo> getUserTowerSortUserInfo(@RequestParam String studentId,@RequestParam String towerId) {

        return rankingListService.getUserTowerSortUserInfo(studentId,towerId);
    }


}