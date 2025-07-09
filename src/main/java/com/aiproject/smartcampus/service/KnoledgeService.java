package com.aiproject.smartcampus.service;

import com.aiproject.smartcampus.commons.client.Result;
import com.aiproject.smartcampus.pojo.dto.HavingTPointDTO;

import java.util.List;

public interface KnoledgeService {

    /**
    * 查询所有错误的知识点
    * */
    Result getALlOKKnowlegePoint();

    /**
     * 根据知识点id进行查询知识点详细信息
     * */
    Result getgetKnowlegeInformationBypointId(String pointId);

    /**
     * agent多知识点生成错误练习
     * */
    Result createListTestByagent(List<String> pointIds,String courseId,String chapter,String decprection);

    /**
     * 根据带权知识点进行自定义生成
     * */
    Result createListTestUsingTByAgent(List<HavingTPointDTO> pointIds,String courseId,String chapter,String decprection);
}
