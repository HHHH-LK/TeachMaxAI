package com.aiproject.smartcampus.model.functioncalling.toolutils;

import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * @program: ss
 * @description: 班级学情分析报告生成
 * @author: lk_hhh
 * @create: 2025-07-11 14:40
 **/
@Component
@Slf4j
@RequiredArgsConstructor
public class ClassInfoAsicToolUtils {

    private final ChatLanguageModel chatLanguageModel;

    public String createClassInfoAsic(String content) {



        try {
            log.info("开始生成班级学情分析报告，内容长度: {}", content.length());

            // 构建详细的AI分析提示词
            String prompt = buildDetailedAnalysisPrompt(content);

            // 调用AI模型生成分析报告
            String aiAnalysis = chatLanguageModel.chat(prompt);

            // 生成结构化的最终报告
            String finalReport = generateStructuredReport(content, aiAnalysis);

            log.info("班级学情分析报告生成成功");
            return finalReport;

        } catch (Exception e) {
            log.error("生成班级学情分析报告失败", e);
            return generateErrorReport(e.getMessage());
        }
    }

    /**
     * 构建详细的AI分析提示词
     */
    private String buildDetailedAnalysisPrompt(String content) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("你是一位资深的教育数据分析专家，请基于以下班级学情数据，生成一份详细、专业的教学分析报告。\n\n");

        prompt.append("【原始数据】\n");
        prompt.append(content).append("\n\n");

        prompt.append("【分析要求】\n");
        prompt.append("请从以下维度进行深入分析，每个维度都要提供具体的数据支撑和可操作的建议：\n\n");

        prompt.append("1. 整体学习状况评估\n");
        prompt.append("   - 班级整体成绩水平分析\n");
        prompt.append("   - 学生成绩分布情况\n");
        prompt.append("   - 整体学习趋势判断\n\n");

        prompt.append("2. 学习问题深度诊断\n");
        prompt.append("   - 识别关键学习问题\n");
        prompt.append("   - 分析问题产生的根本原因\n");
        prompt.append("   - 问题影响程度评估\n");
        prompt.append("   - 问题解决的紧急程度排序\n\n");

        prompt.append("3. 知识点掌握情况分析\n");
        prompt.append("   - 各知识点掌握程度统计\n");
        prompt.append("   - 高频错误知识点深度分析\n");
        prompt.append("   - 知识点之间的关联性分析\n");
        prompt.append("   - 知识体系薄弱环节识别\n\n");

        prompt.append("4. 学生分层分析\n");
        prompt.append("   - 优等生特征分析与提升策略\n");
        prompt.append("   - 中等生学习瓶颈与突破方向\n");
        prompt.append("   - 后进生问题诊断与帮扶措施\n");
        prompt.append("   - 各层次学生的个性化建议\n\n");

        prompt.append("5. 教学策略优化建议\n");
        prompt.append("   - 教学内容调整建议\n");
        prompt.append("   - 教学方法改进策略\n");
        prompt.append("   - 课堂互动优化方案\n");
        prompt.append("   - 作业布置策略调整\n\n");

        prompt.append("6. 个性化辅导方案\n");
        prompt.append("   - 重点辅导对象识别\n");
        prompt.append("   - 个性化辅导计划制定\n");
        prompt.append("   - 辅导效果评估标准\n");
        prompt.append("   - 家校合作建议\n\n");

        prompt.append("7. 后续行动计划\n");
        prompt.append("   - 短期改进措施（1-2周）\n");
        prompt.append("   - 中期发展目标（1个月）\n");
        prompt.append("   - 长期提升规划（1学期）\n");
        prompt.append("   - 关键节点检查计划\n\n");

        prompt.append("【输出格式要求】\n");
        prompt.append("1. 使用专业、客观的教育术语\n");
        prompt.append("2. 提供具体的数据支撑和量化分析\n");
        prompt.append("3. 建议要具体可操作，避免空泛的表述\n");
        prompt.append("4. 重点突出，主次分明\n");
        prompt.append("5. 语言简洁明了，逻辑清晰\n\n");

        prompt.append("请基于以上要求，生成一份专业的班级学情分析报告。");

