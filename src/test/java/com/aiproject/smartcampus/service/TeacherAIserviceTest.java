package com.aiproject.smartcampus.service;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Slf4j
class TeacherAIserviceTest {

    @Autowired
    private TeacherAIservice teacherAIservice;


    @Test
    void getCourseId(){

        String data = teacherAIservice.aiclassAiayaisc("1").getData();

        log.debug(data);

    }


}