package com.aiproject.smartcampus.mapper;

import com.aiproject.smartcampus.pojo.po.Tower;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class TowerMapperTest {

    @Autowired
    TowerMapper towerMapper;

    @Test
    void towerInfoTest() {

        Tower tower = towerMapper.selectById(1);
        System.out.println(tower);

    }

}