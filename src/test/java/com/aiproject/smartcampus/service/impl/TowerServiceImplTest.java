package com.aiproject.smartcampus.service.impl;

import com.aiproject.smartcampus.service.TowerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class TowerServiceImplTest {

    @Autowired
    private TowerService towerService;

    @Test
    void createTower() {

        towerService.loadTest("20","83","1","1");

    }

}