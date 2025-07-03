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
    private String question_options; // 选项
    private String correct_answer; // 正确答案
    private String explanation;  // 描述
    private String answer;        // 正确答案
}