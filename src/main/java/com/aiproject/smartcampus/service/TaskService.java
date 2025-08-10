package com.aiproject.smartcampus.service;


import com.aiproject.smartcampus.commons.client.Result;
import com.aiproject.smartcampus.pojo.po.Task;

import java.util.List;

public interface TaskService {
    Result<Task> getTowerFloorTask(String towerFloorId);

    Result<List<Integer>> getTaskPointIds(String towerFloorId);
}
