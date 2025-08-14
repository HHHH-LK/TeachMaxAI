package com.aiproject.smartcampus.model.functioncalling.toolutils;

import com.aiproject.smartcampus.pojo.po.Teacher;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 教师分配工具类 - 用于构建AI提示词和解析响应
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CreateTeacherAssignToolUtils {

    private final ChatLanguageModel chatLanguageModel;

    /**
     * 从课程名称中提取学科领域
     */
    public String extractSubjectArea(String courseName) {
        try {
            String prompt = buildSubjectAreaPrompt(courseName);
            log.info("学科领域提取提示词: {}", prompt);

            ChatResponse response = chatLanguageModel.chat(UserMessage.from(prompt));
            String content = response.aiMessage().text();

            // 清理响应内容
            String subjectArea = cleanSubjectAreaResponse(content);
            log.info("提取的学科领域: {}", subjectArea);

            return subjectArea;

        } catch (Exception e) {
            log.error("学科领域提取失败", e);
            return null;
        }
    }

    /**
     * 构建学科领域提取提示词
     */
    private String buildSubjectAreaPrompt(String courseName) {
        return "你是一个学科领域提取助手。请根据课程名称提取出最相关的学科领域（如数学、物理、化学、生物、历史、地理、语文、英语、计算机、艺术、体育等）。\n\n" +
                "课程名称: " + courseName + "\n\n" +
                "请直接返回学科领域名称，不要包含任何其他文本。";
    }

    /**
     * 清理学科领域响应
     */
    private String cleanSubjectAreaResponse(String content) {
        // 移除可能的引号和额外文本
        return content.replaceAll("\"", "").trim();
    }

    /**
     * 为课程推荐最适合的教师
     */
    public Teacher recommendBestTeacher(String courseName, List<Teacher> teachers) {
        try {
            String prompt = buildRecommendTeacherPrompt(courseName, teachers);
            log.info("教师推荐提示词: {}", prompt);

            ChatResponse response = chatLanguageModel.chat(UserMessage.from(prompt));
            String content = response.aiMessage().text();

            // 解析推荐的教师ID
            int teacherId = parseRecommendedTeacherId(content);
            log.info("推荐的教师ID: {}", teacherId);

            // 查找匹配的教师
            return findTeacherById(teachers, teacherId);

        } catch (Exception e) {
            log.error("教师推荐失败", e);
            return null;
        }
    }

    /**
     * 构建教师推荐提示词
     */
    private String buildRecommendTeacherPrompt(String courseName, List<Teacher> teachers) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是一个教师推荐助手。请根据课程名称和教师列表，推荐最适合教授该课程的教师。\n\n");
        prompt.append("## 课程信息\n");
        prompt.append("- 课程名称: ").append(courseName).append("\n\n");

        prompt.append("## 教师列表\n");
        for (int i = 0; i < teachers.size(); i++) {
            Teacher teacher = teachers.get(i);
            prompt.append(i + 1).append(". ")
                    .append("教师ID: ").append(teacher.getTeacherId()).append(", ")
                    .append("部门: ").append(teacher.getDepartment()).append(", ")
                    .append("工号: ").append(teacher.getEmployeeNumber()).append("\n");
        }

        prompt.append("\n## 要求\n");
        prompt.append("1. 请根据课程内容和教师专业领域推荐最适合的教师\n");
        prompt.append("2. 直接返回教师ID，不要包含任何其他文本\n");
        prompt.append("3. 如果无法确定，返回第一个教师的ID\n\n");
        prompt.append("现在请推荐最适合的教师ID：");

        return prompt.toString();
    }

    /**
     * 解析推荐的教师ID
     */
    private int parseRecommendedTeacherId(String content) {
        try {
            // 清理响应内容
            String cleaned = content.replaceAll("[^0-9]", "").trim();
            if (!cleaned.isEmpty()) {
                return Integer.parseInt(cleaned);
            }
        } catch (Exception e) {
            log.warn("解析教师ID失败: {}", content, e);
        }
        return 0;
    }

    /**
     * 根据ID查找教师
     */
    private Teacher findTeacherById(List<Teacher> teachers, int teacherId) {
        if (teacherId <= 0) {
            return null;
        }

        for (Teacher teacher : teachers) {
            if (teacher.getTeacherId() == teacherId) {
                return teacher;
            }
        }
        return null;
    }
}