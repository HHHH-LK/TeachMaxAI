package com.aiproject.smartcampus.commons.utils;

import com.aiproject.smartcampus.pojo.bo.ExamCorrectAnswer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class JsonUtilsTest {

    @Test
    void getJsonString() throws Exception {
        String json = "{ \"answer\": true, \"explanation\": \"虚拟内存通过磁盘交换技术，使程序可以使用比物理内存更大的地址空间\" }";
        Object jsonObject = JsonUtils.toJsonObject(json, ExamCorrectAnswer.class);
        ExamCorrectAnswer jsonObject1 = (ExamCorrectAnswer) jsonObject;
        System.out.println(jsonObject1.getAnswer());
        System.out.println(jsonObject1.getExplanation());
    }

}