package com.aiproject.smartcampus.pojo.dto;


import lombok.Data;

import java.util.List;

//考试题目
@Data
public class ExamQuestionDTO {
    private Long examId;         // 考试ID
    private Long questionId;      // 题目ID
    private String questionType;  // 题目类型
    private String content;       // 题目内容
    private Integer score;        // 题目分值
    private Integer sortOrder;    // 题目排序
    private List<String> options; // 选择题选项
    private String answer;        // 正确答案
}