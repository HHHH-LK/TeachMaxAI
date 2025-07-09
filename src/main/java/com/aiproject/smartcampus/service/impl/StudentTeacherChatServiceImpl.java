package com.aiproject.smartcampus.service.impl;

import com.aiproject.smartcampus.commons.client.Result;
import com.aiproject.smartcampus.commons.utils.UserToTypeUtils;
import com.aiproject.smartcampus.controller.ChatSSEController;
import com.aiproject.smartcampus.mapper.StudentTeacherChatMapper;
import com.aiproject.smartcampus.pojo.dto.ChatMessagePushDto;
import com.aiproject.smartcampus.pojo.dto.SendMessageRequestDTO;
import com.aiproject.smartcampus.pojo.po.TeacherStudentMessage;
import com.aiproject.smartcampus.pojo.vo.ChatQueryVO;
import com.aiproject.smartcampus.pojo.vo.ConversationUnreadCountVO;
import com.aiproject.smartcampus.pojo.vo.TeacherInfoVO;
import com.aiproject.smartcampus.service.StudentTeacherChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;


import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @program: ss
 * @description:
 * @author: lk_hhh
 * @create: 2025-07-09 21:21
 **/

@Service
@Slf4j
@RequiredArgsConstructor
public class StudentTeacherChatServiceImpl implements StudentTeacherChatService {

    private final StudentTeacherChatMapper studentTeacherChatMapper;
    private final UserToTypeUtils userToTypeUtils;
    private final ChatSSEController chatSSEController;

    @Override
    public Result getChatByTeacherId(String teacherId) {

        //获取学生id
        String studentId = userToTypeUtils.change();
        ChatQueryVO chatByTeacherId = studentTeacherChatMapper.getChatByTeacherId(studentId, teacherId);

        //如果会话为null，则新建一个会话
        if (chatByTeacherId == null) {
            int i = studentTeacherChatMapper.insertConversation(studentId, teacherId);
            if (i > 0) {
                return Result.success(new ArrayList<>());
            }
        }

        //设置所有信息为已读状态
        Integer i = studentTeacherChatMapper.markReceivedMessagesAsRead(teacherId, studentId);
        if (i < 0) {
            log.warn("用户{}没有接收到任何信息", studentId);
        }

        //会话不为空则进行查询会话历史进行返回
        List<TeacherStudentMessage> allMessagesByStudentAndTeacher = studentTeacherChatMapper
                .getAllMessagesByStudentAndTeacher(studentId, teacherId, chatByTeacherId.getConversationId());

        return Result.success(allMessagesByStudentAndTeacher);
    }

    @Override
    public Result getAllTeacherNotReadNum() {

        String teacherId = userToTypeUtils.change();

        Integer allUnreadMessageCountForTeacher = studentTeacherChatMapper.getAllUnreadMessageCountForTeacher(teacherId);

        return Result.success(allUnreadMessageCountForTeacher);
    }

    @Override
    public Result getAllChatNotReadInfoBytudent() {

        String studentId = userToTypeUtils.change();

        List<ConversationUnreadCountVO> unreadCountByConversationForStudent = studentTeacherChatMapper.getUnreadCountByConversationForStudent(studentId);

        return Result.success(unreadCountByConversationForStudent);

    }

    @Override
    public Result getAllChatNotReadInfoByteacher() {

        String teacherId = userToTypeUtils.change();

        List<ConversationUnreadCountVO> unreadCountByConversationForTeacher = studentTeacherChatMapper.getUnreadCountByConversationForTeacher(teacherId);

        return Result.success(unreadCountByConversationForTeacher);

    }

    @Override
    public Result getAllTeacherInfo(List<String> courseIds) {

        String studentId = userToTypeUtils.change();

        List<TeacherInfoVO> list = studentTeacherChatMapper.getTeachersWithCourseByStudentIdAndCourseIds(studentId, courseIds);

        List<TeacherInfoVO> teacherInfoVOList = list.stream().peek(a ->
                {
                    //对教师信息添加标签字段
                    a.setTags(a.getCourseTags(a.getCourseName()));
                    //设置排序权重
                    a.setT(a.setTBystatus());
                }
        ).sorted((a, b) -> a.getT() - b.getT()).toList();

        return Result.success(teacherInfoVOList);
    }

    @Override
    public Result sendMessage(SendMessageRequestDTO sendMessageRequestDTO) {
        //判断是否有权限进行会话
        String sendUserId = userToTypeUtils.change();
        Integer reciveUserId = sendMessageRequestDTO.getUseId();
        if (reciveUserId == null) {
            return Result.error("你无权进行信息的发送");
        }
        if (sendUserId.equals(reciveUserId.toString())) {
            return Result.error("你无权进行信息的发送");
        }

        //进行发送信息
        Long conversationId = sendMessageRequestDTO.getConversationId();
        String messageType = sendMessageRequestDTO.getMessageType();
        String fileUrl = sendMessageRequestDTO.getFileUrl();
        String content = sendMessageRequestDTO.getContent();

        int i = studentTeacherChatMapper.sendMessage(conversationId, Integer.valueOf(sendUserId), content, messageType, fileUrl);

        if (i <= 0) {
            log.error("发送信息失败");
            return Result.error("发送信息失败");
        }

        // 新增：发送成功后，通过SSE推送给接收者
        try {
            if (chatSSEController != null) {
                // 异步推送，不影响主业务流程
                pushMessageAsync(sendMessageRequestDTO, sendUserId);
            }
        } catch (Exception e) {
            // 推送失败不影响主业务，只记录日志
            log.warn("SSE推送失败，但消息发送成功 - 接收者: {}, 错误: {}", reciveUserId, e.getMessage());
        }

        return Result.success("发送信息成功");
    }

    /**
     * 异步推送消息 - 修复异步注解
     */
    @Async("sseThreadPool") // 指定线程池
    public void pushMessageAsync(SendMessageRequestDTO request, String senderUserId) {
        try {
            // 构建推送消息
            ChatMessagePushDto pushMessage = ChatMessagePushDto.builder()
                    .conversationId(request.getConversationId())
                    .senderUserId(Integer.valueOf(senderUserId))
                    .content(request.getContent())
                    .messageType(request.getMessageType())
                    .fileUrl(request.getFileUrl())
                    .timestamp(System.currentTimeMillis())
                    .build();

            // 推送给接收者
            chatSSEController.pushMessageToUser(request.getUseId().toString(), pushMessage);

            log.info("SSE推送成功 - 发送者: {}, 接收者: {}", senderUserId, request.getUseId());

        } catch (Exception e) {
            log.error("异步推送失败", e);
        }
    }


    @Override
    public Result getAllStudentNotReadNum() {

        String studentId = userToTypeUtils.change();

        Integer allUnreadMessageCountForStudent = studentTeacherChatMapper.getAllUnreadMessageCountForStudent(Integer.valueOf(studentId));

        return Result.success(allUnreadMessageCountForStudent);

    }


}

