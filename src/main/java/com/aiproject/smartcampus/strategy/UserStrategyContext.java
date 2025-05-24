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

    //预注册策略
    public String Preliminaryregisterregister(UserPreliminaryRegisterDTO userPreliminaryRegisterDTO) throws Exception {
        String preliminaryregistertoken = regsterStrategy.Preliminaryregister(userPreliminaryRegisterDTO);
        return preliminaryregistertoken;
    }

    //注册策略
    public void register(String token, UserRegisterDTO userRegisterDTO) throws Exception {
        regsterStrategy.register(token,userRegisterDTO);

    }
    //登录策略
    public String login(UserLoginDTO userLoginDTO) throws Exception {
        String token = loginStrategy.login(userLoginDTO);
        return token;
    }


}
