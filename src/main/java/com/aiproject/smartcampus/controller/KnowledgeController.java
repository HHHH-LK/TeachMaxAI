package com.aiproject.smartcampus.controller;

import com.aiproject.smartcampus.commons.client.Result;
import com.aiproject.smartcampus.pojo.bo.SimpleKnowledgeAnalysisBO;
import com.aiproject.smartcampus.pojo.dto.HavingTPointDTO;
import com.aiproject.smartcampus.service.KnoledgeService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.annotations.Param;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @program: SmartCampus
 * @description: 知识点管理控制器
 * @author: lk_hhh
 * @create: 2025-07-02 18:19
 **/

@Api(tags = "知识点管理")
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/knowledge")
public class KnowledgeController {

    private final KnoledgeService knoledgeService;

    @GetMapping("/checkAllNOtCruuent")
    public Result checkAllNotCruuent(){

        return knoledgeService.getALlOKKnowlegePoint();

    }

    @GetMapping("/getKnowlegeBypointId")
    public Result getKnowlegeById(@Param(value = "pointId") String pointId){

        return knoledgeService.getgetKnowlegeInformationBypointId(pointId);

    }

    //生成相关知识点


    //智能选取知识点进行生成
    @PostMapping("/ai/createTest")
    public Result createListTestByAgent(@RequestBody List<String> pointIds){

        log.info("pointIds:{}",pointIds);
        return knoledgeService.createListTestByagent(pointIds);

    }

    //根据所设定权重进行知识点题目的生成
    @PostMapping("/ai/t/createTest")
    public Result createListTestUsingTByAgent(@RequestBody List<HavingTPointDTO> pointIds){

        log.info("pointIds:{}",pointIds);
        return knoledgeService.createListTestUsingTByAgent(pointIds);
    }

}