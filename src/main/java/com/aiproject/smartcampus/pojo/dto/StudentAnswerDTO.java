package com.aiproject.smartcampus.pojo.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import com.fasterxml.jackson.annotation.JsonFormat;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.NotBlank;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 学生答题DTO
 * 用于接收前端提交的学生答案数据
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentAnswerDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 题目ID
     */
    @NotNull(message = "题目ID不能为空")
    private Integer questionId;

    /**
     * 学生答案
     * 可以是：
     * - 单选题: "A" 或 "B" 等
     * - 多选题: "A,B,C" 或 JSON格式 ["A","B","C"]
     * - 判断题: "true" 或 "false"
     * - 填空题: "具体答案内容"
     * - 简答题: "详细的文字答案"
     */
    private String studentAnswer;

    /**
     * 验证答案是否为空
     */
    public boolean isAnswerEmpty() {
        return studentAnswer == null || studentAnswer.trim().isEmpty();
    }

    /**
     * 获取格式化的答案（去除首尾空格）
     */
    public String getFormattedAnswer() {
        return studentAnswer.replace(" ", "").replace("\"", "");

    }

    /**
     * 验证是否为有效的答题数据
     */
    public boolean isValid() {
        return questionId != null &&
                !isAnswerEmpty();
    }


}