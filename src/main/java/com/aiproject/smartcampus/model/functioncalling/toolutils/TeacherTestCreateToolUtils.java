package com.aiproject.smartcampus.model.functioncalling.toolutils;

import com.aiproject.smartcampus.mapper.QuestionBankMapper;
import com.aiproject.smartcampus.mapper.ChapterQuestionMapper;
import com.aiproject.smartcampus.pojo.po.QuestionBank;
import com.aiproject.smartcampus.pojo.po.ChapterQuestion;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @program: SmartCampus
 * @description: 教师：测试题智能生成
 * @author: lk_hhh
 * @create: 2025-07-11 18:31
 **/

@Component
@Slf4j
@RequiredArgsConstructor
public class TeacherTestCreateToolUtils {

    private static final ChapterQuestion.QuestionType TEST_QUESTION_TYPE = ChapterQuestion.QuestionType.TEST ;
    private final ChatLanguageModel chatLanguageModel;
    private final QuestionBankMapper questionBankMapper;
    private final ChapterQuestionMapper chapterQuestionMapper;
    private final ObjectMapper objectMapper;
    private final Executor executor = Executors.newFixedThreadPool(3);

    // 常量定义
    private static final String DEFAULT_DIFFICULTY = "medium";
    private static final BigDecimal DEFAULT_SCORE = new BigDecimal("2.0");
    private static final Pattern JSON_PATTERN = Pattern.compile("```(?:json)?\\s*([\\s\\S]*?)\\s*```|\\{[\\s\\S]*\\}", Pattern.CASE_INSENSITIVE);

    /**
     * 生成章节测试题
     *
     * @param content 教师需求内容
     * @param classAndChapterInfo 课程章节知识点信息
     * @param wrongKnowledgeInfo 班级错误知识点信息
     * @return 生成的测试题JSON格式
     */
    public String createChapterTest(String content, String classAndChapterInfo, String wrongKnowledgeInfo) {
        try {
            log.info("开始生成章节测试题");

            // 构建AI提示词
            String prompt = buildTestPrompt(content, classAndChapterInfo, wrongKnowledgeInfo);

            // 调用AI生成测试题
            String aiResponse = generateTestQuestions(prompt);

            log.info("章节测试题生成成功，内容长度：{}", aiResponse.length());
            return aiResponse;

        } catch (Exception e) {
            log.error("生成章节测试题失败", e);
            throw new RuntimeException("测试题生成失败: " + e.getMessage(), e);
        }
    }

    /**
     * 生成章节测试题并保存到数据库
     *
     * @param content 教师需求内容
     * @param classAndChapterInfo 课程章节知识点信息
     * @param wrongKnowledgeInfo 班级错误知识点信息
     * @param courseId 课程ID
     * @param chapterId 章节ID
     * @param teacherId 教师ID
     * @return 生成的测试题和保存结果
     */
    public String createChapterTestWithSave(String content, String classAndChapterInfo,
                                            String wrongKnowledgeInfo, Integer courseId,
                                            Integer chapterId, Integer teacherId) {
        try {
            // 生成测试题
            String aiResponse = createChapterTest(content, classAndChapterInfo, wrongKnowledgeInfo);

            // 异步保存到数据库
            CompletableFuture.runAsync(() ->
                            saveTestToDatabase(aiResponse, courseId, chapterId, teacherId), executor)
                    .exceptionally(throwable -> {
                        log.error("异步保存测试题到数据库失败", throwable);
                        return null;
                    });

            return aiResponse;

        } catch (Exception e) {
            log.error("生成并保存章节测试题失败", e);
            throw new RuntimeException("测试题生成保存失败: " + e.getMessage(), e);
        }
    }

