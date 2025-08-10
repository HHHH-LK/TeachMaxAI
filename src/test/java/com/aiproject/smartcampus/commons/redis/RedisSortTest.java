package com.aiproject.smartcampus.commons.redis;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class RedisSortTest {

    @Autowired
    private RedisSort redisSort;

    @Test
    void test(){

        redisSort.getSortedList("2");

    }


}