package com.aiproject.smartcampus.model.functioncalling.toolutils;

import com.aiproject.smartcampus.mapper.QuestionBankMapper;
import com.aiproject.smartcampus.mapper.StudentKnowledgeMasteryMapper;
import com.aiproject.smartcampus.pojo.bo.KnowledgepointBO;
import com.aiproject.smartcampus.pojo.bo.SimpleKnowledgeAnalysisBO;
import com.aiproject.smartcampus.pojo.bo.TestTaskBO;
import com.aiproject.smartcampus.pojo.po.QuestionBank;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @program: SmartCampus
 * @description: 基于知识点进行智能生成题目 - 支持知识点分析数据
 * @author: lk_hhh
 * @create: 2025-06-29 17:36
 * @version: 3.0
 **/

@Service
@RequiredArgsConstructor
@Slf4j
public class NotMasterTestCreatetoolUtils {

    private final ChatLanguageModel chatLanguageModel;
    private final StudentKnowledgeMasteryMapper studentKnowledgeMasteryMapper;
    private final ObjectMapper objectMapper;
    private final QuestionBankMapper questionBankMapper;
    private final Executor executor = Executors.newFixedThreadPool(5);

    // 常量定义
    private static final String DEFAULT_DIFFICULTY = "medium";
    private static final BigDecimal DEFAULT_SCORE = new BigDecimal("1.0");
    private static final int DEFAULT_CREATOR_ID = 0;
    private static final int DEFAULT_COURSE_ID = 0;
    private static final int MAX_QUESTIONS_PER_KNOWLEDGE_POINT = 3;

    // JSON提取的正则表达式
    private static final Pattern JSON_PATTERN = Pattern.compile("```(?:json)?\\s*([\\s\\S]*?)\\s*```|\\{[\\s\\S]*\\}", Pattern.CASE_INSENSITIVE);

    /**
     * 原有方法 - 保持兼容性
     */
    public String createTest(String studentId) {
        return createTest(studentId, null);
    }

    /**
     * 原有方法 - 保持兼容性
     */
    public String createTest(String studentId, Integer courseId) {
        try {
            validateParameters(studentId);
            List<KnowledgepointBO> notMasterKnowledgepoints =
                    studentKnowledgeMasteryMapper.getNotMasterKnowledgepoints(Integer.valueOf(studentId), courseId);

            String finalPrompt = buildEnhancedPrompt(notMasterKnowledgepoints, courseId);
            log.info("为学生[{}]生成题目提示词完成，知识点数量: [{}]", studentId,
                    notMasterKnowledgepoints != null ? notMasterKnowledgepoints.size() : 0);

            String aiResponse = generateQuestions(finalPrompt);

            final Integer finalCourseId = courseId != null ? courseId : DEFAULT_COURSE_ID;
            CompletableFuture.runAsync(() -> saveToDatabase(aiResponse, finalCourseId), executor)
                    .exceptionally(throwable -> {
                        log.error("异步保存题目到数据库失败", throwable);
                        return null;
                    });

            return aiResponse;

        } catch (Exception e) {
            log.error("为学生[{}]创建测试题目时发生错误", studentId, e);
            throw new RuntimeException("题目生成失败: " + e.getMessage(), e);
        }
    }

    /**
     * 新增方法 - 基于知识点分析数据生成测试题
     * @param testTaskBO 测试任务信息
     * @param analysisDataList 知识点分析数据列表
     * @return 生成的题目内容（JSON格式）
     */
    public String createTest(TestTaskBO testTaskBO, List<SimpleKnowledgeAnalysisBO> analysisDataList) {
        try {
            // 参数校验
            validateTestTaskParameters(testTaskBO);

            log.info("为学生[{}]基于{}个知识点分析数据生成测试题",
                    testTaskBO.getStudentId(),
                    analysisDataList != null ? analysisDataList.size() : 0);

            // 构建基于分析数据的AI提示词
            String finalPrompt = buildAnalysisBasedPrompt(testTaskBO, analysisDataList);

            // 调用AI生成题目
            String aiResponse = generateQuestions(finalPrompt);

            // 异步保存到数据库
            final Integer courseId = testTaskBO.getCcourseId() != null ? testTaskBO.getCcourseId() : DEFAULT_COURSE_ID;
            CompletableFuture.runAsync(() -> saveToDatabase(aiResponse, courseId), executor)
                    .exceptionally(throwable -> {
                        log.error("异步保存题目到数据库失败", throwable);
                        return null;
                    });

            return aiResponse;

        } catch (Exception e) {
            log.error("为学生[{}]基于分析数据创建测试题目时发生错误", testTaskBO.getStudentId(), e);
            throw new RuntimeException("题目生成失败: " + e.getMessage(), e);
        }
    }

