package com.aiproject.smartcampus.model.functioncalling;

import com.aiproject.smartcampus.mapper.ChapterQuestionMapper;
import com.aiproject.smartcampus.mapper.QuestionBankMapper;
import com.aiproject.smartcampus.model.functioncalling.toolutils.CreateQuestionByKnowledgeIdtoolUtils;
import com.aiproject.smartcampus.pojo.bo.SimpleKnowledgeAnalysisBO;
import com.aiproject.smartcampus.pojo.bo.TestTaskBO;
import com.aiproject.smartcampus.pojo.po.ChapterQuestion;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
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

    private final CreateQuestionByKnowledgeIdtoolUtils createQuestionByKnowledgeIdtoolUtils;

    private final Integer DEFOURT_AI_CREATE_ID = 0;
    // 测试任务基本信息
    private TestTaskBO testTaskBO;

    private final QuestionBankMapper questionBankMapper;

    private final ChapterQuestionMapper chapterQuestionMapper;

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
            synchronized (this) {
                // 查询全局最大ID，不限制created_by
                Integer beforeMax = questionBankMapper.selectMaxQuestionIdByCreator(DEFOURT_AI_CREATE_ID); // 改为查询全局最大ID

                String test = createQuestionByKnowledgeIdtoolUtils.createTest(testTaskBO, simpleKnowledgeAnalysisBOList);
                result = test;

                // 等待一段时间确保数据保存完成
                Thread.sleep(3000); // 等待3秒

                Integer afterMax = questionBankMapper.selectMaxQuestionIdByCreator(DEFOURT_AI_CREATE_ID);

                log.info("生成前最大ID: {}, 生成后最大ID: {}", beforeMax, afterMax);

                if (afterMax > beforeMax) {
                    addChapterQuestionsBatch(testTaskBO, beforeMax, afterMax);
                } else {
                    Thread.sleep(2000); // 等待2秒
                    afterMax = questionBankMapper.selectMaxQuestionIdByCreator(DEFOURT_AI_CREATE_ID);
                    if (afterMax > beforeMax) {
                        addChapterQuestionsBatch(testTaskBO, beforeMax, afterMax);
                    } else {
                        log.warn("没有检测到新生成的题目，可能保存失败或还在处理中");
                        throw new Exception("没有检测到新生成的题目，可能保存失败或还在处理中");
                    }
                }
            }
        } catch (Exception e) {
            log.error("测试题生成失败", e);
            result = "测试题生成失败: " + e.getMessage();
        }
    }


    /**
     * 批量插入版本 - 性能更好，避免主键冲突
     */
    @Transactional(rollbackFor = Exception.class)
    public void addChapterQuestionsBatch(TestTaskBO testTaskBO, int beforeMax, int afterMax) throws Exception {
        if (afterMax <= beforeMax) {
            log.warn("无效的ID范围: beforeMax={}, afterMax={}", beforeMax, afterMax);
            return;
        }

        List<ChapterQuestion> chapterQuestions = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        // 批量构建数据
        for (int i = beforeMax + 1; i <= afterMax; i++) {
            ChapterQuestion chapterQuestion = new ChapterQuestion();
            chapterQuestion.setQuestionType(ChapterQuestion.QuestionType.PRACTICE);
            chapterQuestion.setChapterId(testTaskBO.getChapterId());
            chapterQuestion.setQuestionId(i);
            chapterQuestion.setCreatedAt(now);
            // 不设置id，让数据库自动生成
            chapterQuestions.add(chapterQuestion);
        }

        // 批量插入
        try {
            int insertCount = chapterQuestionMapper.batchInsert(chapterQuestions);
            if (insertCount != chapterQuestions.size()) {
                log.error("批量插入失败，期望插入{}条，实际插入{}条", chapterQuestions.size(), insertCount);
                throw new Exception("智能生成题目关联异常：批量插入不完整");
            }
            log.info("成功批量添加{}道章节题目", chapterQuestions.size());

        } catch (Exception e) {
            log.error("批量添加章节题目失败", e);
            throw new Exception("智能生成题目关联异常", e);
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