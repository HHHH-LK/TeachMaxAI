package com.aiproject.smartcampus.controller;

import com.aiproject.smartcampus.commons.client.Result;
import com.aiproject.smartcampus.pojo.dto.SendMessageRequestDTO;
import com.aiproject.smartcampus.service.StudentTeacherChatService;
import dev.langchain4j.agent.tool.P;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @program: ss
 * @description:师生交流
 * @author: lk_hhh
 * @create: 2025-07-09 21:18
 **/

@RestController
@RequestMapping("/studentteacher/chat")
@RequiredArgsConstructor
public class StudentTeacherChatController {

    private final StudentTeacherChatService studentTeacherChatService;


    /**
     * 查询对应的会话信息
     */
    @GetMapping("/getChatByteacherId")
    public Result getChatByteacherId(@RequestParam("teacherId") String teacherId) {

        return studentTeacherChatService.getChatByTeacherId(teacherId);

    }

    /**
     * 查询学生总未读信息数量
     */
    @GetMapping("/getAllStudentNOtReadNum")
    public Result getAllStudentNotReadNum() {

        return studentTeacherChatService.getAllStudentNotReadNum();
    }

    /**
     * 查询老师总未读信息数量
     */
    @GetMapping("/getALlTeacherNOtReadNum")
    public Result getALlTeacherNotReadNum() {

        return studentTeacherChatService.getAllTeacherNotReadNum();

    }

    /**
     * 查询学生会话为读总信息
     */
    @GetMapping("/getAllChatNotReadInfoBytudent")
    public Result getAllChatNotReadInfoBytudent() {

        return studentTeacherChatService.getAllChatNotReadInfoBytudent();

    }

    /**
     * 查询老师会话为读信息
     * */
    @GetMapping("/getAllChatNotReadInfoByteacher")
    public Result getAllChatNotReadInfoByteacher() {

        return studentTeacherChatService.getAllChatNotReadInfoByteacher();
    }

    /**
     * 获取老师老师信息
     * */
    @PostMapping("getAllTeacherInfo")
    public Result getAllTeacherInfo(@RequestBody List<String> courseIds) {

        return studentTeacherChatService.getAllTeacherInfo(courseIds);

    }

    /**
     * 发送对话
     * */
    @PostMapping("sentChatMemory")
    public Result sentChatMemory(@RequestBody SendMessageRequestDTO sendMessageRequestDTO) {

        return studentTeacherChatService.sendMessage(sendMessageRequestDTO);

    }



}