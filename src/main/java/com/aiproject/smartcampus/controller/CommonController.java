package com.aiproject.smartcampus.controller;

import com.aiproject.smartcampus.commons.Result;
import com.aiproject.smartcampus.pojo.dto.UserPreliminaryRegisterDTO;
import com.aiproject.smartcampus.pojo.dto.UserRegisterDTO;
import com.aiproject.smartcampus.service.CommonService;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * @program: SmartCampus
 * @description: 公用控制类
 * @author: lk
 * @create: 2025-05-20 09:18
 **/

@RestController
@RequestMapping("/common")
@RequiredArgsConstructor
public class CommonController {

    private final CommonService commonService;

    //用户初步注册

    @PostMapping("/preliminaryregister")
    @ApiOperation(value = "用户初步注册")
    public Result register(UserPreliminaryRegisterDTO userPreliminaryRegisterDTO) throws Exception {
        //注册流程
        return commonService.userPreliminaryregister(userPreliminaryRegisterDTO);

    }

    //用户信息填写
    @PostMapping("/register")
    @ApiOperation(value = "注册信息填写")
    public Result login( @RequestHeader("Authorization") String registrationToken,@RequestBody UserRegisterDTO userRegisterDTO) throws Exception {

        return commonService.userRegister(registrationToken,userRegisterDTO);
    }




}
