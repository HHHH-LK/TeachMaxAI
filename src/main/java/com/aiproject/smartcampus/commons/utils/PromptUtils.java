package com.aiproject.smartcampus.commons.utils;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @program: SmartCampus
 * @description: 构建高效提示词工具类
 * @author: lk
 * @create: 2025-05-17 18:35
 * @version: 2.0 - 优化版本
 **/
public class PromptUtils {

    // 常量定义
    private static final String DELIMITER_START = "=== %s 开始 ===";
    private static final String DELIMITER_END = "=== %s 结束 ===";
    private static final String RESPONSE_CAN = "CAN";
    private static final String RESPONSE_CANNOT = "CANNOT";
    private static final String NO_ANSWER = "抱歉，根据当前信息无法准确回答您的问题。";

    /**
     * 构建 RAG 判断提示词 - 判断是否能基于检索内容回答问题
     */
    public static String buildRagCanPrompt(String retrievedContent, String memoryContent, String userQuestion) {
        return String.format(
                "# 任务：判断信息充分性\n\n" +
                        "## 可用信息：\n" +
                        "%s\n%s\n%s\n\n" +
                        "## 用户问题：\n%s\n\n" +
                        "## 判断标准：\n" +
                        "请严格评估上述信息是否足以**完整、准确**地回答用户问题。\n" +
                        "- 信息完整且相关 → 仅回复：%s\n" +
                        "- 信息不足或不相关 → 仅回复：%s\n\n" +
                        "**注意：只回复判断结果，不要其他内容。**",
                formatSection("检索内容", retrievedContent),
                formatSection("对话记忆", memoryContent),
                formatSection("用户问题", userQuestion),
                userQuestion,
                RESPONSE_CAN,
                RESPONSE_CANNOT
        );
    }

    /**
     * 构建 RAG 回答提示词 - 基于检索内容生成答案
     */
    public static String buildRagAnswerPrompt(String retrievedContent, String memoryContent, String userQuestion) {
        return String.format(
                "# 角色：专业RAG智能助手\n\n" +
                        "## 信息来源（按优先级）：\n" +
                        "### 1. 检索内容（主要依据）\n%s\n\n" +
                        "### 2. 对话记忆（上下文参考）\n%s\n\n" +
                        "## 用户问题：\n%s\n\n" +
                        "## 回答要求：\n" +
                        "请按以下结构回答：\n\n" +
                        "**核心观点：**\n" +
                        "[基于检索内容的一句话核心回答]\n\n" +
                        "**详细说明：**\n" +
                        "[结合检索内容的详细解释，必要时引用具体片段]\n\n" +
                        "## 回答原则：\n" +
                        "1. 优先使用检索内容，确保答案准确性\n" +
                        "2. 结合对话记忆保持上下文连贯\n" +
                        "3. 信息不足时可适当补充通用知识\n" +
                        "4. 完全无法回答时，直接返回：\"%s\"",
                formatContent(retrievedContent),
                formatContent(memoryContent),
                userQuestion,
                NO_ANSWER
        );
    }

    /**
     * 构建更智能的系统提示
     */
    public static String buildSystemPrompt() {
        return """
            你是一个智能助手，可以通过工具来获取信息。            
            使用SearchEngine工具的时机：
            1. 用户询问实时信息、新闻、天气、日期等
            2. 需要最新数据或事实验证
            3. 询问具体的产品、服务或技术细节
            4. 当前知识库无法回答的问题   
            搜索时请提取关键词，避免使用过长的查询语句。
            """;
    }

    /**
     * 构建基于记忆的判断提示词
     */
    public static String buildCanPrompt(String memoryContent, String userQuestion) {
        return String.format(
                "# 任务：能力评估\n\n" +
                        "## 可用信息：\n" +
                        "%s\n\n" +
                        "## 用户问题：\n%s\n\n" +
                        "## 评估要求：\n" +
                        "请客观评估是否能基于**自身知识库**和**上述记忆内容**，给出完整且准确的答案。\n\n" +
                        "### 判断标准：\n" +
                        "- ✅ 能够完整准确回答 → 回复：%s\n" +
                        "- ❌ 信息不足或不确定 → 回复：%s\n\n" +
                        "**严格要求：仅输出判断结果，无需解释。**",
                formatSection("对话记忆", memoryContent),
                userQuestion,
                RESPONSE_CAN,
                RESPONSE_CANNOT
        );
    }

