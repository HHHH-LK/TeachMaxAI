package com.aiproject.smartcampus.model.summer;

import com.aiproject.smartcampus.commons.client.ResultCilent;
import com.aiproject.smartcampus.commons.utils.UserLocalThreadUtils;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static com.aiproject.smartcampus.model.prompts.SystemPrompts.INITENT_SUMMER_PROMPT;

/**
 * @program: SmartCampus
 * @description: 模型对结果进行处理
 * @author: lk
 * @create: 2025-05-28 00:33
 **/

@Service
@Slf4j
@RequiredArgsConstructor
public class ModelSummer {

    private final ResultCilent resultCilent;

    private final ChatLanguageModel chatLanguageModel;

    public String summer(List<String> intents) {
        List<CompletableFuture<String>> result = new ArrayList<>();
        for (String intent : intents) {
            List<CompletableFuture<String>> intentResults = resultCilent.getResult(intent);
            if (intentResults != null && !intentResults.isEmpty()) {
                result.addAll(intentResults);
            } else {
                log.warn("任务 {} 的结果为空，跳过添加", intent);
            }
        }
        if (result.isEmpty()) {
            log.warn("所有任务结果都为空，返回默认响应");
            return "系统正在处理您的请求，相关信息暂时无法获取，请稍后重试";
        }

        //等待所有任务执行完
        CompletableFuture.allOf(result.toArray(new CompletableFuture[0])).join();
        try {
            // 收集任务的结果
            List<String> completedResults = result.stream()
                    .map(CompletableFuture::join)
                    .toList();
            String summaryInput = String.join("\n", completedResults);
            String userMemory = UserLocalThreadUtils.getUserMemory();
            String QUEST_PROMPT="请严格基于用户的需求进行生成\""+"用户的需求为"+userMemory+"输出要求为只输出纯文本，不需要其他任何的特殊符号，请返回符合流式输出的结果进行格式化处理。";
            ChatResponse chatResponse = chatLanguageModel.chat(
                    SystemMessage.from(INITENT_SUMMER_PROMPT+QUEST_PROMPT),
                    UserMessage.from(summaryInput)
            );
            return chatResponse.aiMessage().text();

        } catch (Exception e) {
            log.error("模型处理异常", e);
            return "模型处理失败：" + e.getMessage();
        }
    }

}
