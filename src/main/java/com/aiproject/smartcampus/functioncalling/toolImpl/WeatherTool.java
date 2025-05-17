package com.aiproject.smartcampus.functioncalling.toolImpl;

import com.aiproject.smartcampus.exception.WeatherExpection;
import com.aiproject.smartcampus.functioncalling.Tool;
import com.aiproject.smartcampus.pojo.entity.WeatherInfo;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * @program: SmartCampus
 * @description: 工具实现
 * @author: lk
 * @create: 2025-05-17 16:33
 **/

@Data
@Slf4j
public class WeatherTool implements Tool {

    private String city;
    private String result;

    //工具执行
    @Override
    public void run() {
        try {
            WeatherInfo info = com.aiproject.smartcampus.tools.WeatherTool.getWeatherInfo(city);
            result = info.toString();
        } catch (Exception e) {
            log.error("获取天气失败");
            throw new WeatherExpection("获取天气失败");
        }
    }


}
