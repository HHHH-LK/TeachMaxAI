package com.aiproject.smartcampus.exception;

/**
 * @program: SmartCampus
 * @description: 天气工具获取异常
 * @author: lk
 * @create: 2025-05-17 16:44
 **/

public class WeatherExpection extends RuntimeException{

    public WeatherExpection(String message) {
        super(message);

    }
    public WeatherExpection() {

    }

}
