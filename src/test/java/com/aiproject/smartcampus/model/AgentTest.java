package com.aiproject.smartcampus.model;

import com.aiproject.smartcampus.commons.utils.UserLocalThreadUtils;
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


    }

    @Test
    void setChatAgent(){

        UserLocalThreadUtils.setUserId("1");
        String ss = chatAgent.start("我叫什么");
        System.out.println(ss);

    }






}