    /**
     * 测试任务参数校验
     */
    private void validateTestTaskParameters(TestTaskBO testTaskBO) {
        if (testTaskBO == null) {
            throw new IllegalArgumentException("测试任务信息不能为空");
        }

        if (testTaskBO.getStudentId() == null) {
            throw new IllegalArgumentException("学生ID不能为空");
        }
    }

    /**
     * 构建基于知识点分析数据的AI提示词
     */
    private String buildAnalysisBasedPrompt(TestTaskBO testTaskBO, List<SimpleKnowledgeAnalysisBO> analysisDataList) {
        StringBuilder prompt = new StringBuilder();

        // 系统角色设定和重要提示
        prompt.append("你是一个专业的智能教育助手。请注意：你的回复必须是纯JSON格式，不要包含任何markdown标记、代码块符号或其他文本。\n\n");
        prompt.append("⚠️ 重要：请直接返回JSON对象，不要使用```json```代码块包装！\n\n");

        // 任务描述 - 针对分析数据优化
        appendAnalysisTaskDescription(prompt);

        // 学生信息
        appendStudentInfo(prompt, testTaskBO);

        // 知识点分析信息
        appendKnowledgeAnalysisInfo(prompt, analysisDataList);

        // 数据库结构要求
        appendDatabaseStructureRequirements(prompt, testTaskBO.getCcourseId());

        // 题目类型详细说明
        appendQuestionTypeDetails(prompt);

        // 难度等级说明
        appendDifficultyLevelDetails(prompt);

        // JSON格式示例
        appendJsonFormatExample(prompt);

        // 基于分析数据的生成规则
        appendAnalysisBasedQualityRequirements(prompt);

        // 最终提示
        prompt.append("🔥 请严格按照上述要求，基于学生的知识点掌握情况生成个性化题目，直接返回JSON对象（不要使用代码块格式）：");

        return prompt.toString();
    }

    /**
     * 添加基于分析的任务描述
     */
    private void appendAnalysisTaskDescription(StringBuilder prompt) {
        prompt.append("## 📋 任务要求\n")
                .append("请根据学生的知识点分析数据，生成个性化的练习题目。重点关注以下策略：\n\n")
                .append("### 基本要求：\n")
                .append("- 根据正确率调整题目难度：正确率低的知识点生成基础题，正确率高的生成进阶题\n")
                .append("- 优先为答题数量少的知识点生成题目，增加练习机会\n")
                .append("- 为错误率高的知识点生成更多解析详细的题目\n")
                .append("- 题目内容准确、清晰，符合教育标准\n")
                .append("- 选项设置合理，干扰项有适当迷惑性但不误导\n")
                .append("- 提供详细的答案解析，帮助学生理解知识点\n\n");
    }

    /**
     * 添加学生信息
     */
    private void appendStudentInfo(StringBuilder prompt, TestTaskBO testTaskBO) {
        prompt.append("## 👤 学生信息\n")
                .append(String.format("- 学生ID: %d\n", testTaskBO.getStudentId()));

        if (testTaskBO.getCcourseId() != null) {
            prompt.append(String.format("- 课程ID: %d\n", testTaskBO.getCcourseId()));
        }
        prompt.append("\n");
    }

