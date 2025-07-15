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
 * @description: 模型对结果进行处理 - 针对流式输出优化
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
            return "系统正在处理您的请求，相关信息暂时无法获取，请稍后重试。";
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

            // 修改后的提示词 - 针对流式输出优化
            String QUEST_PROMPT = buildStreamingOptimizedPrompt(userMemory);

            ChatResponse chatResponse = chatLanguageModel.chat(
                    SystemMessage.from(INITENT_SUMMER_PROMPT + QUEST_PROMPT),
                    UserMessage.from(summaryInput)
            );

            // 后处理：确保输出符合流式格式
            String rawResponse = chatResponse.aiMessage().text();
            return postProcessForStreaming(rawResponse);

        } catch (Exception e) {
            log.error("模型处理异常", e);
            return "模型处理失败，请稍后重试。";
        }
    }

    /**
     * 构建针对流式输出优化的提示词
     */
    private String buildStreamingOptimizedPrompt(String userMemory) {
        return """
                
                ## 特别要求（针对流式输出优化）：
                
                ### 📝 输出格式要求：
                1. **纯文本输出**：不使用任何Markdown格式符号，包括：
                   - 不使用 # ## ### 等标题符号
                   - 不使用 ** 粗体符号
                   - 不使用 * - 列表符号  
                   - 不使用 ``` 代码块符号
                   - 不使用 > 引用符号
                   - 不使用 [] () 链接符号
                
                2. **段落结构**：
                   - 使用自然的段落分隔（双换行）
                   - 每个段落内容完整，逻辑清晰
                   - 段落长度适中（50-200字为宜）
                
                3. **列表表达**：
                   - 使用自然语言表达列表内容
                   - 用"首先"、"其次"、"另外"、"最后"等词汇
                   - 或用"一方面"、"另一方面"等表达
                
                4. **重点强调**：
                   - 使用"需要特别注意的是"、"重点是"等自然表达
                   - 通过语言逻辑而非格式符号来突出重点
                
                5. **代码和技术内容**：
                   - 用自然语言描述代码逻辑
                   - 技术概念用通俗易懂的方式解释
                
                ### 🎯 流式显示适配：
                1. **句子完整性**：每个句子必须完整，不能中途断开
                2. **逻辑连贯性**：前后句子逻辑连贯，便于分段显示
                3. **阅读节奏**：句子长短搭配合理，符合自然阅读习惯
                
                ### 用户需求：
                """ + userMemory + """
                
                请严格按照以上要求，生成一个温暖、智能、完全符合流式输出格式的纯文本回应。
                """;
    }

    /**
     * 后处理：确保输出符合流式格式
     */
    private String postProcessForStreaming(String rawResponse) {
        if (rawResponse == null || rawResponse.trim().isEmpty()) {
            return "抱歉，暂时无法为您提供回答。";
        }

        String processed = rawResponse;

        // 1. 移除所有Markdown格式符号
        processed = removeMarkdownFormatting(processed);

        // 2. 优化段落结构
        processed = optimizeParagraphStructure(processed);

        // 3. 规范化标点和空格
        processed = normalizeText(processed);

        // 4. 确保流式输出友好
        processed = ensureStreamingFriendly(processed);

        return processed.trim();
    }

    /**
     * 移除Markdown格式符号
     */
    private String removeMarkdownFormatting(String text) {
        // 移除标题符号
        text = text.replaceAll("^#{1,6}\\s+", "");
        text = text.replaceAll("\n#{1,6}\\s+", "\n");

        // 移除粗体和斜体
        text = text.replaceAll("\\*\\*([^*]+)\\*\\*", "$1");
        text = text.replaceAll("\\*([^*]+)\\*", "$1");
        text = text.replaceAll("__([^_]+)__", "$1");
        text = text.replaceAll("_([^_]+)_", "$1");

        // 移除代码块
        text = text.replaceAll("```[\\s\\S]*?```", "（代码示例已省略）");
        text = text.replaceAll("`([^`]+)`", "$1");

        // 移除列表符号，转换为自然语言
        text = convertListsToNaturalLanguage(text);

        // 移除引用符号
        text = text.replaceAll("^>\\s+", "");
        text = text.replaceAll("\n>\\s+", "\n");

        // 移除链接格式
        text = text.replaceAll("\\[([^\\]]+)\\]\\([^)]+\\)", "$1");

        return text;
    }

    /**
     * 将列表转换为自然语言
     */
    private String convertListsToNaturalLanguage(String text) {
        String[] lines = text.split("\n");
        StringBuilder result = new StringBuilder();
        List<String> currentList = new ArrayList<>();
        boolean inList = false;

        for (String line : lines) {
            String trimmed = line.trim();

            // 检测列表项
            if (trimmed.matches("^[-*+]\\s+.*") || trimmed.matches("^\\d+\\.\\s+.*")) {
                // 提取列表内容
                String content = trimmed.replaceAll("^[-*+\\d.]+\\s+", "");
                currentList.add(content);
                inList = true;
            } else {
                // 如果之前在处理列表，现在结束了
                if (inList && !currentList.isEmpty()) {
                    result.append(convertListToText(currentList)).append("\n");
                    currentList.clear();
                    inList = false;
                }

                // 添加普通行
                if (!trimmed.isEmpty()) {
                    result.append(line).append("\n");
                }
            }
        }

        // 处理最后的列表
        if (inList && !currentList.isEmpty()) {
            result.append(convertListToText(currentList));
        }

        return result.toString();
    }

    /**
     * 将列表项转换为自然文本
     */
    private String convertListToText(List<String> listItems) {
        if (listItems.isEmpty()) {
            return "";
        }
        if (listItems.size() == 1) {
            return listItems.get(0);
        }

        StringBuilder text = new StringBuilder();
        for (int i = 0; i < listItems.size(); i++) {
            if (i == 0) {
                text.append("首先，").append(listItems.get(i)).append("。");
            } else if (i == listItems.size() - 1) {
                text.append("最后，").append(listItems.get(i)).append("。");
            } else {
                text.append("其次，").append(listItems.get(i)).append("。");
            }

            if (i < listItems.size() - 1) {
                text.append(" ");
            }
        }

        return text.toString();
    }

    /**
     * 优化段落结构
     */
    private String optimizeParagraphStructure(String text) {
        // 规范化换行
        text = text.replaceAll("\r\n", "\n");
        text = text.replaceAll("\r", "\n");

        // 移除多余空行（保留最多两个连续换行）
        text = text.replaceAll("\n{3,}", "\n\n");

        // 确保段落间有适当分隔
        text = text.replaceAll("([。！？])([\\u4e00-\\u9fff])", "$1\n\n$2");

        return text;
    }

    /**
     * 规范化文本
     */
    private String normalizeText(String text) {
        // 规范化中英文间的空格
        text = text.replaceAll("([\\u4e00-\\u9fff])([a-zA-Z0-9])", "$1 $2");
        text = text.replaceAll("([a-zA-Z0-9])([\\u4e00-\\u9fff])", "$1 $2");

        // 规范化标点符号
        text = text.replaceAll("\\s+([，。！？；：])", "$1");
        text = text.replaceAll("([，。！？；：])([\\u4e00-\\u9fff])", "$1 $2");

        // 移除多余空格
        text = text.replaceAll(" +", " ");
        text = text.replaceAll("\n ", "\n");
        text = text.replaceAll(" \n", "\n");

        return text;
    }

    /**
     * 确保流式输出友好
     */
    private String ensureStreamingFriendly(String text) {
        String[] paragraphs = text.split("\n\n");
        StringBuilder result = new StringBuilder();

        for (String paragraph : paragraphs) {
            String trimmed = paragraph.trim();
            if (trimmed.isEmpty()) {
                continue;
            }

            // 如果段落太长，进行分句
            if (trimmed.length() > 300) {
                String[] sentences = trimmed.split("(?<=[。！？])\\s*");
                for (String sentence : sentences) {
                    if (!sentence.trim().isEmpty()) {
                        result.append(sentence.trim()).append("\n\n");
                    }
                }
            } else {
                result.append(trimmed).append("\n\n");
            }
        }

        return result.toString();
    }
}