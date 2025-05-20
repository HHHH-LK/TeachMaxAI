package com.aiproject.smartcampus.exception;

/**
 * @program: SmartCampus
 * @description: 用户异常处理
 * @author: lk
 * @create: 2025-05-20 12:35
 **/

public class UserExpection extends RuntimeException{

    public UserExpection(){
        super();
    }

    public UserExpection(String message){
        super(message);
    }

}