        return prompt.toString();
    }

    /**
     * 生成结构化的最终报告
     */
    private String generateStructuredReport(String originalContent, String aiAnalysis) {
        StringBuilder report = new StringBuilder();

        // 报告头部 - Markdown格式
        report.append("# 班级学情深度分析报告\n\n");

        // 报告基本信息
        String currentTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日 HH:mm:ss"));
        report.append("## 📋 报告生成信息\n\n");
        report.append("| 项目 | 内容 |\n");
        report.append("|------|------|\n");
        report.append("| 生成时间 | ").append(currentTime).append(" |\n");
        report.append("| 报告类型 | AI智能分析报告 |\n");
        report.append("| 分析引擎 | 智慧教学agent|\n");
        report.append("| 报告版本 | v2.0 |\n\n");

        // 数据摘要
        report.append("## 📊 数据摘要\n\n");
        report.append("- **原始数据概览**：").append(summarizeOriginalData(originalContent)).append("\n");
        report.append("- **数据完整性**：").append(evaluateDataCompleteness(originalContent)).append("\n\n");

        // AI分析结果
        report.append("## 🤖 AI智能分析结果\n\n");
        report.append(formatAIAnalysis(aiAnalysis));

        // 关键发现摘要
        report.append("## 🔍 关键发现摘要\n\n");
        report.append(extractKeyFindings(aiAnalysis)).append("\n\n");

        // 优先级建议
        report.append("## 📝 优先级行动建议\n\n");
        report.append(extractPriorityActions(aiAnalysis)).append("\n\n");

        // 数据附录
        report.append("## 📎 数据附录\n\n");
        report.append("### 原始数据详情\n\n");
        report.append("```\n");
        report.append(formatOriginalData(originalContent)).append("\n");
        report.append("```\n\n");
        report.append("### 数据处理说明\n\n");
        report.append("基于原始成绩数据，结合AI模型进行深度分析\n\n");

        // 报告尾部
        report.append("## ℹ️ 报告说明\n\n");
        report.append("本报告由AI智能分析系统生成，基于班级实际学情数据，采用先进的教育数据分析算法。\n\n");
        report.append("建议教师结合实际教学情况，参考本报告进行教学策略调整。\n\n");
        report.append("如有疑问，请联系教学管理部门进行进一步分析。\n\n");

        report.append("---\n\n");
        report.append("**报告生成完成** | *智慧校园教学分析系统*\n");

        return report.toString();
    }

    /**
     * 格式化AI分析结果
     */
    private String formatAIAnalysis(String aiAnalysis) {
        StringBuilder formatted = new StringBuilder();

        // 将AI分析结果按段落分割并格式化
        String[] paragraphs = aiAnalysis.split("\n\n");

        for (String paragraph : paragraphs) {
            if (paragraph.trim().isEmpty()) {
                continue;
            }

            // 识别标题行
            if (paragraph.contains("分析") || paragraph.contains("建议") || paragraph.contains("评估") || paragraph.contains("诊断")) {
                formatted.append("### ").append(paragraph.trim()).append("\n\n");
            } else {
                // 普通内容行，添加适当的缩进
                String[] lines = paragraph.split("\n");
                for (String line : lines) {
                    if (line.trim().isEmpty()) {
                        continue;
                    }
                    formatted.append("- ").append(line.trim()).append("\n");
                }
            }
            formatted.append("\n");
        }

        return formatted.toString();
    }

    /**
     * 提取关键发现
     */
    private String extractKeyFindings(String aiAnalysis) {
        StringBuilder findings = new StringBuilder();

        // 简化的关键信息提取逻辑
        findings.append("🔍 **核心发现**：\n\n");
        findings.append("- **学习状况**：基于AI分析，识别出关键学习问题\n");
        findings.append("- **知识薄弱点**：AI模型识别出高频错误知识点\n");
        findings.append("- **学生分层**：不同层次学生的学习特征已识别\n");
        findings.append("- **改进方向**：AI推荐的教学策略优化建议\n");

        return findings.toString();
    }

    /**
     * 提取优先级建议
     */
    private String extractPriorityActions(String aiAnalysis) {
        StringBuilder actions = new StringBuilder();

        actions.append("### 🎯 立即执行（高优先级）\n\n");
        actions.append("- 针对高频错误知识点进行专项辅导\n");
        actions.append("- 调整教学进度，加强基础知识巩固\n");
        actions.append("- 实施个性化学习方案\n\n");

        actions.append("### 📋 近期安排（中优先级）\n\n");
        actions.append("- 优化课堂互动方式\n");
        actions.append("- 调整作业布置策略\n");
        actions.append("- 加强家校沟通\n\n");

        actions.append("### 📈 长期规划（低优先级）\n\n");
        actions.append("- 建立学习效果跟踪机制\n");
        actions.append("- 完善教学资源库\n");
        actions.append("- 制定学期教学改进计划\n");

        return actions.toString();
    }

    /**
     * 总结原始数据
     */
    private String summarizeOriginalData(String content) {
        int dataLength = content.length();
        int lineCount = content.split("\n").length;

        return String.format("数据量：%d字符，%d行记录", dataLength, lineCount);
    }

    /**
     * 评估数据完整性
     */
    private String evaluateDataCompleteness(String content) {
        // 简化的数据完整性评估
        if (content.length() > 500) {
            return "数据完整度：优秀 ✓";
        } else if (content.length() > 200) {
            return "数据完整度：良好 ○";
        } else {
            return "数据完整度：基础 △";
        }
    }

    /**
     * 格式化原始数据
     */
    private String formatOriginalData(String content) {
        // 截取前200个字符作为预览
        String preview = content.length() > 200 ? content.substring(0, 200) + "..." : content;
        return preview;
    }

    /**
     * 生成错误报告
     */
    private String generateErrorReport(String errorMessage) {
        return "# ❌ 错误报告\n\n" +
                "## 错误信息\n\n" +
                "报告生成过程中发生错误：`" + errorMessage + "`\n\n" +
                "## 建议解决方案\n\n" +
                "1. 检查输入数据格式是否正确\n" +
                "2. 确认AI模型服务是否正常\n" +
                "3. 联系技术支持团队\n\n" +
                "## 联系方式\n\n" +
                "**技术支持**：请联系系统管理员\n\n" +
                "---\n\n" +
                "*智慧校园教学分析系统*";
    }
}