package com.aiproject.smartcampus.model.functioncalling.toolutils;

import com.aiproject.smartcampus.pojo.vo.ExamCreationResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ExamCreaterToolUtilsTest {

    @Autowired
    private ExamCreaterToolUtils examCreaterToolUtils;

    @Test
    void getCourseId() {

        ExamCreationResult exam = examCreaterToolUtils.createExam("帮我生成一道大题", "2");
        System.out.println(exam);

    }

}