package com.aiproject.smartcampus.pojo.bo;

import lombok.Data;

@Data
public class KnowledgePointErrorSummary {
    private Integer pointId;
    private String pointName;
    private String description;
    private String difficultyLevel;
    private String keywords;
    private String courseName;
    private Integer totalWrongCount; // 总错误次数
    private Integer totalAnswerCount; // 总答题次数
    private Double averageErrorRate; // 平均错误率百分比

    public KnowledgePointErrorSummary(Integer pointId, String pointName, String description,
                                      String difficultyLevel, String keywords, String courseName) {
        this.pointId = pointId;
        this.pointName = pointName;
        this.description = description;
        this.difficultyLevel = difficultyLevel;
        this.keywords = keywords;
        this.courseName = courseName;
        this.totalWrongCount = 0;
        this.totalAnswerCount = 0;
        this.averageErrorRate = 0.0; // 初始化 averageErrorRate
    }

    public void incrementWrongAnswerCount(int count) {
        this.totalWrongCount += count;
    }

    public void incrementTotalAnswerCount(int count) {
        this.totalAnswerCount += count;
    }
}