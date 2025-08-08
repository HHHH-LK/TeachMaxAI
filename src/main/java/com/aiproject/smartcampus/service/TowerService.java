package com.aiproject.smartcampus.service;


import com.aiproject.smartcampus.commons.client.Result;
import com.aiproject.smartcampus.pojo.po.Tower;
import com.aiproject.smartcampus.pojo.po.TowerFloor;
import com.aiproject.smartcampus.service.impl.TowerServiceImpl;
import org.springframework.data.redis.core.ZSetOperations;

import java.util.List;
import java.util.Set;

public interface TowerService {
    Result<Boolean> createTowerByAgent(String studentId, String courseId);

    Result<List<Tower>> getTowerByStudentId(String studentId);

    Result<List<TowerFloor>> getTowerFloorByTowerId(String towerId);

    Result<List<TowerServiceImpl.SortedUser>> getUserSortByOneTowerId(String towerId);

    Result<List<TowerServiceImpl.SortedUser>> getTotleSorted();

    Result<String> getTowerStoryByTowerId(String towerId);

    Result<String> getTowerFloorStoryByTowerFloorId(String towerFloorId);

    Result<TowerFloor> getTowerFloorInfoByFloorId(String towerFloorId);

    Result setIsPass(String towerFloorId, Integer isPass);
}
