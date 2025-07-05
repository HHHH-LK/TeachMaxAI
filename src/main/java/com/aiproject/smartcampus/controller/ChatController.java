package com.aiproject.smartcampus.controller;


import com.aiproject.smartcampus.commons.client.Result;
import com.aiproject.smartcampus.model.ChatAgent;
import com.aiproject.smartcampus.pojo.dto.ChatDTO;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
@Slf4j
public class ChatController {

    private final ChatAgent chatAgent;

    @PostMapping("/userchat")
    public Result userchat(@RequestBody ChatDTO chatDTO) {
        log.info("用户问题：{}", chatDTO.getQuestion());
        String start = chatAgent.start(chatDTO.getQuestion());
        String trim = start.trim();
        System.out.printf(trim);
        return Result.success(trim);

    }




}