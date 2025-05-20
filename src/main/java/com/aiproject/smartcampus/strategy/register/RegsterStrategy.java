package com.aiproject.smartcampus.strategy.register;


import com.aiproject.smartcampus.pojo.dto.UserPreliminaryRegisterDTO;
import com.aiproject.smartcampus.pojo.dto.UserRegisterDTO;

public interface RegsterStrategy {

//This method is used to register a user with preliminary information
    public String Preliminaryregister(UserPreliminaryRegisterDTO userPreliminaryRegisterDTO) throws Exception;
    public void register(String token, UserRegisterDTO userRegisterDTO) throws Exception;

}
