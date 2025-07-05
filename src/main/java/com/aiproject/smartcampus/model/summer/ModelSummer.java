package com.aiproject.smartcampus.model.summer;

import com.aiproject.smartcampus.commons.client.ResultCilent;
import com.aiproject.smartcampus.commons.utils.UserLocalThreadUtils;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
@RequiredArgsConstructor
public class ModelSummer {

    private final ResultCilent resultCilent;
    private final ChatLanguageModel chatLanguageModel;
    private final ChatMemoryStore chatMemoryStore;       // 注入你的 LocalStore

    /**
     * 记忆存储的统一 ID，这里你也可以根据场景生成动态 ID
     */
    private static String MEMORY_ID = "smart_campus_summary_memory:";
    private static final int MAX_MEMORY_MESSAGES = 10;

    private static final String SUMMARY_SYSTEM_PROMPT =
            "你是一个智能助手，能够基于历史对话记忆，对以下多步任务结果进行总结。"
                    + "请结合上下文和历史记录，给出精准、有条理的汇总。";

    public String summer(List<String> intents) {
        // 1. 收集所有子任务结果
        List<CompletableFuture<String>> futures = new ArrayList<>();
        for (String intent : intents) {
            List<CompletableFuture<String>> intentResults = resultCilent.getResult(intent);
            if (intentResults != null && !intentResults.isEmpty()) {
                futures.addAll(intentResults);
            } else {
                log.warn("任务 {} 的结果为空，跳过添加", intent);
            }
        }
        if (futures.isEmpty()) {
            log.warn("所有任务结果都为空，返回默认响应");
            return "系统正在处理您的请求，相关信息暂时无法获取，请稍后重试";
        }
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        MEMORY_ID = MEMORY_ID + UserLocalThreadUtils.test();

        // 2. 从内存里拉取最近的几条对话记录
        List<ChatMessage> history = chatMemoryStore.getMessages(MEMORY_ID);
        if (history == null) {
            history = new ArrayList<>();
        }
        // 只保留最后几条，避免上下文过长
        int start = Math.max(history.size() - MAX_MEMORY_MESSAGES, 0);
        List<ChatMessage> recentHistory = history.subList(start, history.size());

        // 3. 拼接新的上下文
        List<String> completedResults = futures.stream()
                .map(CompletableFuture::join)
                .toList();
        String newContext = String.join("\n\n", completedResults);

        List<ChatMessage> messages = new ArrayList<>();
        messages.add(SystemMessage.from(SUMMARY_SYSTEM_PROMPT));
        messages.addAll(recentHistory);
        messages.add(UserMessage.from(newContext));

        // 4. 调用 LLM 生成汇总
        String summary;
        try {
            ChatResponse response = chatLanguageModel.chat(messages);
            summary = response.aiMessage().text();
        } catch (Exception e) {
            log.error("模型处理异常", e);
            return "模型处理失败：" + e.getMessage();
        }

        try {
            chatMemoryStore.updateMessages(MEMORY_ID, List.of(
                    UserMessage.from(newContext),
                    AiMessage.from(summary)
            ));
        } catch (Exception e) {
            log.warn("更新汇总记忆失败，但不影响主流程", e);
        }

        return summary;
    }
}
