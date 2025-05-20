package com.aiproject.smartcampus.rag;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class RAGChainTest {

    @Autowired
    private RAGChain ragChain;

    @Test
    void test() {
        String answer = ragChain.chat("北京今天天气怎么样", "5");
        System.out.println(answer);

    }

}