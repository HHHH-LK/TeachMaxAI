package com.aiproject.smartcampus.model.functioncalling;

import com.aiproject.smartcampus.model.functioncalling.toolutils.NotMasterTestCreatetoolUtils;
import com.aiproject.smartcampus.pojo.bo.SimpleKnowledgeAnalysisBO;
import com.aiproject.smartcampus.pojo.bo.TestTaskBO;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @program: SmartCampus
 * @description: 未掌握知识点测试生成 - 基于知识点分析数据生成
 * @author: lk_hhh
 * @create: 2025-07-01 10:26
 * @version: 3.0
 **/

@Data
@Slf4j
@Component
@RequiredArgsConstructor
public class NotMasterTestCreateTool implements Tool {

    private final NotMasterTestCreatetoolUtils notMasterTestCreatetoolUtils;

    // 测试任务基本信息
    private TestTaskBO testTaskBO;

    // 需要生成题目的知识点分析数据集合
    private List<SimpleKnowledgeAnalysisBO> simpleKnowledgeAnalysisBOList;

    // 生成结果
    private String result;

    @Override
    public void run() {
        log.info("正在为学生[{}]创建测试题，基于{}个知识点分析数据",
                testTaskBO != null ? testTaskBO.getStudentId() : "未知",
                simpleKnowledgeAnalysisBOList != null ? simpleKnowledgeAnalysisBOList.size() : 0);

        // 参数校验
        if (!validateParameters()) {
            return;
        }

        try {
            // 调用工具类生成测试题
            String test = notMasterTestCreatetoolUtils.createTest(testTaskBO, simpleKnowledgeAnalysisBOList);

            log.info("测试题生成成功，内容长度: {}", test != null ? test.length() : 0);
            log.debug("生成的测试题内容: {}", test);

            result = test;

        } catch (Exception e) {
            log.error("测试题生成失败", e);
            result = "测试题生成失败: " + e.getMessage();
        }
    }

    /**
     * 参数校验
     */
    private boolean validateParameters() {
        if (testTaskBO == null) {
            log.error("测试任务信息不能为空");
            result = "测试任务信息不能为空";
            return false;
        }

        if (testTaskBO.getStudentId() == null) {
            log.error("学生ID不能为空");
            result = "学生ID不能为空";
            return false;
        }

        if (simpleKnowledgeAnalysisBOList == null || simpleKnowledgeAnalysisBOList.isEmpty()) {
            log.warn("知识点分析数据为空，将生成通用题目");
            // 不直接返回false，允许生成通用题目
        }

        return true;
    }

    /**
     * 获取知识点统计信息（用于日志输出）
     */
    public String getKnowledgePointsSummary() {
        if (simpleKnowledgeAnalysisBOList == null || simpleKnowledgeAnalysisBOList.isEmpty()) {
            return "无知识点数据";
        }

        StringBuilder summary = new StringBuilder();
        summary.append("知识点列表: ");

        for (int i = 0; i < simpleKnowledgeAnalysisBOList.size(); i++) {
            SimpleKnowledgeAnalysisBO analysis = simpleKnowledgeAnalysisBOList.get(i);
            if (i > 0) {
                summary.append(", ");
            }
            summary.append(analysis.getPointName())
                    .append("(正确率:")
                    .append(analysis.getAccuracyRate() != null ? analysis.getAccuracyRate() + "%" : "未知")
                    .append(")");
        }

        return summary.toString();
    }
}