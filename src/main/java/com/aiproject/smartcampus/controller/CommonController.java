package com.aiproject.smartcampus.controller;

import com.aiproject.smartcampus.commons.Result;
import com.aiproject.smartcampus.pojo.dto.UserLoginDTO;
import com.aiproject.smartcampus.pojo.dto.UserPreliminaryRegisterDTO;
import com.aiproject.smartcampus.pojo.dto.UserRegisterDTO;
import com.aiproject.smartcampus.service.CommonService;
import io.swagger.annotations.ApiOperation;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
    @Operation(summary = "用户初步注册", description = "用户进入系统前的初步信息登记")
    public Result register(@RequestBody UserPreliminaryRegisterDTO userPreliminaryRegisterDTO) throws Exception {

        return commonService.userPreliminaryregister(userPreliminaryRegisterDTO);

    }

    //用户信息填写
    @PostMapping("/register")
    @Operation(summary = "用户注册信息填写", description = "用户进入系统前的完整信息登记")
    public Result login(@RequestHeader("token") String registrationToken, @RequestBody UserRegisterDTO userRegisterDTO) throws Exception {

        return commonService.userRegister(registrationToken, userRegisterDTO);
    }

    //用户登录功能
    @GetMapping("/login")
    @Operation(summary = "用户登录", description = "用户登录功能")
    public Result login(UserLoginDTO userLoginDTO) throws Exception {
        return commonService.userLogin(userLoginDTO);
    }

    //用户退出功能
    @GetMapping("/logout")
    @Operation(summary = "用户退出", description = "用户退出功能")
    public Result logout(@RequestParam(value = "token") String token)  {
        return commonService.userLogout(token);
    }

    //文件上传功能
    @PostMapping("/fileupload")
    @Operation(summary = "文件上传", description = "文件上传功能")
    public Result upload(@RequestParam(value = "file") MultipartFile file) throws Exception {
        return commonService.upload(file);
    }


}
