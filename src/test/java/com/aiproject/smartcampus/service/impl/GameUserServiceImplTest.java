package com.aiproject.smartcampus.service.impl;

import com.aiproject.smartcampus.commons.client.Result;
import com.aiproject.smartcampus.pojo.po.GameUser;
import com.aiproject.smartcampus.service.GameUserService;
import org.apache.poi.ss.formula.functions.T;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;


@SpringBootTest
class GameUserServiceImplTest {


    @Autowired
    private GameUserService gameUserService;


    @Test
    void getGameUserById() {
        GameUser gameUser = new GameUser();
        gameUser.setGameName("aaa");
        gameUser.setExp(11);
        gameUser.setLevel(11);
        gameUser.setStudentId(1);

        Result<T> gamePlayerLevel = gameUserService.setGamePlayerInfo(gameUser);

    }

    @Test
    void getGameUserByGameId() {
        Result<Integer> gamePlayerLevel = gameUserService.getGamePlayerLevel("1");
        System.out.println(gamePlayerLevel.getData());

    }

}