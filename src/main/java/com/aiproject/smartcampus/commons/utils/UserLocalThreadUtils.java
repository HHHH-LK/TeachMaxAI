package com.aiproject.smartcampus.commons.utils;


import com.aiproject.smartcampus.pojo.dto.UserRegisterDTO;
import lombok.Data;

/**
 * @program: SmartCampus
 * @description: 线程集合工具类·1
 * @author: lk
 * @create: 2025-05-17 17:33
 **/

@Data
public class   UserLocalThreadUtils {

    private static ThreadLocal<UserRegisterDTO> threadLocal = new ThreadLocal<>();

    public static UserRegisterDTO getUserInfo(){
        return threadLocal.get();
    }
    public static void setUserInfo(UserRegisterDTO user){threadLocal.set(user);}
    public static void removeUserInfo(){threadLocal.remove();}

}
