package com.aiproject.smartcampus.commons.utils;

import dev.langchain4j.data.message.ChatMessage;
import org.springframework.data.redis.connection.ReactiveSubscription;

import java.util.List;

/**
 * @program: SmartCampus
 * @description: 构建提示词
 * @author: lk
 * @create: 2025-05-17 18:35
 **/

public class PromptUtils {
    public static String buildCheckPrompt(String content, String userMessage) {
        String prompt = String.format(
                "请根据以下内容：\n" +
                        "%s\n" +
                        "能否回答问题：%s\n" +
                        "如果能，请只返回“能”；如果不能，请只返回“不能”。",
                content,
                userMessage
        );
        return prompt;
    }
    public static String buildAnswerPrompt(String content, String userMessage) {
        String answerprompt = String.format(
                "请根据以下内容：\n" +
                        "%s\n" +
                        "回答问题：%s\n",
                content,
                userMessage
        );
        return answerprompt;
    }

    public static String buildAnswerPrompt(String result,String content, String userMessage) {
        //构建系统提示词
        String promptTem = "请根据以下结果{result}\n{content}\n 回复该问题{userMessage}\n";
        //替换提示词
        String prompt = promptTem.replace("{result}", result)
                .replace("{content}",content)
                .replace("{userMessage}", userMessage);
        return prompt;
    }

    public static String buildSummaryPrompt(List<ChatMessage> messge){

        StringBuffer stringBuffer=new StringBuffer();
        for (ChatMessage s : messge) {
            stringBuffer.append(s.toString());
            stringBuffer.append("\n");
        }

        String prompt="请总结 "+stringBuffer.toString()+"\n 简要概述用户的需要意图,只返回用户的意图";
        return prompt;
    }

}
