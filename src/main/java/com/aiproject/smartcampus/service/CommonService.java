package com.aiproject.smartcampus.service;

import com.aiproject.smartcampus.commons.client.Result;
import com.aiproject.smartcampus.pojo.dto.*;
import com.aiproject.smartcampus.pojo.po.Student;
import com.aiproject.smartcampus.pojo.po.Teacher;
import com.aiproject.smartcampus.pojo.po.User;
import org.springframework.web.multipart.MultipartFile;

public interface CommonService {

    //登录
    Result login(LoginDTO loginDTO);

    //注册
    Result register(RegisterDTO registerDTO);

//    Result sendPasswordResetCode(PasswordResetVerificationDTO dto);
    //重置密码
    Result resetPassword(PasswordResetDTO dto);

    //完善用户信息
    Result completeStudentInfo(Integer userId, CompleteStudentDTO studentInfo);

    Result completeTeacherInfo(Integer userId, CompleteTeacherDTO teacherInfo);
}
