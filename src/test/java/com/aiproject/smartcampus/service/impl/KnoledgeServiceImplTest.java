package com.aiproject.smartcampus.service.impl;

import com.aiproject.smartcampus.commons.client.Result;
import com.aiproject.smartcampus.service.KnoledgeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;

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

}