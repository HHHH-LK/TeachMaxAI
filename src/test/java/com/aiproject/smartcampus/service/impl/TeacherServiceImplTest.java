package com.aiproject.smartcampus.service.impl;

import com.aiproject.smartcampus.commons.client.Result;
import com.aiproject.smartcampus.service.TeacherService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class TeacherServiceImplTest {

    @Autowired
    TeacherService teacherService;

    @Test
    void getAllClassInfo(){

        Result allClassInfo = teacherService.getAllClassInfo("1");

        Object data = allClassInfo.getData();

        System.out.println(data);

    }

    @Test
    void getClassInfo(){

        Result theMaxUncorrectPoint = teacherService.getTheMaxUncorrectPoint("1");
        Object data = theMaxUncorrectPoint.getData();
        System.out.println(data);

    }



}