    /**
     * 构建测试题生成的AI提示词
     */
    private String buildTestPrompt(String content, String classAndChapterInfo, String wrongKnowledgeInfo) {
        StringBuilder prompt = new StringBuilder();

        // 系统角色设定
        prompt.append("你是一名专业的教育测试专家。请根据以下信息生成高质量的章节测试题。\n");
        prompt.append("⚠️ 重要：请直接返回JSON对象，不要使用```json```代码块包装！\n\n");

        // 任务描述
        appendTestTaskDescription(prompt);

        // 教师需求
        appendTeacherRequirement(prompt, content);

        // 章节知识点信息
        appendChapterInfo(prompt, classAndChapterInfo);

        // 错误知识点信息
        appendWrongKnowledgeInfo(prompt, wrongKnowledgeInfo);

        // 测试题生成要求
        appendTestRequirements(prompt);

        // JSON格式要求
        appendTestJsonFormat(prompt);

        // 质量标准
        appendTestQualityStandards(prompt);

        prompt.append("🎯 请严格按照上述要求生成章节测试题，直接返回JSON对象：");

        return prompt.toString();
    }

    /**
     * 添加测试任务描述
     */
    private void appendTestTaskDescription(StringBuilder prompt) {
        prompt.append("## 📋 任务要求\n")
                .append("生成一套完整的章节测试题，用于检验学生对章节知识点的掌握情况。\n\n")
                .append("### 测试目标：\n")
                .append("- 全面覆盖章节重点知识点\n")
                .append("- 重点关注班级易错知识点\n")
                .append("- 难度适中，区分度良好\n")
                .append("- 题型多样，全面评估\n")
                .append("- 时间控制在30-45分钟内完成\n\n");
    }

    /**
     * 添加教师需求
     */
    private void appendTeacherRequirement(StringBuilder prompt, String content) {
        prompt.append("## 👨‍🏫 教师需求\n");
        if (StringUtils.hasText(content)) {
            prompt.append(content).append("\n\n");
        } else {
            prompt.append("生成标准章节测试题，重点考查核心知识点掌握情况\n\n");
        }
    }

    /**
     * 添加章节信息
     */
    private void appendChapterInfo(StringBuilder prompt, String classAndChapterInfo) {
        prompt.append("## 📚 章节知识点信息\n")
                .append(classAndChapterInfo)
                .append("\n\n");
    }

    /**
     * 添加错误知识点信息
     */
    private void appendWrongKnowledgeInfo(StringBuilder prompt, String wrongKnowledgeInfo) {
        prompt.append("## ⚠️ 班级易错知识点\n")
                .append(wrongKnowledgeInfo)
                .append("\n\n");
    }

    /**
     * 添加测试题要求
     */
    private void appendTestRequirements(StringBuilder prompt) {
        prompt.append("## 📝 测试题生成要求\n")
                .append("### 题型分布建议：\n")
                .append("- **单选题（40%）**：基础概念理解，每题2分\n")
                .append("- **多选题（20%）**：综合知识运用，每题3分\n")
                .append("- **判断题（20%）**：概念辨析，每题1分\n")
                .append("- **填空题（10%）**：关键术语记忆，每题2分\n")
                .append("- **简答题（10%）**：原理解释应用，每题5分\n\n")
                .append("### 难度分布：\n")
                .append("- **简单题（30%）**：基础知识点直接考查\n")
                .append("- **中等题（50%）**：知识点综合运用\n")
                .append("- **困难题（20%）**：深度理解和分析\n\n")
                .append("### 重点关注：\n")
                .append("- 优先覆盖核心知识点（is_core=1）\n")
                .append("- 重点考查班级易错知识点\n")
                .append("- 设置合适的干扰项和迷惑选项\n")
                .append("- 提供详细的解题思路和解析\n\n");
    }

