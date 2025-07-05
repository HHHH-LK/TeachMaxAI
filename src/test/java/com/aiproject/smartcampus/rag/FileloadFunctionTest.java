package com.aiproject.smartcampus.rag;

import com.aiproject.smartcampus.model.rag.FileloadFunction;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;


@SpringBootTest
class FileloadFunctionTest {

    @Autowired
    private FileloadFunction fileloadFunction;


    @Test
    void test() {

        fileloadFunction.documentsloade();


    }

  
}