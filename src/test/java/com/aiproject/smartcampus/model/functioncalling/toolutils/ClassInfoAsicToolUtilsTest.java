package com.aiproject.smartcampus.model.functioncalling.toolutils;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ClassInfoAsicToolUtilsTest {

    @Autowired
    ClassInfoAsicToolUtils classInfoAsicToolUtils;

    @Test
    void createClassInfoAsic() {
        String classInfoAsic = classInfoAsicToolUtils.createClassInfoAsic("<UNK>");
    }


}