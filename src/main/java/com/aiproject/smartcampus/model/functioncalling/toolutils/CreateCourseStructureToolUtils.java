package com.aiproject.smartcampus.model.functioncalling.toolutils;

import com.aiproject.smartcampus.pojo.po.Course;
import com.aiproject.smartcampus.pojo.po.KnowledgePoint;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class CreateCourseStructureToolUtils {

    private final ChatLanguageModel chatLanguageModel;
    private final ObjectMapper objectMapper;

    // JSON提取的正则表达式
    private static final Pattern JSON_PATTERN = Pattern.compile("```(?:json)?\\s*([\\s\\S]*?)\\s*```|\\{[\\s\\S]*\\}", Pattern.CASE_INSENSITIVE);

    /**
     * 创建课程结构
     */
    public String createCourseStructure(Course course) {
        try {
            // 构建AI提示词
            String prompt = buildAIPrompt(course);
            log.info("AI提示词: {}", prompt);

            // 调用AI模型生成课程结构
            return generateCourseStructure(prompt);

        } catch (Exception e) {
            log.error("课程结构生成失败", e);
            throw new RuntimeException("课程结构生成失败: " + e.getMessage(), e);
        }
    }

    /**
     * 构建AI提示词
     */
    private String buildAIPrompt(Course course) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是一个专业的课程设计助手。请根据以下要求创建课程结构：\n\n");
        prompt.append("## 🧠 课程基本信息\n");
        prompt.append("- 课程名称: ").append(course.getCourseName()).append("\n");

        if (course.getSemester() != null) {
            prompt.append("- 学期: ").append(course.getSemester()).append("\n");
        }

        if (course.getStatus() != null) {
            prompt.append("- 课程状态: ").append(course.getStatus().getDescription()).append("\n");
        }

        prompt.append("## 🎯 课程结构设计任务\n");
        prompt.append("请根据指定课程，自动生成结构合理、内容完整合理的课程章节与知识点设计方案。\n");
        prompt.append("要求如下：\n");
        prompt.append("1. 生成一个完整的课程章节结构，章节数量应合理（不做固定限制），覆盖课程核心内容。\n");
        prompt.append("2. 每个章节应包含若干核心知识点（不限制数量），确保知识点覆盖面广，重点突出。\n");
        prompt.append("3. 每个章节的知识点需紧密围绕章节主题，具有明确的逻辑关联，体现由浅入深的递进关系。\n");
        prompt.append("4. 所有知识点命名应准确、简洁，使用规范的专业术语。\n");
        prompt.append("5. 为每个知识点智能判断难度等级（仅限：easy, medium, hard），依据其理论深度与实际掌握难度判断。\n");
        prompt.append("6. 为每个知识点生成3-5个关键词（keywords），用逗号分隔，体现核心概念。\n");
        prompt.append("7. 每个章节应提供1-2句话的简要描述，概括该章节的教学目标与主要内容。\n");
        prompt.append("8. 章节顺序需合理编号，体现课程整体学习路径。\n\n");

        prompt.append("## 📝 输出格式要求\n");
        prompt.append("直接返回纯JSON格式，不要包含任何额外文本，格式如下：\n\n");
        prompt.append("{\n");
        prompt.append("  \"courseName\": \"课程名称\",\n");
        prompt.append("  \"chapters\": [\n");
        prompt.append("    {\n");
        prompt.append("      \"chapterName\": \"章节1名称\",\n");
        prompt.append("      \"chapterDescription\": \"章节简要描述\",\n");
        prompt.append("      \"chapterOrder\": 1,\n");
        prompt.append("      \"knowledgePoints\": [\n");
        prompt.append("        {\"pointName\": \"知识点1.1名称\", \"description\": \"简要说明\", \"difficulty_level\": \"medium\", \"keywords\": \"关键词1,关键词2,关键词3\"},\n");
        prompt.append("        {\"pointName\": \"知识点1.2名称\", \"description\": \"简要说明\", \"difficulty_level\": \"easy\", \"keywords\": \"关键词4,关键词5\"}\n");
        prompt.append("      ]\n");
        prompt.append("    },\n");
        prompt.append("    {\n");
        prompt.append("      \"chapterName\": \"章节2名称\",\n");
        prompt.append("      \"chapterDescription\": \"章节简要描述\",\n");
        prompt.append("      \"chapterOrder\": 2,\n");
        prompt.append("      \"knowledgePoints\": [\n");
        prompt.append("        {\"pointName\": \"知识点2.1名称\", \"description\": \"简要说明\", \"difficulty_level\": \"hard\", \"keywords\": \"关键词6,关键词7,关键词8\"}\n");
        prompt.append("      ]\n");
        prompt.append("    }\n");
        prompt.append("  ]\n");
        prompt.append("}\n\n");
        prompt.append("请注意：\n");
        prompt.append("- chapterOrder 从1开始递增\n");
        prompt.append("- difficulty_level 必须是 easy, medium 或 hard\n");
        prompt.append("- keywords 应为3-5个核心概念关键词，用逗号分隔\n");
        prompt.append("- 章节描述应简洁明了，概括章节主要内容\n");
        prompt.append("- 知识点难度应基于其复杂度和深度智能判断\n");
        prompt.append("现在请生成课程结构：");

        return prompt.toString();
    }

    /**
     * 调用AI生成课程结构
     */
    private String generateCourseStructure(String prompt) {
        try {
            ChatResponse response = chatLanguageModel.chat(UserMessage.from(prompt));
            String content = response.aiMessage().text();

            // 清理可能的额外标记
            return cleanJsonResponse(content);
        } catch (Exception e) {
            log.error("AI课程结构生成失败", e);
            throw new RuntimeException("AI服务暂时不可用: " + e.getMessage());
        }
    }

    /**
     * 清理JSON响应
     */
    private String cleanJsonResponse(String content) {
        // 移除可能的代码块标记
        String cleaned = content.replaceAll("```json|```", "").trim();

        // 验证是否为有效JSON
        try {
            objectMapper.readTree(cleaned);
            return cleaned;
        } catch (Exception e) {
            log.warn("AI返回内容需要二次清理", e);

            // 尝试提取JSON部分
            int startIndex = cleaned.indexOf('{');
            int endIndex = cleaned.lastIndexOf('}');

            if (startIndex >= 0 && endIndex > startIndex) {
                return cleaned.substring(startIndex, endIndex + 1);
            }

            throw new IllegalArgumentException("AI返回无效的JSON格式: " + cleaned);
        }
    }

    /**
     * 解析AI返回的课程结构
     */
    public CourseStructure parseAIResponse(String aiResponse) throws Exception {
        JsonNode rootNode = objectMapper.readTree(aiResponse);

        CourseStructure structure = new CourseStructure();
        structure.setCourseName(rootNode.path("courseName").asText());

        List<ChapterStructure> chapters = new ArrayList<>();
        JsonNode chaptersNode = rootNode.path("chapters");

        for (JsonNode chapterNode : chaptersNode) {
            ChapterStructure chapter = new ChapterStructure();
            chapter.setChapterName(chapterNode.path("chapterName").asText());
            chapter.setChapterDescription(chapterNode.path("chapterDescription").asText());
            chapter.setChapterOrder(chapterNode.path("chapterOrder").asInt());

            List<KnowledgePointStructure> points = new ArrayList<>();
            JsonNode pointsNode = chapterNode.path("knowledgePoints");

            for (JsonNode pointNode : pointsNode) {
                KnowledgePointStructure point = new KnowledgePointStructure();
                point.setPointName(pointNode.path("pointName").asText());
                point.setDescription(pointNode.path("description").asText());

                // 获取难度等级，如果不存在则默认为中等
                String difficulty = pointNode.path("difficulty_level").asText("medium");
                point.setDifficultyLevel(KnowledgePoint.DifficultyLevel.fromValue(difficulty));

                // 获取关键词，如果不存在则默认为空
                String keywords = pointNode.path("keywords").asText("");
                point.setKeywords(keywords);

                points.add(point);
            }

            chapter.setKnowledgePoints(points);
            chapters.add(chapter);
        }

        structure.setChapters(chapters);
        return structure;
    }

    // 内部数据结构
    @Data
    public static class CourseStructure {
        private String courseName;
        private List<ChapterStructure> chapters;
    }

    @Data
    public static class ChapterStructure {
        private String chapterName;
        private String chapterDescription;
        private int chapterOrder;
        private List<KnowledgePointStructure> knowledgePoints;
    }

    @Data
    public static class KnowledgePointStructure {
        private String pointName;
        private String description;
        private KnowledgePoint.DifficultyLevel difficultyLevel;
        private String keywords;
    }
}