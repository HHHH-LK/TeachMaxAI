package com.aiproject.smartcampus.controller;

import com.aiproject.smartcampus.service.StudentService;
import io.swagger.annotations.Api;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * @program: SmartCampus
 * @description: 用户控制层
 * @author: lk
 * @create: 2025-05-19 15:22
 **/

@RestController
@RequestMapping("/usr")
@Slf4j
@RequiredArgsConstructor
@Api(tags = "用户管理")
public class UserController {

    private final StudentService studentService;






}
