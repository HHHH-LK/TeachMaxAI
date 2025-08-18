package com.aiproject.smartcampus.service.impl;

import com.aiproject.smartcampus.commons.client.Result;
import com.aiproject.smartcampus.service.RankingListService;
import com.aiproject.smartcampus.service.TowerService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
@SpringBootTest
class RankingListServiceImplTest {

    @Autowired
    private RankingListService rankingListService;
    @Autowired
    private TowerService towerService;

    @Test
    void getRankingListByStudentId() {

        Result<RankingListServiceImpl.SortUserInfo> userTowerSortUserInfo = rankingListService.getUserTowerSortUserInfo("1", "1");
        log.info("userTowerSortUserInfo={}", userTowerSortUserInfo.getData());

    }

    @Test
    void getRankingListByFloorId() {

        Result<List<TowerServiceImpl.SortedUser>> userSortByOneTowerId = towerService.getUserSortByOneTowerId("44");

        log.info("userSortByOneTowerId={}", userSortByOneTowerId.getData());
    }


}