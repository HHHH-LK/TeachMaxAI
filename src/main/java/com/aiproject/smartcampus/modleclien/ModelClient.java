package com.aiproject.smartcampus.modleclien;

import dev.langchain4j.community.model.dashscope.QwenChatModel;
import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;

/**
 * @program: SmartCampus
 * @description: 模型链接转换
 * @author: lk
 * @create: 2025-05-25 14:34
 **/

@RequiredArgsConstructor
public class ModelClient {

    private static String api_key ;//=System.getenv("DASHSCOPE_API_KEY");
    private static String modle_name;


    public static ModelClient builder(String api_key,String modle_name){

        ModelClient modelClient = new ModelClient();
        modelClient.api_key = api_key;
        modelClient.modle_name = modle_name;
        return modelClient;
    }

    public ChatLanguageModel build(){
        //基于modelName和apikey进行处理模型的转换
        return  QwenChatModel.builder()
                .apiKey(api_key)
                .modelName(modle_name)
                .build();

    }


}
