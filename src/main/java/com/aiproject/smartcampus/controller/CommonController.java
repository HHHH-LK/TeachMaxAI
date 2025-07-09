package com.aiproject.smartcampus.controller;

import com.aiproject.smartcampus.commons.client.Result;
import com.aiproject.smartcampus.pojo.dto.LoginDTO;
import com.aiproject.smartcampus.pojo.dto.PasswordResetDTO;
import com.aiproject.smartcampus.pojo.dto.PasswordResetVerificationDTO;
import com.aiproject.smartcampus.pojo.dto.RegisterDTO;
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
    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "支持用户名/邮箱/手机号登录")
    public Result login(@RequestBody LoginDTO loginDTO) {
        return commonService.login(loginDTO);
    }

    @PostMapping("/register")
    @Operation(summary = "用户注册", description = "学生/教师通用注册")
    public Result register(@RequestBody RegisterDTO registerDTO) {
        return commonService.register(registerDTO);
    }

//    @PostMapping("/password/verify")
//    @Operation(summary = "密码找回验证", description = "验证手机/邮箱并发送验证码")
//    public Result verifyResetPassword(@RequestBody PasswordResetVerificationDTO dto) {
//        return commonService.sendPasswordResetCode(dto);
//    }

    @PostMapping("/password/reset")
    @Operation(summary = "密码重置", description = "验证后设置新密码")
    public Result resetPassword(@RequestBody PasswordResetDTO dto) {
        return commonService.resetPassword(dto);
    }

    @PostMapping("/logout")
    public Result logout() {

        return Result.success("功能还未完成");
    }


}
