package com.aiproject.smartcampus.model;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;


@SpringBootTest
class AgentTest {

    @Autowired
    private Agent agent;

    @Test
    void start() {
        String answer = agent.start("高考试卷题型一般怎么分布");
        System.out.println(answer);
    }

}