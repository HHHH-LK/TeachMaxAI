package com.aiproject.smartcampus.model.intent.summer;

import com.aiproject.smartcampus.commons.ResultCilent;
import com.aiproject.smartcampus.model.prompts.SystemPrompts;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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
            result.addAll(resultCilent.getResult(intent));
        }
        //等待所有任务执行完
        CompletableFuture.allOf(result.toArray(new CompletableFuture[0])).join();
        try {
            // 收集任务的结果
            List<String> completedResults = result.stream()
                    .map(CompletableFuture::join)
                    .toList();
            String summaryInput = String.join("\n", completedResults);
            ChatResponse chatResponse = chatLanguageModel.chat(
                    SystemMessage.from(INITENT_SUMMER_PROMPT),
                    UserMessage.from(summaryInput)
            );
            return chatResponse.aiMessage().text();

        } catch (Exception e) {
            log.error("模型处理异常", e);
            return "模型处理失败：" + e.getMessage();
        }


    }

}
