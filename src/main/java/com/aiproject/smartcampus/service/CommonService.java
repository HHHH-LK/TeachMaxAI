package com.aiproject.smartcampus.service;

import com.aiproject.smartcampus.commons.client.Result;
import com.aiproject.smartcampus.pojo.dto.UserLoginDTO;
import com.aiproject.smartcampus.pojo.dto.UserPreliminaryRegisterDTO;
import com.aiproject.smartcampus.pojo.dto.UserRegisterDTO;
import org.springframework.web.multipart.MultipartFile;

public interface CommonService {
/**
 * 用户预注册，填写完用户信息后方可真正注册成功
 * */
    Result userPreliminaryregister(UserPreliminaryRegisterDTO userPreliminaryRegisterDTO) throws Exception;
/**
 * 用户注册后用户信息的编写
 * */
    Result userRegister(String registrationToken, UserRegisterDTO userRegisterDTO) throws Exception;
/**
 * 用户登陆
 * */
    Result userLogin(UserLoginDTO userLoginDTO) throws Exception;
/**
 * 用户退出登陆
 * */
    Result userLogout(String token);

    /**
     *文件上传
     */
    Result upload(MultipartFile file);

    /**
     * 定时消息发送
     */
    Result notificateService(Integer userId);

    /**
     * 广播发送消息
     */
    Result broadcastNotification(String content);

    /**
     * 消息确认
     */
    Result notificationConfirm(String charater,String userId, String notificatioonId,String content);


}
