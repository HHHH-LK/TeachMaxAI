package com.aiproject.smartcampus.service;

import com.aiproject.smartcampus.commons.client.Result;
import com.aiproject.smartcampus.pojo.po.GameUser;
import org.apache.poi.ss.formula.functions.T;

public interface GameUserService {
    Result<GameUser> getGamePlayerInfo(String studentId);

    Result<T> setGamePlayerInfo(GameUser gameUser);

    Result<Integer> getGamePlayerLevel(String studentId);
}
