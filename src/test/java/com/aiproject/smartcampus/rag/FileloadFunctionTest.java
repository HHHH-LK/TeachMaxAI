package com.aiproject.smartcampus.rag;


import com.aiproject.smartcampus.model.rag.FileloadFunction;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;


@SpringBootTest
class FileloadFunctionTest {

    @Autowired
    private FileloadFunction fileloadFunction;


    @Test
    void test() {

        fileloadFunction.processDocumentDynamically(new File("/Users/lk_hhh/Documents/ss/TeacherMaxAI/documents/java_programming_guide.md"));


    }

  
}