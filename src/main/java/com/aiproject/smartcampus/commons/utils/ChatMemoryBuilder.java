package com.aiproject.smartcampus.commons.utils;

import com.aiproject.smartcampus.store.LocalStore;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @program: SmartCampus
 * @description: 构建memory
 * @author: lk
 * @create: 2025-05-17 17:15
 **/

@RequiredArgsConstructor
public class ChatMemoryBuilder {

    public static ChatMemory buildChatMemory(List<ChatMessage> messages, int maxMessages) {
        ChatMemory chatMemory = MessageWindowChatMemory.withMaxMessages(maxMessages);
        for (ChatMessage message : messages) {
            chatMemory.add(message);
        }
        return chatMemory;
    }

    public static ChatMemory buildChatMemory( String userId, ChatMemoryStore store) {
        ChatMemory chatMemory = MessageWindowChatMemory.builder()
                .chatMemoryStore(store)
                .id(userId)
                .maxMessages(10)
                .build();

        return chatMemory;
    }
}
