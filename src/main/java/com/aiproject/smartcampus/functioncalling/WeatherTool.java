package com.aiproject.smartcampus.functioncalling;

import com.aiproject.smartcampus.exception.WeatherExpection;
import com.aiproject.smartcampus.functioncalling.toolutils.WeatherToolUtils;
import com.aiproject.smartcampus.pojo.bo.WeatherInfo;
import dev.langchain4j.agent.tool.ToolSpecification;
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
            WeatherInfo info = WeatherToolUtils.getWeatherInfo(city);
            result = info.toString();
        } catch (Exception e) {
            log.error("获取天气失败");
            throw new WeatherExpection("获取天气失败");
        }
    }



}
