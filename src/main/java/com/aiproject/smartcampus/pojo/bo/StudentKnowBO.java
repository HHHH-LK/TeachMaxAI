package com.aiproject.smartcampus.pojo.bo;

import lombok.Data;

@Data
public class StudentKnowBO {
    private Integer pointId;
    private String pointName;
    private String description;
    private String difficultyLevel;
    private String keywords;
    private String courseName;
    private Integer studentId;
    private Boolean isCorrect; // 新增：是否答对

    // 构造函数确保字段不为null
    public StudentKnowBO() {
        this.isCorrect = false;
    }
}
