package com.aiproject.smartcampus.model;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Duration;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;


@SpringBootTest
class AgentTest {

    @Autowired
    private Agent agent;

    @Test
    void start() {
        long l = System.currentTimeMillis();
        LocalDateTime now = LocalDateTime.now();
        System.out.println(now);
        System.out.println(l);
        String answer = agent.start("湖南有什么历史");
        System.out.println(answer);
        LocalDateTime now2 = LocalDateTime.now();
        long l1 = System.currentTimeMillis();
        System.out.println(l1 - l);
        System.out.println( Duration.between(now, now2));


    }




}