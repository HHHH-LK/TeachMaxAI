package com.aiproject.smartcampus.model.rag;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.Tokenizer;
import org.springframework.stereotype.Component;

/**
 * @program: lecture-langchain-20250525
 * @description: token  分词器
 * @author: lk
 * @create: 2025-05-14 17:35
 **/

@Component
public class LocalTokenizerFuncation implements Tokenizer {

    @Override
    public int estimateTokenCountInText(String s) {
        return s.length();
    }

    @Override
    public int estimateTokenCountInMessage(ChatMessage chatMessage) {
        return estimateTokenCountInText(chatMessage.toString());
    }

    @Override
    public int estimateTokenCountInMessages(Iterable<ChatMessage> iterable) {
        int total = 0;
        for (ChatMessage message : iterable) {
            total += estimateTokenCountInMessage(message);
        }
        return total;
    }

    @Override
    public int estimateTokenCountInToolSpecifications(Iterable<ToolSpecification> iterable) {
        int total = 0;
        for (ToolSpecification spec : iterable) {
            total += estimateTokenCountInText(spec.name());
            total += estimateTokenCountInText(spec.description());
        }
        return total;
    }

    @Override
    public int estimateTokenCountInToolExecutionRequests(Iterable<ToolExecutionRequest> iterable) {
        int total = 0;
        for (ToolExecutionRequest request : iterable) {
            total += estimateTokenCountInText(request.name());
            total += estimateTokenCountInText(request.arguments().toString());
        }
        return total;
    }


}
