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
        String answer = ragChain.chat("帮我生成一份含高中数学所有知识点的试卷，题目数量为10道题，全部为简答题？", "5");
        System.out.println(answer);

    }

}