    /**
     * 添加JSON格式要求
     */
    private void appendTestJsonFormat(StringBuilder prompt) {
        prompt.append("## 📄 JSON格式要求\n")
                .append("请严格按照以下格式返回：\n\n")
                .append("{\n")
                .append("  \"test_info\": {\n")
                .append("    \"test_title\": \"章节测试题标题\",\n")
                .append("    \"total_score\": 100,\n")
                .append("    \"estimated_time\": 40,\n")
                .append("    \"difficulty_level\": \"medium\"\n")
                .append("  },\n")
                .append("  \"questions\": [\n")
                .append("    {\n")
                .append("      \"point_id\": 1,\n")
                .append("      \"question_type\": \"single_choice\",\n")
                .append("      \"question_content\": \"题目内容\",\n")
                .append("      \"question_options\": [\n")
                .append("        {\"label\": \"A\", \"content\": \"选项A\", \"is_correct\": true},\n")
                .append("        {\"label\": \"B\", \"content\": \"选项B\", \"is_correct\": false}\n")
                .append("      ],\n")
                .append("      \"correct_answer\": \"A\",\n")
                .append("      \"explanation\": \"详细解析\",\n")
                .append("      \"difficulty_level\": \"medium\",\n")
                .append("      \"score_points\": 2.0\n")
                .append("    }\n")
                .append("  ]\n")
                .append("}\n\n");
    }

    /**
     * 添加质量标准
     */
    private void appendTestQualityStandards(StringBuilder prompt) {
        prompt.append("## ✅ 质量标准\n")
                .append("1. **科学性**：题目内容准确无误，符合学科标准\n")
                .append("2. **针对性**：紧密结合章节知识点和易错点\n")
                .append("3. **区分性**：能有效区分不同水平的学生\n")
                .append("4. **完整性**：包含完整的题目、选项、答案、解析\n")
                .append("5. **规范性**：严格遵循JSON格式要求\n")
                .append("6. **教育性**：通过解析促进学生理解\n\n");
    }

    /**
     * 调用AI生成测试题
     */
    private String generateTestQuestions(String prompt) {
        try {
            String response = chatLanguageModel.chat(prompt);
            String cleanedContent = extractAndCleanJson(response);
            validateJsonResponse(cleanedContent);
            return cleanedContent;
        } catch (Exception e) {
            log.error("AI生成测试题失败", e);
            throw new RuntimeException("AI服务暂时不可用，请稍后重试", e);
        }
    }

    /**
     * 提取和清理JSON内容
     */
    private String extractAndCleanJson(String content) {
        if (!StringUtils.hasText(content)) {
            throw new IllegalStateException("AI返回内容为空");
        }

        String trimmedContent = content.trim();
        String jsonContent = null;

        Matcher matcher = JSON_PATTERN.matcher(trimmedContent);
        if (matcher.find()) {
            jsonContent = matcher.group(1) != null ? matcher.group(1).trim() : matcher.group(0).trim();
        }

        if (jsonContent == null) {
            if (trimmedContent.startsWith("{") && trimmedContent.endsWith("}")) {
                jsonContent = trimmedContent;
            }
        }

        if (jsonContent == null) {
            int startIndex = trimmedContent.indexOf('{');
            int endIndex = trimmedContent.lastIndexOf('}');
            if (startIndex != -1 && endIndex != -1 && startIndex < endIndex) {
                jsonContent = trimmedContent.substring(startIndex, endIndex + 1);
            }
        }

        if (jsonContent == null) {
            throw new IllegalStateException("AI返回内容不包含有效的JSON格式");
        }

        return cleanJsonContent(jsonContent);
    }

    /**
     * 清理JSON内容
     */
    private String cleanJsonContent(String jsonContent) {
        return jsonContent.replaceAll("```json\\s*", "")
                .replaceAll("```\\s*", "")
                .replaceAll("(?m)^\\s*//.*$", "")
                .trim();
    }

    /**
     * 验证JSON响应
     */
    private void validateJsonResponse(String content) {
        try {
            JsonNode rootNode = objectMapper.readTree(content);
            if (!rootNode.has("questions")) {
                throw new IllegalStateException("AI返回格式不正确：缺少questions字段");
            }
        } catch (Exception e) {
            throw new IllegalStateException("AI返回内容不是有效的JSON格式: " + e.getMessage(), e);
        }
    }

