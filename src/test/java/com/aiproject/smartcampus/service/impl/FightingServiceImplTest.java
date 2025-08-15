package com.aiproject.smartcampus.service.impl;

import com.aiproject.smartcampus.commons.client.Result;
import com.aiproject.smartcampus.service.FightingService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;


@SpringBootTest
@Slf4j
class FightingServiceImplTest {

    @Autowired
    private FightingService fightingService;


    @Test
    void getRequireExp() {

        Result<Integer> levelAdds = fightingService.getLevelAdds("1", "3000");
        log.info("levelAdds: {}", levelAdds.getData());


    }

    @Test
    void getRequireExp2() {

        for (int i = 1; i <= 10; i++) {
            Result<Long> requireExp = fightingService.getRequireExp(String.valueOf(i));
            log.info("requireExp: {}", requireExp.getData());
        }

    }


}