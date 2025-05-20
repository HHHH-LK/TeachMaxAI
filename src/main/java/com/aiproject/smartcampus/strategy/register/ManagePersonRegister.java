package com.aiproject.smartcampus.strategy.register;

import com.aiproject.smartcampus.mapper.ManagePersonMapper;
import com.aiproject.smartcampus.pojo.dto.UserPreliminaryRegisterDTO;
import com.aiproject.smartcampus.pojo.dto.UserRegisterDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * @program: SmartCampus
 * @description: 管理人员注册
 * @author: lk
 * @create: 2025-05-20 09:33
 **/

@Component
@RequiredArgsConstructor
public class ManagePersonRegister implements RegsterStrategy {

    private final ManagePersonMapper managePersonMapper;

    @Override
    public String Preliminaryregister(UserPreliminaryRegisterDTO userPreliminaryRegisterDTO) {
        //预处理
        return null;
    }

    @Override
    public void register(String token, UserRegisterDTO userRegisterDTO) throws Exception {
        //注册
    }
}