    /**
     * 保存测试题到数据库
     */
    /**
     * 调整后的TeacherTestCreateToolUtils中的保存方法
     */
    @Transactional(rollbackFor = Exception.class)
    public void saveTestToDatabase(String content, Integer courseId, Integer chapterId, Integer teacherId) {
        try {
            JsonNode rootNode = objectMapper.readTree(content);
            JsonNode questionsNode = rootNode.get("questions");

            if (questionsNode != null && questionsNode.isArray()) {
                List<ChapterQuestion> chapterQuestions = new ArrayList<>();
                int savedCount = 0;

                for (JsonNode questionNode : questionsNode) {
                    try {
                        // 1. 保存到 question_bank 表
                        QuestionBank questionBank = buildQuestionBankFromJson(questionNode, courseId, teacherId);
                        questionBankMapper.insert(questionBank);

                        // 2. 准备章节题目关系数据
                        if (chapterId != null) {
                            ChapterQuestion chapterQuestion = new ChapterQuestion();
                            chapterQuestion.setChapterId(chapterId);
                            chapterQuestion.setQuestionId(questionBank.getQuestionId());
                            chapterQuestion.setQuestionType(TEST_QUESTION_TYPE); // "test"
                            chapterQuestion.setCreatedAt(LocalDateTime.now());

                            chapterQuestions.add(chapterQuestion);
                        }

                        savedCount++;

                    } catch (Exception e) {
                        log.error("保存单个测试题失败: {}", questionNode.toString(), e);
                    }
                }

                // 3. 批量保存章节题目关系 - 使用您现有的方法
                if (!chapterQuestions.isEmpty()) {
                    try {
                        int batchResult = chapterQuestionMapper.batchInsert(chapterQuestions);
                        log.info("批量保存章节题目关系成功，影响行数：{}", batchResult);
                    } catch (Exception e) {
                        log.error("批量保存章节题目关系失败", e);
                        // 如果批量插入失败，尝试单个插入
                        for (ChapterQuestion cq : chapterQuestions) {
                            try {
                                chapterQuestionMapper.insert(cq);
                            } catch (Exception ex) {
                                log.error("单个插入章节题目关系失败：{}", cq.toString(), ex);
                            }
                        }
                    }
                }

                log.info("测试题保存完成，共保存 {} 道题目", savedCount);
            }
        } catch (Exception e) {
            log.error("保存测试题到数据库失败", e);
            throw new RuntimeException("测试题保存失败: " + e.getMessage(), e);
        }
    }

    /**
     * 从JSON构建QuestionBank对象
     */
    private QuestionBank buildQuestionBankFromJson(JsonNode questionNode, Integer courseId, Integer teacherId)
            throws Exception {
        QuestionBank questionBank = new QuestionBank();

        questionBank.setCourseId(courseId);
        questionBank.setCreatedBy(teacherId);
        questionBank.setPointId(getIntValue(questionNode, "point_id"));
        questionBank.setQuestionContent(getStringValue(questionNode, "question_content"));
        questionBank.setExplanation(getStringValue(questionNode, "explanation"));

        questionBank.setQuestionType(QuestionBank.QuestionType.fromValue(
                getStringValue(questionNode, "question_type")));
        questionBank.setDifficultyLevel(QuestionBank.DifficultyLevel.fromValue(
                getStringValue(questionNode, "difficulty_level", DEFAULT_DIFFICULTY)));

        String scoreStr = getStringValue(questionNode, "score_points", DEFAULT_SCORE.toString());
        questionBank.setScorePoints(new BigDecimal(scoreStr));

        // 处理选项
        JsonNode optionsNode = questionNode.get("question_options");
        if (optionsNode != null && !optionsNode.isNull()) {
            questionBank.setQuestionOptions(objectMapper.writeValueAsString(optionsNode));
        }

        // 处理答案
        JsonNode answerNode = questionNode.get("correct_answer");
        if (answerNode != null && !answerNode.isNull()) {
            String answerJson = answerNode.isTextual() ?
                    objectMapper.writeValueAsString(answerNode.asText()) :
                    objectMapper.writeValueAsString(answerNode);
            questionBank.setCorrectAnswer(answerJson);
        }

        questionBank.setCreatedAt(LocalDateTime.now());
        questionBank.setUpdatedAt(LocalDateTime.now());

        return questionBank;
    }

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
    private String getStringValue(JsonNode node, String fieldName) {
        return getStringValue(node, fieldName, null);
    }

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
}