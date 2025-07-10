package com.aiproject.smartcampus.pojo.vo;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 试卷创建结果类
 * 包含生成的试卷JSON和创建报告
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamCreationResult {

    /**
     * 创建是否成功
     */

    private Boolean success;

    /**
     * 错误信息（如果失败）
     */
    private String errorMessage;

    /**
     * 考试ID
     */
    private Integer examId;

    /**
     * 生成的试卷内容（JSON格式字符串）
     * 包含所有题目的完整信息
     */
    private String examPaperJson;

    /**
     * 创建报告（文本格式）
     */
    private String creationReport;

    /**
     * 创建成功结果
     */
    public static ExamCreationResult success(Integer examId, String examPaperJson, String creationReport) {
        return ExamCreationResult.builder()
                .success(true)
                .examId(examId)
                .examPaperJson(examPaperJson)
                .creationReport(creationReport)
                .build();
    }

    /**
     * 创建失败结果
     */
    public static ExamCreationResult failure(String errorMessage) {
        return ExamCreationResult.builder()
                .success(false)
                .errorMessage(errorMessage)
                .build();
    }
}