package com.aiproject.smartcampus.controller;

import com.aiproject.smartcampus.commons.client.Result;
import com.aiproject.smartcampus.pojo.dto.UserLoginDTO;
import com.aiproject.smartcampus.pojo.dto.UserPreliminaryRegisterDTO;
import com.aiproject.smartcampus.pojo.dto.UserRegisterDTO;
import com.aiproject.smartcampus.service.CommonService;
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

    /**
     * 用户信息填写
     * */
    @PostMapping("/register")
    @Operation(summary = "用户注册信息填写", description = "用户进入系统前的完整信息登记")
    public Result login(@RequestParam("token") String registrationToken, @RequestBody UserRegisterDTO userRegisterDTO) throws Exception {

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

    //todo 文件上传功能待完成
    @PostMapping("/fileupload")
    @Operation(summary = "文件上传", description = "文件上传功能")
    public Result upload(@RequestParam(value = "file") MultipartFile file) throws Exception {
        return commonService.upload(file);
    }

    //定时消息实时通知
    @GetMapping("/notificate")
    @Operation(summary = "定时消息实时通知",description = "定时消息实时通知")
    public Result notificate(@RequestParam(value = "userId") Integer userId) throws Exception {
        return commonService.notificateService(userId);
    }

    //发布系统通知
    @GetMapping("/broadcastNotification")
    @Operation(summary = "发布系统通知",description = "发布系统通知")
    public Result getAllUser(@RequestParam(value = "content")  String content) {
        return commonService.broadcastNotification(content);
    }

    /**
     * 消息确认
     * */
    @PostMapping("/notificationConfirm")
    @Operation(summary = "消息确认", description = "消息确认")
    public Result notificationConfirm(String charater,String userId , String notificatioonId,String content) throws Exception {

        return commonService.notificationConfirm(charater,userId,notificatioonId,content);
    }






}
