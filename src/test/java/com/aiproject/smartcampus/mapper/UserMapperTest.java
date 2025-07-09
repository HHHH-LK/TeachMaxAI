package com.aiproject.smartcampus.mapper;

import com.aiproject.smartcampus.pojo.po.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;


@SpringBootTest
class UserMapperTest {

    @Autowired
    UserMapper userMapper;

    @Test
    void test(){

        User user = userMapper.selectById(1);
        System.out.println(user);

    }

    @Test
    void test1(){



    }

}