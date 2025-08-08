package com.aiproject.smartcampus.service;


import com.aiproject.smartcampus.commons.client.Result;

public interface TowerService {
    Result<Boolean> createTowerByAgent(String studentId, String courseId);
}
