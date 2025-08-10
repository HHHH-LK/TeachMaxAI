package com.aiproject.smartcampus.service.impl;

import com.aiproject.smartcampus.commons.client.Result;
import com.aiproject.smartcampus.mapper.TaskMapper;
import com.aiproject.smartcampus.service.TaskService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class TaskServiceImplTest {

    @Autowired
    private TaskService taskService;

    @Test
    void getTowerFloorTask() {


        Result<List<Integer>> taskPointIds = taskService.getTaskPointIds("45");

        System.out.println(taskPointIds.getData());

    }


}