package com.aiproject.smartcampus.strategy;

import com.aiproject.smartcampus.pojo.dto.UserLoginDTO;
import com.aiproject.smartcampus.pojo.dto.UserPreliminaryRegisterDTO;
import com.aiproject.smartcampus.pojo.dto.UserRegisterDTO;
import com.aiproject.smartcampus.strategy.login.AccountLogin;
import com.aiproject.smartcampus.strategy.login.LoginStrategy;
import com.aiproject.smartcampus.strategy.login.PhoneLogin;
import com.aiproject.smartcampus.strategy.register.RegsterStrategy;
import lombok.Data;

/**
 * @program: SmartCampus
 * @description: 选择策略
 * @author: lk
 * @create: 2025-05-20 09:34
 **/

@Data
public class UserStrategyContext {

    private RegsterStrategy regsterStrategy;
    private LoginStrategy loginStrategy;


    public String Preliminaryregisterregister(UserPreliminaryRegisterDTO userPreliminaryRegisterDTO) throws Exception {
        String preliminaryregistertoken = regsterStrategy.Preliminaryregister(userPreliminaryRegisterDTO);
        return preliminaryregistertoken;
    }

    public void register(String token, UserRegisterDTO userRegisterDTO) throws Exception {
        regsterStrategy.register(token,userRegisterDTO);

    }

    public String AccountLogin(UserLoginDTO userLoginDTO) throws Exception {
        String token = loginStrategy.login(userLoginDTO);
        return token;
    }

    public String PhoneLogin(UserLoginDTO userLoginDTO) throws Exception {
        String token = loginStrategy.login(userLoginDTO);
        return token;

    }


}
