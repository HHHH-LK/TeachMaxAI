package com.aiproject.smartcampus.service.impl;

import com.aiproject.smartcampus.commons.client.Result;
import com.aiproject.smartcampus.service.StudentTeacherChatService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;


@SpringBootTest
@Slf4j
class StudentTeacherChatServiceImplTest {

    @Autowired
    private StudentTeacherChatService studentTeacherChatService;

    @Test
    void setConnection() {

        Result<Long> longResult = studentTeacherChatService.setConnection("1", "9");
        assertTrue(longResult.isSuccess());

        log.info(longResult.getData().toString());

    }
}