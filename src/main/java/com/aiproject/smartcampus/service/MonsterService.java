package com.aiproject.smartcampus.service;


import com.aiproject.smartcampus.commons.client.Result;
import com.aiproject.smartcampus.pojo.po.Monster;

public interface MonsterService {


    Result<Monster> getBossInfo(String floorId);
}
