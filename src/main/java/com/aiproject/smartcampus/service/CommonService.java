package com.aiproject.smartcampus.service;

import com.aiproject.smartcampus.commons.Result;
import com.aiproject.smartcampus.pojo.dto.UserPreliminaryRegisterDTO;
import com.aiproject.smartcampus.pojo.dto.UserRegisterDTO;

public interface CommonService {

    Result userPreliminaryregister(UserPreliminaryRegisterDTO userPreliminaryRegisterDTO) throws Exception;
    Result userRegister(String registrationToken,UserRegisterDTO userRegisterDTO) throws Exception;

}
