package com.aiproject.smartcampus.config;

import com.aiproject.smartcampus.pojo.bo.ToolList;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.web.search.searchapi.SearchApiWebSearchEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * @program: SmartCampus
 * @description: 工具类初始化
 * @author: lk
 * @create: 2025-05-25 14:54
 **/

@Configuration
@RequiredArgsConstructor
@Slf4j
public class ToolInit {

    private final ToolList toolList;

    //工具声明
    @Bean
    public ToolSpecification weatherTool() {
        ToolSpecification weathertool = ToolSpecification.builder()
                .name("WeatherTool")
                .description("查询天气怎么样")
                .parameters(JsonObjectSchema.builder()
                        .addStringProperty("city", "城市名字")
                        .required("city")
                        .build())
                .build();
        //将声明的tool存入list种
        List<ToolSpecification> tools = toolList.getTools();
        //判断是否已经存在
        boolean exists = tools.stream().anyMatch(tool -> "WeatherTool".equals(tool.name()));
        if (!exists) {

            log.info("天气搜索工具初始化成功");
             tools.add(weathertool);
        }

        return weathertool;
    }

    //联网能力初始化
    @Bean
    public SearchApiWebSearchEngine SearchEngine() {
        String apiKey = System.getenv("SEARCH_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("联网搜索apikey不能为空");
        }
        SearchApiWebSearchEngine searchEngine = SearchApiWebSearchEngine.builder()
                .apiKey(apiKey)
                .engine("google")
                .optionalParameters(Map.of(
                        "num", "10",           // 增加结果数量
                        "tbs", "qdr:y",        // 时间限制
                        "hl", "zh-CN",         // 语言
                        "gl", "cn",            // 地理位置
                        "safe", "active",      // 安全搜索
                        "filter", "1"          // 过滤重复结果
                ))
                .build();
        // 构造工具定义
        ToolSpecification searchToolSpec = ToolSpecification.builder()
                .name("SearchEngine")
                .description("联网搜索")
                .parameters(JsonObjectSchema.builder()
                        .addStringProperty("query", "搜索内容")
                        .required("query")
                        .build())
                .build();
        //将声明的tool存入list种
        List<ToolSpecification> tools = toolList.getTools();
        //判断是否已经存在
        boolean exists = tools.stream().anyMatch(tool -> "SearchEngine".equals(tool.name()));
        if (!exists) {
   //         log.info("联网搜索工具初始化成功");
  //          tools.add(searchToolSpec);
        }
        return searchEngine;
    }


}
