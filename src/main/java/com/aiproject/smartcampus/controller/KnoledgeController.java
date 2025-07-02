package com.aiproject.smartcampus.controller;

import com.aiproject.smartcampus.commons.client.Result;
import com.aiproject.smartcampus.service.KnoledgeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @program: SmartCampus
 * @description: 知识点管理
 * @author: lk_hhh
 * @create: 2025-07-02 18:19
 **/

@RestController
@RequiredArgsConstructor
@RequestMapping("/knoledge")
public class KnoledgeController {

    private final KnoledgeService knoledgeService;

    /**
     * 查询所有错误知识点
     * */
    @GetMapping("")
    public Result getAllNotMasterKnoledge(){


        return knoledgeService.getAllNotMasterKnoledge();
    }



}