package com.aiproject.smartcampus.controller;

import com.aiproject.smartcampus.SmartCampusApplication;
import com.aiproject.smartcampus.commons.client.Result;
import com.aiproject.smartcampus.pojo.vo.ExamCreationResult;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = SmartCampusApplication.class)
@Slf4j
class TeacherAIControllerTest {

    @Autowired
    private TeacherAIController teacherAIController;


    @Test
    void teacherAIControllerTest(){

        Result<ExamCreationResult> intelligentExam = teacherAIController.createIntelligentExam("1", "1");
        ExamCreationResult data = intelligentExam.getData();
        log.info("<UNK>:{}",data);

    }

}