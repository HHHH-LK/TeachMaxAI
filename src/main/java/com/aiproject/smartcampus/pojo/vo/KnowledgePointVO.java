package com.aiproject.smartcampus.pojo.vo;

import lombok.Data;

/**
 * 知识点VO（用于智能试卷创建）
 */
@Data
public class KnowledgePointVO {
    private Integer pointId;
    private String pointName;
    private String description;
    private String difficultyLevel;
    private String keywords;
}