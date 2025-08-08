package com.aiproject.smartcampus.model.functioncalling.toolutils;

import com.aiproject.smartcampus.pojo.po.Tower;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class TowerCreateToolUtilsTest {

    @Autowired
    private TowerCreateToolUtils towerCreateToolUtils;


    @Test
    void getPonintNameById() {

        Boolean towerByStudentIdAndCourseId = towerCreateToolUtils.createTowerByStudentIdAndCourseId("1", "7");

        System.out.println(Boolean.TRUE.equals(towerByStudentIdAndCourseId) ? "塔初始化成功" : "初始化失败");


    }

    @Test
    void getPonintNameById2() {

        Tower towerInfo = towerCreateToolUtils.getTowerInfo(3, "5");

    }


}