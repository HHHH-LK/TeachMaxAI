package com.aiproject.smartcampus.model.intent.handler.impl;

import com.aiproject.smartcampus.SmartCampusApplication;
import com.aiproject.smartcampus.model.handler.impl.SeptIntentRagHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;


@SpringBootTest(classes = SmartCampusApplication.class)
class SeptIntentRagHandlerTest {

    @Autowired
    private SeptIntentRagHandler septIntentRagHandler;

    @Test
    void handle() {

        String anser = septIntentRagHandler.run("查询我十四周所有的课程", null);
        System.out.println(anser);

    }

}