package com.aiproject.smartcampus.service.impl;

import com.aiproject.smartcampus.commons.client.Result;
import com.aiproject.smartcampus.pojo.dto.HavingTPointDTO;
import com.aiproject.smartcampus.service.KnoledgeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;


@SpringBootTest
class KnoledgeServiceImplTest {

    @Autowired
    KnoledgeService knoledgeService;

    @Test
    void setKnoledgeService() {

        Result aLlOKKnowlegePoint = knoledgeService.getALlOKKnowlegePoint();
        System.out.println(aLlOKKnowlegePoint.getData());

    }

    @Test
    void getKnoledgeInformationById() {
        Result result = knoledgeService.getgetKnowlegeInformationBypointId("1");
        System.out.println(result.getData());
    }

    @Test
    void getKnoledgeInformationById2() {
        ArrayList<String> objects = new ArrayList<>();
        objects.add("1");
        objects.add("2");
        objects.add("3");
        objects.add("4");
        objects.add("5");
        objects.add("6");
        objects.add("7");
        objects.add("8");
        objects.add("9");
        objects.add("10");
        objects.add("11");

        Result listTestByagent = knoledgeService.createListTestByagent(objects);
        System.out.println(listTestByagent.getData());

    }

    @Test
    void getKnoledgeInformationById3() {

        // 输入数据
        List<HavingTPointDTO> userWeights = Arrays.asList(
                new HavingTPointDTO(1, 5),  // 知识点1，权重5
                new HavingTPointDTO(2, 3),  // 知识点2，权重3
                new HavingTPointDTO(3, 8),  // 知识点3，权重8
                new HavingTPointDTO(4, 9),
                new HavingTPointDTO(5, 9),
                new HavingTPointDTO(6, 10),
                new HavingTPointDTO(7, 1),
                new HavingTPointDTO(8, 2),
                new HavingTPointDTO(9,1),
                new HavingTPointDTO(10, 10),
                new HavingTPointDTO(11,5)

        );

        Result<String> result = knoledgeService.createListTestUsingTByAgent(userWeights);

        System.out.println(result.getData());

    }

}