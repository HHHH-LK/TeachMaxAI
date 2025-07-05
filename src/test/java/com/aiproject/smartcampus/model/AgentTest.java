package com.aiproject.smartcampus.model;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Duration;
import java.time.LocalDateTime;


@SpringBootTest
class AgentTest {

    @Autowired
    private ChatAgent chatAgent;

    @Test
    void start() {
        long l = System.currentTimeMillis();
        LocalDateTime now = LocalDateTime.now();
        System.out.println(now);
        System.out.println(l);
        String answer = chatAgent.start("请你基于刚刚生成的方案为我出一些函数与极限练习题，帮我快速掌握重点任务" );
        System.out.println(answer);
        LocalDateTime now2 = LocalDateTime.now();
        long l1 = System.currentTimeMillis();
        System.out.println(l1 - l);
        System.out.println( Duration.between(now, now2));


    }




}