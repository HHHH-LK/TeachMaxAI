package com.aiproject.smartcampus.rag;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;


@SpringBootTest
class RagAPITest {

    @Autowired
    private RagAPI ragAPI;

    @Test
    void test() {
        String result = ragAPI.doEasyRag("1", "湖南今天天气怎么样");
        System.out.println(result);

    }

}