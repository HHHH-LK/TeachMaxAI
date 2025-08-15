package com.aiproject.smartcampus.service.impl;

import com.aiproject.smartcampus.commons.client.Result;
import com.aiproject.smartcampus.service.RankingListService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
@SpringBootTest
class RankingListServiceImplTest {

    @Autowired
    private RankingListService rankingListService;

    @Test
    void getRankingListByStudentId() {

        Result<RankingListServiceImpl.SortUserInfo> userTowerSortUserInfo = rankingListService.getUserTowerSortUserInfo("1", "1");
        log.info("userTowerSortUserInfo={}", userTowerSortUserInfo.getData());

    }


}