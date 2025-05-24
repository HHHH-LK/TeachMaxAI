package com.aiproject.smartcampus.service;

import com.aiproject.smartcampus.commons.Result;
import com.aiproject.smartcampus.pojo.dto.UserLoginDTO;
import com.aiproject.smartcampus.pojo.dto.UserPreliminaryRegisterDTO;
import com.aiproject.smartcampus.pojo.dto.UserRegisterDTO;
import org.springframework.web.multipart.MultipartFile;

public interface CommonService {

    Result userPreliminaryregister(UserPreliminaryRegisterDTO userPreliminaryRegisterDTO) throws Exception;
    Result userRegister(String registrationToken,UserRegisterDTO userRegisterDTO) throws Exception;

    Result userLogin(UserLoginDTO userLoginDTO) throws Exception;

    Result userLogout(String token);

    Result upload(MultipartFile file);
}
