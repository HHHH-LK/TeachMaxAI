package com.aiproject.smartcampus.pojo.dto;
import lombok.Data;

//考核基本信息
@Data
public class ExamDTO {
    //考核id
    private Integer exam_id;
    //考核名称
    private String title;
    //考核类型
    private String type;
    //考核难度
    private String difficulty;
    //题目数量
    private Integer totalQuestions;
    //总分值
    private Integer totalScore;
    //题型
    private String questionType;
    //特殊要求
    private String specialRequirements;

}
