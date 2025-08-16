package com.aiproject.smartcampus.pojo.dto;

import lombok.Data;

@Data
public class CreateMaterialDTO {
    private String courseId; // 课程ID
    private String chapterId; // 章节ID（可选，用于指定资料所属章节）
    private String resourceUrl; // 资源链接或存储路径（如云存储地址）
    private String materialTitle; // 资料名称
    private String materialDescription; // 资料描述
    private Integer estimatedTime; // 预计学习时长（分钟）
    private Integer userId; // 创建者用户ID
    private String difficultyLevel; // 难度等级（如：easy, medium, hard）
}
