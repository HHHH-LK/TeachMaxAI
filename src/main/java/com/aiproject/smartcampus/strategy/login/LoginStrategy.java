package com.aiproject.smartcampus.strategy.login;


import com.aiproject.smartcampus.pojo.dto.UserLoginDTO;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

/**
 * 登录策略
 * */
public interface LoginStrategy {

    String login(UserLoginDTO userLoginDTO) throws Exception;

}
