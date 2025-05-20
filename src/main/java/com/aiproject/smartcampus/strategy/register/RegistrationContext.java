package com.aiproject.smartcampus.strategy.register;

import com.aiproject.smartcampus.pojo.dto.UserPreliminaryRegisterDTO;
import com.aiproject.smartcampus.pojo.dto.UserRegisterDTO;
import lombok.Data;

/**
 * @program: SmartCampus
 * @description: 选择策略
 * @author: lk
 * @create: 2025-05-20 09:34
 **/

@Data
public class RegistrationContext {

    private RegsterStrategy regsterStrategy;


    public String Preliminaryregisterregister(UserPreliminaryRegisterDTO userPreliminaryRegisterDTO) throws Exception {
        String preliminaryregistertoken = regsterStrategy.Preliminaryregister(userPreliminaryRegisterDTO);
        return preliminaryregistertoken;
    }

    public void register(String token, UserRegisterDTO userRegisterDTO) throws Exception {
        regsterStrategy.register(token,userRegisterDTO);

    }



}
