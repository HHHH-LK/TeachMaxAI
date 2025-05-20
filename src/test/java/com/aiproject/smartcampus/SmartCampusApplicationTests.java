package com.aiproject.smartcampus;

import com.aiproject.smartcampus.commons.utils.PromptUtils;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class SmartCampusApplicationTests {

    @Autowired
    private ChatLanguageModel chatLanguageModel;

    @Test
    void contextLoads() {


        String prompt = PromptUtils.buildAnswerPrompt("李明是猪", "李明是谁");
        String answer = chatLanguageModel.chat(prompt);
        System.out.println(answer);

    }

}