    /**
     * 添加知识点分析信息
     */
    private void appendKnowledgeAnalysisInfo(StringBuilder prompt, List<SimpleKnowledgeAnalysisBO> analysisDataList) {
        prompt.append("## 📊 知识点分析数据\n");

        if (analysisDataList != null && !analysisDataList.isEmpty()) {
            for (int i = 0; i < analysisDataList.size(); i++) {
                SimpleKnowledgeAnalysisBO analysis = analysisDataList.get(i);
                prompt.append(String.format("**%d. %s**\n", i + 1, analysis.getPointName()))
                        .append(String.format("   - 知识点ID: %s\n", analysis.getPointId()))
                        .append(String.format("   - 难度等级: %s\n", analysis.getDifficultyLevel() != null ? analysis.getDifficultyLevel() : "未设置"))
                        .append(String.format("   - 已答题数: %d\n", analysis.getTotalAnswered() != null ? analysis.getTotalAnswered() : 0))
                        .append(String.format("   - 正确题数: %d\n", analysis.getCorrectCount() != null ? analysis.getCorrectCount() : 0))
                        .append(String.format("   - 错误题数: %d\n", analysis.getWrongCount() != null ? analysis.getWrongCount() : 0))
                        .append(String.format("   - 正确率: %.1f%%\n", analysis.getAccuracyRate() != null ? analysis.getAccuracyRate() : 0.0));

                if (StringUtils.hasText(analysis.getDescription())) {
                    prompt.append(String.format("   - 描述: %s\n", analysis.getDescription()));
                }

                if (StringUtils.hasText(analysis.getKeywords())) {
                    prompt.append(String.format("   - 关键词: %s\n", analysis.getKeywords()));
                }

                // 基于数据给出生成建议
                appendGenerationSuggestion(prompt, analysis);
                prompt.append("\n");
            }
        } else {
            prompt.append("⚠️ 暂无知识点分析数据，请生成通用基础练习题目\n\n");
        }
    }

    /**
     * 基于分析数据添加生成建议
     */
    private void appendGenerationSuggestion(StringBuilder prompt, SimpleKnowledgeAnalysisBO analysis) {
        prompt.append("   - 生成建议: ");

        Double accuracyRate = analysis.getAccuracyRate() != null ? analysis.getAccuracyRate() : 0.0;
        Integer totalAnswered = analysis.getTotalAnswered() != null ? analysis.getTotalAnswered() : 0;

        if (totalAnswered < 5) {
            prompt.append("练习不足，建议生成2-3道基础题目增加练习");
        } else if (accuracyRate < 60.0) {
            prompt.append("掌握较差，建议生成简单题目并提供详细解析");
        } else if (accuracyRate < 80.0) {
            prompt.append("掌握一般，建议生成中等难度题目巩固理解");
        } else {
            prompt.append("掌握良好，建议生成进阶题目提升能力");
        }
        prompt.append("\n");
    }

    /**
     * 添加基于分析数据的质量要求
     */
    private void appendAnalysisBasedQualityRequirements(StringBuilder prompt) {
        prompt.append("## ✅ 基于分析数据的生成质量要求\n")
                .append("1. **个性化**: 根据学生的知识点掌握情况调整题目难度和数量\n")
                .append("2. **针对性**: 重点关注正确率低和练习少的知识点\n")
                .append("3. **层次性**: 同一知识点根据掌握情况设置合适的难度梯度\n")
                .append("4. **教育性**: 为薄弱知识点提供更详细的解析和指导\n")
                .append("5. **规范性**: 严格遵循JSON格式，字段完整\n")
                .append("6. **多样性**: 合理搭配不同题型，避免单一\n")
                .append("7. **适度性**: 每个知识点生成").append(MAX_QUESTIONS_PER_KNOWLEDGE_POINT).append("道题目，重点知识点可适当增加\n")
                .append("8. **智能化**: 正确率<60%的知识点优先生成easy难度，正确率>80%的生成medium或hard难度\n\n");
    }

    // 保留原有的所有其他方法...
    private void validateParameters(String studentId) {
        if (!StringUtils.hasText(studentId)) {
            throw new IllegalArgumentException("学生ID不能为空");
        }

        try {
            Integer.valueOf(studentId);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("学生ID必须是有效的数字");
        }
    }

    private String buildEnhancedPrompt(List<KnowledgepointBO> knowledgepoints, Integer courseId) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是一个专业的智能教育助手。请注意：你的回复必须是纯JSON格式，不要包含任何markdown标记、代码块符号或其他文本。\n\n");
        prompt.append("⚠️ 重要：请直接返回JSON对象，不要使用```json```代码块包装！\n\n");

        appendTaskDescription(prompt);
        appendKnowledgePointsInfo(prompt, knowledgepoints);
        appendDatabaseStructureRequirements(prompt, courseId);
        appendQuestionTypeDetails(prompt);
        appendDifficultyLevelDetails(prompt);
        appendJsonFormatExample(prompt);
        appendQualityRequirements(prompt);

