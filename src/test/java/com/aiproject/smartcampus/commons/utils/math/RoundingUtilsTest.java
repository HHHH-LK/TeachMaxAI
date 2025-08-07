package com.aiproject.smartcampus.commons.utils.math;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class RoundingUtilsTest {

    @Test
    void getKonwledgeNameById() {

        Double v = RoundingUtils.round(3.9999);

        System.out.println(v);

    }


}