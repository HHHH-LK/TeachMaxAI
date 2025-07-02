package com.aiproject.smartcampus.commons.utils;


import com.aiproject.smartcampus.pojo.dto.UserLoginDTO;
import lombok.Data;
import org.springframework.stereotype.Component;

/**
 * @program: SmartCampus
 * @description: 线程集合工具类·1
 * @author: lk
 * @create: 2025-05-17 17:33
 **/

@Data
public class  UserLocalThreadUtils {

    private static ThreadLocal<UserLoginDTO> threadLocal = new ThreadLocal<>();

    public static UserLoginDTO getUserInfo(){
        return threadLocal.get();
    }
    public static void setUserInfo(UserLoginDTO user){threadLocal.set(user);}
    public static void removeUserInfo(){threadLocal.remove();}

}
