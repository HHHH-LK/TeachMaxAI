package com.aiproject.smartcampus.commons.sort;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class RedisSortTest {

    @Autowired
    private RedisSort redisSort;

    @Test
    void test(){

        redisSort.getSortedList("2");

    }


}