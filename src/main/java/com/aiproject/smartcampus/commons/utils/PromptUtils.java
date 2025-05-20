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

    public static String buildRagCanPrompt(String retrievedContent, String memoryContent, String userQuestion) {
        return String.format(
                "下面是检索到的相关内容（RAG）和历史对话记忆：\n\n" +
                        "---- 检索内容 START ----\n%s\n---- 检索内容 END ----\n\n" +
                        "---- 记忆内容 START ----\n%s\n---- 记忆内容 END ----\n\n" +
                        "用户问题：%s\n\n" +
                        "请判断你是否可以仅基于自身知识和上述信息是否足够完整地回答用户问题：\n" +
                        "- 如果可以，请仅返回 “CAN”。\n" +
                        "- 如果不行，请仅返回 “CANNOT”。",
                retrievedContent, memoryContent, userQuestion
        );
    }

    public static String buildRagAnswerPrompt(String retrievedContent, String memoryContent, String userQuestion) {
        return String.format(
                "你是一位专业的 RAG 助手。\n\n" +
                        "以下内容由检索模块返回，请作为主要依据：\n" +
                        "---- 检索到的相关内容 START ----\n%s\n---- 检索到的相关内容 END ----\n\n" +
                        "以下是历史对话记忆：\n" +
                        "---- 记忆内容 START ----\n%s\n---- 记忆内容 END ----\n\n" +
                        "用户提问：%s\n\n" +
                        "请先基于检索内容给出“核心观点”（一句话）；\n" +
                        "然后给出“扩展说明”，必要时引用检索片段；\n" +
                        "如果检索内容和记忆都不足以完整回答，可补充模型自身知识\n" +
                        "如果完全无法回答，则只返回：“无法回答”。\n\n" +
                        "注意：输出不要包含多余解释，直接给出上述两部分内容。",
                retrievedContent, memoryContent, userQuestion
        );
    }



    /**
         * 判断型 Prompt：仅返回 CAN 或 CANNOT
         */
    public static String buildCanPrompt(String memoryContent, String userQuestion) {
        return String.format(
                "以下是历史对话记忆内容：\n%s\n\n" +
                        "用户问题：%s\n\n" +
                        "请判断你是否可以仅基于自身知识和以下记忆内容，完整且准确地回答用户问题：\n" +
                        "- 如果可以，请仅返回“CAN”。\n" +
                        "- 如果不行，请仅返回“CANNOT”。",
                memoryContent,
                userQuestion
        );
    }



    /**
             * 回答型 Prompt：真正生成答案
             */
            public static String buildAnswerPrompt (String memoryContent, String userQuestion){
                return String.format(
                        "以下是对话记忆内容：\n%s\n\n" +
                                "请基于以上记忆和用户问题，直接给出最佳答案。不要输出任何其他内容或解释。\n\n" +
                                "用户问题：%s",
                        memoryContent,
                        userQuestion
                );
            }

    public static String buildFuncCanPrompt(String toolResult, String memoryContent, String retrievedContent, String userQuestion) {
        return String.format(
                "以下是工具调用结果、历史对话记忆和检索到的相关内容：\n\n" +
                        "---- 工具调用结果 START ----\n%s\n---- 工具调用结果 END ----\n\n" +
                        "---- 历史对话记忆 START ----\n%s\n---- 历史对话记忆 END ----\n\n" +
                        "---- 检索到的相关内容 START ----\n%s\n---- 检索到的相关内容 END ----\n\n" +
                        "用户问题：%s\n\n" +
                        "请判断你是否可以基于上述信息，完整且准确地回答用户问题：\n" +
                        "- 如果可以，请仅返回“CAN”。\n" +
                        "- 如果不行，请仅返回“CANNOT”。",
                toolResult,
                memoryContent,
                retrievedContent,
                userQuestion
        );
    }


    /**
     * 工具调用回答型 Prompt：真正生成答案
     */
    public static String buildFuncAnswerPrompt(String toolResult, String memoryContent, String retrievedContent, String userQuestion) {
        return String.format(
                "你是一位专业的 AI 助手。\n\n" +
                        "请根据以下信息回答用户的问题：\n\n" +
                        "---- 工具调用结果 START ----\n%s\n---- 工具调用结果 END ----\n\n" +
                        "---- 历史对话记忆 START ----\n%s\n---- 历史对话记忆 END ----\n\n" +
                        "---- 检索到的相关内容 START ----\n%s\n---- 检索到的相关内容 END ----\n\n" +
                        "用户问题：%s\n\n" +
                        "请基于以上信息结合模型自身能力进行回答"+
                        "在回答中，直接给出最佳答案。不要输出任何其他内容或解释\n" +
                        "如果无法回答，请仅返回：“很抱歉，我无法根据现有的信息回答您的问题。您可以尝试重新表述问题或提供更多上下文。”。",
                toolResult,
                memoryContent,
                retrievedContent,
                userQuestion
        );
    }


    public static String buildisOKPrompt(String content, String userMessage) {
        String answerprompt = String.format(
                "请根据以下内容以及你自身的能力：\n" +
                        "%s\n" +
                        "回答问题：%s\n"+
                "如果能够回答用户的问题则返回答案，否则返则无法回答",
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
