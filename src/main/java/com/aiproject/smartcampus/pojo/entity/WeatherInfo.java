package com.aiproject.smartcampus.pojo.entity;


import com.aiproject.smartcampus.tools.WeatherTool;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 封装完整天气信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WeatherInfo {
    /** 天气状况枚举 */
    private WeatherTool condition;
    /** 最低温度，单位°C */
    private int minTemp;
    /** 最高温度，单位°C */
    private int maxTemp;
    /** 风速及描述，例如“微风，3级” */
    private String wind;
}
