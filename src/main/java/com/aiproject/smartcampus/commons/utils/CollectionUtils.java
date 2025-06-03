package com.aiproject.smartcampus.commons.utils;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.rag.content.Content;

import java.util.List;

/**
 * @program: lecture-langchain-20250525
 * @description: 集合工具类
 * @author: lk
 * @create: 2025-05-11 11:03
 **/

public class CollectionUtils {
    public static Boolean isEmpty(List collection) {
        if (collection == null || collection.isEmpty()) {
            return true;
        }
        return false;
    }

    public static String MessageSplicing(List<ChatMessage> collection) {
        if (isEmpty(collection)) {
            return "";
        }
        StringBuilder stringBuilder=new StringBuilder();
        for (ChatMessage message : collection) {
            stringBuilder.append(message.text()).append("\n");
        }
        String content = stringBuilder.toString();
        return content;
    }

    public static String ContentSplicing(List<Content> collection) {
        if (isEmpty(collection)) {
            return "";
        }
        StringBuilder stringBuilder = new StringBuilder();
        for (Content message : collection) {
            stringBuilder.append(message.textSegment().text()).append("\n");
        }
        String content = stringBuilder.toString();
        return content;
    }


    }