        prompt.append("🔥 请严格按照上述要求生成题目，直接返回JSON对象（不要使用代码块格式）：");
        return prompt.toString();
    }

    private void appendTaskDescription(StringBuilder prompt) {
        prompt.append("## 📋 任务要求\n")
                .append("请根据学生未掌握的知识点，生成高质量的个性化练习题目，帮助学生巩固和提升相关知识点的掌握程度。\n\n")
                .append("### 基本要求：\n")
                .append("- 每个知识点生成2-3道不同难度的题目，循序渐进\n")
                .append("- 题目内容准确、清晰，符合教育标准\n")
                .append("- 选项设置合理，干扰项有适当迷惑性但不误导\n")
                .append("- 提供详细的答案解析，帮助学生理解知识点\n")
                .append("- 优先生成基础题目，确保学生能掌握核心概念\n\n");
    }

    private void appendKnowledgePointsInfo(StringBuilder prompt, List<KnowledgepointBO> knowledgepoints) {
        prompt.append("## 🎯 学生未掌握的知识点\n");

        if (knowledgepoints != null && !knowledgepoints.isEmpty()) {
            for (int i = 0; i < knowledgepoints.size(); i++) {
                KnowledgepointBO kp = knowledgepoints.get(i);
                prompt.append(String.format("**%d. %s**\n", i + 1, kp.getKnowledgepointName()))
                        .append(String.format("   - 知识点ID: %s\n",
                                kp.getPointId() != null ? kp.getPointId() : "系统自动分配"));

                if (StringUtils.hasText(kp.getDescription())) {
                    prompt.append(String.format("   - 描述: %s\n", kp.getDescription()));
                }
                prompt.append("\n");
            }
        } else {
            prompt.append("⚠️ 暂无未掌握的知识点，请生成通用基础练习题目\n\n");
        }
    }

    private void appendDatabaseStructureRequirements(StringBuilder prompt, Integer courseId) {
        prompt.append("## 💾 数据库字段要求\n")
                .append("生成的题目必须严格符合以下数据库表结构 `question_bank`：\n\n")
                .append("### 必填字段：\n")
                .append("- **point_id** (Integer): 知识点ID，必须对应上述知识点\n")
                .append("- **question_type** (String): 题目类型枚举值\n")
                .append("- **question_content** (String): 题目内容，清晰准确\n")
                .append("- **correct_answer** (String): 正确答案\n")
                .append("- **explanation** (String): 详细的答案解析\n\n")
                .append("### 可选字段：\n")
                .append("- **course_id** (Integer): 课程ID")
                .append(courseId != null ? String.format("，本次生成使用: %d\n", courseId) : "，系统默认值\n")
                .append("- **question_options** (Array): 题目选项，仅选择题和判断题需要\n")
                .append("- **difficulty_level** (String): 难度等级枚举值\n")
                .append("- **score_points** (Number): 题目分值，默认1.0\n\n");
    }

    private void appendQuestionTypeDetails(StringBuilder prompt) {
        prompt.append("## 📝 题目类型说明\n")
                .append("支持以下5种题目类型，请根据知识点特性选择合适的类型：\n\n")
                .append("### 1. single_choice (单选题)\n")
                .append("- 适用于概念理解、事实记忆类知识点\n")
                .append("- 必须提供4个选项（A、B、C、D），仅一个正确答案\n")
                .append("- correct_answer为选项标签（如\"A\"）\n\n")
                .append("### 2. multiple_choice (多选题)\n")
                .append("- 适用于综合性、多方面特征的知识点\n")
                .append("- 提供4-6个选项，可能有2-3个正确答案\n")
                .append("- correct_answer为正确选项标签数组（如[\"A\",\"B\"]）\n\n")
                .append("### 3. true_false (判断题)\n")
                .append("- 适用于概念判断、原理验证\n")
                .append("- 固定选项：A.正确 B.错误\n")
                .append("- correct_answer为\"A\"或\"B\"\n\n")
                .append("### 4. fill_blank (填空题)\n")
                .append("- 适用于关键概念、公式、术语记忆\n")
                .append("- 题目中用\"_____\"标记空白处\n")
                .append("- correct_answer为标准答案字符串\n\n")
                .append("### 5. short_answer (简答题)\n")
                .append("- 适用于原理解释、过程描述类知识点\n")
                .append("- 要求学生用简短文字回答\n")
                .append("- correct_answer为标准答案要点\n\n");
    }

    private void appendDifficultyLevelDetails(StringBuilder prompt) {
        prompt.append("## ⭐ 难度等级说明\n")
                .append("请根据题目复杂度和学生水平合理设置难度：\n\n")
                .append("- **easy**: 基础概念理解，直接应用基本知识\n")
                .append("- **medium**: 知识点综合运用，需要一定分析能力\n")
                .append("- **hard**: 复杂应用场景，需要深度思考和推理\n\n");
    }

    private void appendJsonFormatExample(StringBuilder prompt) {
        prompt.append("## 📄 JSON格式要求\n")
                .append("请严格按照以下格式返回，直接输出JSON对象（不要包装在代码块中）：\n\n")
                .append("{\n")
                .append("  \"questions\": [\n")
                .append("    {\n")
                .append("      \"point_id\": 1,\n")
                .append("      \"question_type\": \"single_choice\",\n")
                .append("      \"question_content\": \"以下哪个选项最能准确描述XXX概念？\",\n")
                .append("      \"question_options\": [\n")
                .append("        {\"label\": \"A\", \"content\": \"选项A内容\", \"is_correct\": true},\n")
                .append("        {\"label\": \"B\", \"content\": \"选项B内容\", \"is_correct\": false},\n")
                .append("        {\"label\": \"C\", \"content\": \"选项C内容\", \"is_correct\": false},\n")
                .append("        {\"label\": \"D\", \"content\": \"选项D内容\", \"is_correct\": false}\n")
                .append("      ],\n")
                .append("      \"correct_answer\": \"A\",\n")
                .append("      \"explanation\": \"详细解析：选择A的原因是...\",\n")
                .append("      \"difficulty_level\": \"easy\",\n")
                .append("      \"score_points\": 1.0\n")
                .append("    }\n")
                .append("  ]\n")
                .append("}\n\n");
    }

    private void appendQualityRequirements(StringBuilder prompt) {
        prompt.append("## ✅ 生成质量要求\n")
                .append("1. **准确性**: 题目内容科学准确，答案无争议\n")
                .append("2. **针对性**: 紧密围绕未掌握的知识点设计\n")
                .append("3. **层次性**: 同一知识点的题目要有难度梯度\n")
                .append("4. **教育性**: 通过解析帮助学生理解原理\n")
                .append("5. **规范性**: 严格遵循JSON格式，字段完整\n")
                .append("6. **多样性**: 合理搭配不同题型，避免单一\n")
                .append("7. **适度性**: 每个知识点不超过").append(MAX_QUESTIONS_PER_KNOWLEDGE_POINT).append("道题目\n\n");
    }

    /**
     * 调用AI模型生成题目，支持重试机制
     */

    /**
     * 调用AI模型生成题目，支持重试机制
     */
    @Retryable(value = {Exception.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    private String generateQuestions(String prompt) {
        try {
            ChatResponse response = chatLanguageModel.chat(UserMessage.from(prompt));
            String content = response.aiMessage().text();

            String cleanedContent = extractAndCleanJson(content);
            validateAiResponse(cleanedContent);

            log.info("AI题目生成成功，清理后内容长度: {}", cleanedContent.length());
            return cleanedContent;
        } catch (Exception e) {
            log.error("AI题目生成失败", e);
            throw new RuntimeException("AI服务暂时不可用，请稍后重试", e);
        }
    }

    /**
     * 提取和清理JSON内容
     * 处理AI可能返回的markdown代码块格式
     */

    /**
     * 提取和清理JSON内容
     * 处理AI可能返回的markdown代码块格式
     */
    private String extractAndCleanJson(String content) {
        if (!StringUtils.hasText(content)) {
            throw new IllegalStateException("AI返回内容为空");
        }

        log.debug("原始AI返回内容: {}", content);

        String trimmedContent = content.trim();
        String jsonContent = null;

        Matcher matcher = JSON_PATTERN.matcher(trimmedContent);
        if (matcher.find()) {
            if (matcher.group(1) != null) {
                jsonContent = matcher.group(1).trim();
                log.debug("从代码块中提取JSON: {}", jsonContent);
            } else {
                jsonContent = matcher.group(0).trim();
                log.debug("提取到直接JSON: {}", jsonContent);
            }
        }

        if (jsonContent == null) {
            if (trimmedContent.startsWith("{") && trimmedContent.endsWith("}")) {
                jsonContent = trimmedContent;
                log.debug("内容本身就是JSON格式");
            }
        }

        if (jsonContent == null) {
            int startIndex = trimmedContent.indexOf('{');
            int endIndex = trimmedContent.lastIndexOf('}');
            if (startIndex != -1 && endIndex != -1 && startIndex < endIndex) {
                jsonContent = trimmedContent.substring(startIndex, endIndex + 1);
                log.debug("通过位置提取JSON: {}", jsonContent);
            }
        }

        if (jsonContent == null) {
            log.error("无法从AI响应中提取有效的JSON内容，原始内容: {}", content);
            throw new IllegalStateException("AI返回内容不包含有效的JSON格式");
        }

        jsonContent = cleanJsonContent(jsonContent);

        log.debug("最终清理后的JSON: {}", jsonContent);
        return jsonContent;
    }

    /**
     * 清理JSON内容中的常见问题
     */

    /**
     * 清理JSON内容中的常见问题
     */
    private String cleanJsonContent(String jsonContent) {
        if (!StringUtils.hasText(jsonContent)) {
            return jsonContent;
        }

        jsonContent = jsonContent.replaceAll("```json\\s*", "")
                .replaceAll("```\\s*", "")
                .replaceAll("^```", "")
                .replaceAll("```$", "");

        jsonContent = jsonContent.trim();
        jsonContent = jsonContent.replaceAll("(?m)^\\s*//.*$", "");

        return jsonContent;
    }

    /**
     * 验证AI返回的内容格式
     */

    /**
     * 验证AI返回的内容格式
     */
    private void validateAiResponse(String content) {
        if (!StringUtils.hasText(content)) {
            throw new IllegalStateException("AI返回内容为空");
        }

        try {
            JsonNode rootNode = objectMapper.readTree(content);
            if (!rootNode.has("questions")) {
                throw new IllegalStateException("AI返回格式不正确：缺少questions字段");
            }

            JsonNode questionsNode = rootNode.get("questions");
            if (!questionsNode.isArray() || questionsNode.size() == 0) {
                throw new IllegalStateException("AI返回的questions字段格式不正确或为空");
            }

            log.info("AI返回内容验证通过，包含{}道题目", questionsNode.size());
        } catch (JsonProcessingException e) {
            log.error("JSON解析失败，内容: {}", content, e);
            throw new IllegalStateException("AI返回内容不是有效的JSON格式: " + e.getMessage(), e);
        }
    }

    /**
     * 保存题目到数据库（事务处理）
     */

    /**
     * 保存题目到数据库（事务处理）
     */
    @Transactional(rollbackFor = Exception.class)
    public void saveToDatabase(String content, Integer courseId) {
        try {
            JsonNode rootNode = objectMapper.readTree(content);
            JsonNode questionsNode = rootNode.get("questions");

            if (questionsNode != null && questionsNode.isArray()) {
                int savedCount = 0;
                int errorCount = 0;

                for (JsonNode questionNode : questionsNode) {
                    try {
                        if (saveQuestionToDatabase(questionNode, courseId)) {
                            savedCount++;
                        } else {
                            errorCount++;
                        }
                    } catch (Exception e) {
                        log.error("保存单个题目失败: {}", questionNode.toString(), e);
                        errorCount++;
                    }
                }

                log.info("题目保存完成 - 成功: {}, 失败: {}", savedCount, errorCount);
            }
        } catch (Exception e) {
            log.error("解析AI生成题目并写入数据库时出错", e);
            throw new RuntimeException("题目保存失败: " + e.getMessage(), e);
        }
    }

    /**
     * 保存单个题目到数据库
     */

    /**
     * 保存单个题目到数据库
     */
    private boolean saveQuestionToDatabase(JsonNode questionNode, Integer courseId) {
        try {
            QuestionBank questionBank = buildQuestionBankFromJson(questionNode, courseId);

            validateQuestionBank(questionBank);
            questionBank.setCreatedAt(LocalDateTime.now());
            questionBank.setUpdatedAt(LocalDateTime.now());
            questionBankMapper.insert(questionBank);
            log.debug("成功保存题目: {}", questionBank.getQuestionContent());
            return true;

        } catch (Exception e) {
            log.error("保存单个题目失败", e);
            return false;
        }
    }

    /**
     * 从JSON构建QuestionBank对象 - 完全匹配实体类
     */

    /**
     * 从JSON构建QuestionBank对象 - 完全匹配实体类
     */
    private QuestionBank buildQuestionBankFromJson(JsonNode questionNode, Integer courseId)
            throws JsonProcessingException {
        QuestionBank questionBank = new QuestionBank();

        questionBank.setCourseId(courseId != null ? courseId : DEFAULT_COURSE_ID);
        questionBank.setPointId(getIntValue(questionNode, "point_id"));
        questionBank.setQuestionContent(getStringValue(questionNode, "question_content"));
        questionBank.setExplanation(getStringValue(questionNode, "explanation"));
        questionBank.setCreatedBy(DEFAULT_CREATOR_ID);

        questionBank.setQuestionType(QuestionBank.QuestionType.fromValue(
                getStringValue(questionNode, "question_type")));
        questionBank.setDifficultyLevel(QuestionBank.DifficultyLevel.fromValue(
                getStringValue(questionNode, "difficulty_level", DEFAULT_DIFFICULTY)));

        String scoreStr = getStringValue(questionNode, "score_points", DEFAULT_SCORE.toString());
        questionBank.setScorePoints(new BigDecimal(scoreStr));

        JsonNode optionsNode = questionNode.get("question_options");
        if (optionsNode != null && !optionsNode.isNull()) {
            questionBank.setQuestionOptions(objectMapper.writeValueAsString(optionsNode));
        }

        JsonNode answerNode = questionNode.get("correct_answer");
        if (answerNode != null && !answerNode.isNull()) {
            String answerJson;
            if (answerNode.isTextual()) {
                answerJson = objectMapper.writeValueAsString(answerNode.asText());
            } else {
                answerJson = objectMapper.writeValueAsString(answerNode);
            }
            questionBank.setCorrectAnswer(answerJson);
        }

        return questionBank;
    }

    /**
     * 安全获取整数值
     */

    /**
     * 安全获取整数值
     */
    private Integer getIntValue(JsonNode node, String fieldName) {
        JsonNode fieldNode = node.get(fieldName);
        if (fieldNode != null && !fieldNode.isNull()) {
            return fieldNode.asInt();
        }
        throw new IllegalArgumentException("必填字段 " + fieldName + " 不能为空");
    }

    /**
     * 安全获取字符串值
     */

    /**
     * 安全获取字符串值
     */
    private String getStringValue(JsonNode node, String fieldName) {
        return getStringValue(node, fieldName, null);
    }

    /**
     * 安全获取字符串值（带默认值）
     */

    /**
     * 安全获取字符串值（带默认值）
     */
    private String getStringValue(JsonNode node, String fieldName, String defaultValue) {
        JsonNode fieldNode = node.get(fieldName);
        if (fieldNode != null && !fieldNode.isNull()) {
            return fieldNode.asText();
        }
        if (defaultValue != null) {
            return defaultValue;
        }
        throw new IllegalArgumentException("必填字段 " + fieldName + " 不能为空");
    }

    /**
     * 验证题目数据完整性
     */

    /**
     * 验证题目数据完整性
     */
    private void validateQuestionBank(QuestionBank questionBank) {
        if (!StringUtils.hasText(questionBank.getQuestionContent())) {
            throw new IllegalArgumentException("题目内容不能为空");
        }

        if (questionBank.getQuestionType() == null) {
            throw new IllegalArgumentException("题目类型不能为空");
        }

        if (!StringUtils.hasText(questionBank.getExplanation())) {
            throw new IllegalArgumentException("题目解析不能为空");
        }

        if (questionBank.getPointId() == null) {
            throw new IllegalArgumentException("知识点ID不能为空");
        }

        if ((questionBank.isChoiceQuestion() || questionBank.isTrueFalse())
                && !StringUtils.hasText(questionBank.getQuestionOptions())) {
            throw new IllegalArgumentException("选择题和判断题必须设置题目选项");
        }
    }

    /**
     * 优雅关闭线程池
     */

    /**
     * 优雅关闭线程池
     */
    public void shutdown() {
        if (executor instanceof ExecutorService) {
            ((ExecutorService) executor).shutdown();
        }
    }
}