    /**
     * 构建基于记忆的回答提示词
     */
    public static String buildAnswerPrompt(String memoryContent, String userQuestion) {
        return String.format(
                "# 角色：智能对话助手\n\n" +
                        "## 对话上下文：\n" +
                        "%s\n\n" +
                        "## 当前问题：\n%s\n\n" +
                        "## 回答要求：\n" +
                        "- 基于对话记忆和自身知识，直接给出最优解答\n" +
                        "- 保持回答的准确性和相关性\n" +
                        "- 语言自然流畅，符合对话习惯\n" +
                        "- 不要输出解释性前缀或后缀",
                formatContent(memoryContent),
                userQuestion
        );
    }

    /**
     * 构建工具调用结果判断提示词
     */
    public static String buildFuncCanPrompt(String toolResult, String memoryContent, String retrievedContent, String userQuestion) {
        return String.format(
                "# 任务：多源信息充分性判断\n\n" +
                        "## 信息汇总：\n" +
                        "%s\n\n%s\n\n%s\n\n" +
                        "## 用户问题：\n%s\n\n" +
                        "## 判断任务：\n" +
                        "综合评估上述**所有信息源**是否足以完整回答用户问题。\n\n" +
                        "### 评估维度：\n" +
                        "1. 信息完整性：是否覆盖问题的关键方面\n" +
                        "2. 信息准确性：数据和事实是否可靠\n" +
                        "3. 信息时效性：是否为最新相关信息\n\n" +
                        "### 判断结果：\n" +
                        "- 满足所有条件 → %s\n" +
                        "- 任一条件不满足 → %s\n\n" +
                        "**仅输出判断结果。**",
                formatSection("工具调用结果", toolResult),
                formatSection("对话记忆", memoryContent),
                formatSection("检索内容", retrievedContent),
                userQuestion,
                RESPONSE_CAN,
                RESPONSE_CANNOT
        );
    }

    /**
     * 构建通用回答提示词（重载方法优化）
     */
    public static String buildAnswerPrompt(String result, String content, String userMessage) {
        // 参数安全处理
        String safeResult = sanitizeInput(result);
        String safeContent = sanitizeInput(content);
        String safeUserMessage = sanitizeInput(userMessage);

        return String.format(
                "# 智能问答任务\n\n" +
                        "## 问题分析结果：\n" +
                        "%s\n\n" +
                        "## 参考信息：\n" +
                        "%s\n\n" +
                        "## 用户问题：\n%s\n\n" +
                        "## 回答指南：\n" +
                        "1. 优先使用分析结果中的准确信息\n" +
                        "2. 结合参考信息提供全面回答\n" +
                        "3. 确保回答逻辑清晰、重点突出\n" +
                        "4. 使用友好专业的语言风格",
                formatContent(safeResult),
                formatContent(safeContent),
                safeUserMessage
        );
    }

    /**
     * 构建对话历史摘要提示词
     */
    public static String buildConversationSummaryPrompt(List<ChatMessage> chatHistory) {
        if (chatHistory == null || chatHistory.isEmpty()) {
            return "暂无对话历史。";
        }

        String historyText = chatHistory.stream()
                .map(msg -> String.format("- %s: %s",
                        msg instanceof UserMessage ? "用户" : "助手",
                        msg.text()))
                .collect(Collectors.joining("\n"));

        return String.format(
                "# 对话摘要任务\n\n" +
                        "## 对话历史：\n" +
                        "%s\n\n" +
                        "## 摘要要求：\n" +
                        "请提取对话中的**关键信息**和**重要上下文**，生成简洁摘要：\n" +
                        "1. 主要讨论话题\n" +
                        "2. 重要事实和结论\n" +
                        "3. 用户关注重点\n" +
                        "4. 待解决问题\n\n" +
                        "**摘要应简洁明了，便于后续对话参考。**",
                historyText
        );
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 格式化内容段落
     */
    private static String formatSection(String title, String content) {
        String safeContent = sanitizeInput(content);
        if (safeContent.isEmpty()) {
            return String.format("### %s：\n[暂无相关信息]", title);
        }
        return String.format("### %s：\n%s", title, safeContent);
    }

    /**
     * 格式化纯内容
     */
    private static String formatContent(String content) {
        String safeContent = sanitizeInput(content);
        return safeContent.isEmpty() ? "[暂无信息]" : safeContent;
    }

    /**
     * 输入参数安全处理
     */
    private static String sanitizeInput(String input) {
        if (input == null) {
            return "";
        }
        return input.trim();
    }

    /**
     * 验证提示词长度（可选的长度控制）
     */
    public static boolean isPromptLengthValid(String prompt, int maxLength) {
        return prompt != null && prompt.length() <= maxLength;
    }

    /**
     * 创建分隔符
     */
    private static String createDelimiter(String type) {
        return String.format(DELIMITER_START, type);
    }
}