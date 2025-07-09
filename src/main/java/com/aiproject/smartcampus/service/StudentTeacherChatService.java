package com.aiproject.smartcampus.service;


import com.aiproject.smartcampus.commons.client.Result;
import com.aiproject.smartcampus.pojo.dto.SendMessageRequestDTO;

import java.util.List;

public interface StudentTeacherChatService {

    Result getChatByTeacherId(String teacherId);

    Result getAllStudentNotReadNum();

    Result getAllTeacherNotReadNum();

    Result getAllChatNotReadInfoBytudent();

    Result getAllChatNotReadInfoByteacher();

    Result getAllTeacherInfo(List<String> courseIds);

    Result sendMessage(SendMessageRequestDTO sendMessageRequestDTO);
}
