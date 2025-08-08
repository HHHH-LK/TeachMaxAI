package com.aiproject.smartcampus.pojo.dto;

import lombok.Data;

@Data
public class GetKnowledgePointDTO {
    private String courseId;
    private String pointId;
    private String pointName;
    private String description;
    private String difficulty_level; // 难度等级
    private String keywords; // 关键词
}
