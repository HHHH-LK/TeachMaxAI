package com.aiproject.smartcampus.controller;

import com.aiproject.smartcampus.commons.client.Result;
import com.aiproject.smartcampus.pojo.po.Student;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Slf4j
class StudentControllerTest {

    @Autowired
    StudentController studentController;

    @Test
    void updateStudent() {
        Result<String> stringResult = studentController.academicAnalysis("1");
        String data = stringResult.getData();
        log.info("data: {}", data);

    }


}