package com.aiproject.smartcampus.service.impl;

import com.aiproject.smartcampus.commons.client.Result;
import com.aiproject.smartcampus.model.functioncalling.toolutils.ExamCreaterToolUtils;
import com.aiproject.smartcampus.model.functioncalling.toolutils.ExamMarkingToolUtils;
import com.aiproject.smartcampus.pojo.vo.ExamCreationResult;
import com.aiproject.smartcampus.service.TeacherAIservice;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * @program: ss
 * @description:
 * @author: lk_hhh
 * @create: 2025-07-11 04:23
 **/

@Service
@RequiredArgsConstructor
@Slf4j
public class TeacherAIserviceImpl implements TeacherAIservice {

    private final ExamMarkingToolUtils examMarkingToolUtils;
    private final ExamCreaterToolUtils examCreaterToolUtils;

    @Override
    public Result aiMarkingExam(String studentId, String examId) {

        String mark = examMarkingToolUtils.mark(studentId, examId);

        return Result.success(mark);

    }

    /**
     * 智能创建试卷
     * @param content 试卷要求描述
     * @param courseId 课程ID
     * @return 创建结果
     */
    @Override
    public ExamCreationResult createIntelligentExam(String content, String courseId) {
        try {
            log.info("开始创建智能试卷: courseId={}, content={}", courseId, content);

            // 调用工具类创建试卷
            ExamCreationResult result = examCreaterToolUtils.createExam(content, courseId);

            if (result.getSuccess()) {
                log.info("智能试卷创建成功: examId={}", result.getExamId());

                // 这里可以添加后续处理逻辑
                // 例如：发送通知、记录日志、缓存结果等

                return result;
            } else {
                log.error("智能试卷创建失败: {}", result.getErrorMessage());
                return result;
            }

        } catch (Exception e) {
            log.error("创建智能试卷异常: courseId={}", courseId, e);
            return ExamCreationResult.failure("系统异常：" + e.getMessage());
        }
    }

    /**
     * 获取试卷JSON内容
     * @param content 试卷要求
     * @param courseId 课程ID
     * @return 试卷JSON字符串
     */
    public String getExamPaperJson(String content, String courseId) {
        ExamCreationResult result = createIntelligentExam(content, courseId);

        if (result.getSuccess()) {
            return result.getExamPaperJson();
        } else {
            log.error("获取试卷JSON失败: {}", result.getErrorMessage());
            return "{}";
        }
    }

    /**
     * 获取创建报告
     * @param content 试卷要求
     * @param courseId 课程ID
     * @return 创建报告文本
     */
    public String getCreationReport(String content, String courseId) {
        ExamCreationResult result = createIntelligentExam(content, courseId);

        if (result.getSuccess()) {
            return result.getCreationReport();
        } else {
            return "创建失败：" + result.getErrorMessage();
        }
    }
}