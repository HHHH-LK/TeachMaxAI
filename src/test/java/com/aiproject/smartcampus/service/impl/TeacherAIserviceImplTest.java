package com.aiproject.smartcampus.service.impl;

import com.aiproject.smartcampus.commons.client.Result;
import com.aiproject.smartcampus.model.functioncalling.toolutils.ExamMarkingToolUtils;
import com.aiproject.smartcampus.service.TeacherAIservice;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class TeacherAIserviceImplTest {

    @Autowired
    private TeacherAIservice teacherAIservice;

    @Test
    void aiTest() {

        Result<String> result = teacherAIservice.teacherCreateTest("帮我创建一套练习题难度适中", "1", "1");

        String data = result.getData();

        System.out.println(data);

    }


}