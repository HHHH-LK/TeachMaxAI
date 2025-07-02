package com.aiproject.smartcampus.service.impl;

import com.aiproject.smartcampus.commons.client.Result;
import com.aiproject.smartcampus.commons.utils.UserLocalThreadUtils;
import com.aiproject.smartcampus.mapper.KnowlegeMapper;
import com.aiproject.smartcampus.mapper.UserMapper;
import com.aiproject.smartcampus.pojo.dto.UserLoginDTO;
import com.aiproject.smartcampus.pojo.po.User;
import com.aiproject.smartcampus.service.KnoledgeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * @program: SmartCampus
 * @description:
 * @author: lk_hhh
 * @create: 2025-07-02 18:23
 **/

@Slf4j
@RequiredArgsConstructor
@Service
public class KnoledgeServiceImpl implements KnoledgeService {

    private final KnowlegeMapper knowledgeMapper;
    private final UserMapper userMapper;

    @Override
    public Result getAllNotMasterKnoledge() {

        //  获取出当前用户信息
        User userInfo = UserLocalThreadUtils.getUserInfo();
        // 获取当前学生的学号


        return null;
    }
}