package com.aiproject.smartcampus.rag;

import com.aiproject.smartcampus.model.rag.ChatChain;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;


@SpringBootTest
class RAGChainTest {

    @Autowired
    private ChatChain chatChain;


    @Test
    void chat() {

        String answer = chatChain.chat("我要上的课程中学分最高的三门课是那三门", "1");
        System.out.println(answer);
    }


}