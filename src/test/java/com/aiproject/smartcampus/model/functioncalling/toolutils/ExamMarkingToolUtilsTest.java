package com.aiproject.smartcampus.model.functioncalling.toolutils;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest
@Slf4j
class ExamMarkingToolUtilsTest {

    @Autowired
    private ExamMarkingToolUtils examMarkingToolUtils;

    @Test
    void test(){

        String mark = examMarkingToolUtils.mark("1", "1");

        log.info("Marking result: {}", mark);

    }

}