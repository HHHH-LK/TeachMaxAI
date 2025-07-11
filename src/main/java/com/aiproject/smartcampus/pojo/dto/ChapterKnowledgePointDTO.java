package com.aiproject.smartcampus.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * 章节知识点查询结果实体类
 * 用于封装章节重点知识点查询的返回结果
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChapterKnowledgePointDTO {
    
    /**
     * 课程ID
     */
    private Integer courseId;
    
    /**
     * 章节ID
     */
    private Integer chapterId;
    
    /**
     * 章节名称
     */
    private String chapterName;
    
    /**
     * 章节顺序
     */
    private Integer chapterOrder;
    
    /**
     * 章节难度等级 (easy, medium, hard)
     */
    private String chapterDifficulty;
    
    /**
     * 知识点ID
     */
    private Integer pointId;
    
    /**
     * 知识点名称
     */
    private String pointName;
    
    /**
     * 知识点描述
     */
    private String pointDescription;
    
    /**
     * 知识点难度等级 (easy, medium, hard)
     */
    private String pointDifficulty;
    
    /**
     * 知识点关键词
     */
    private String keywords;
    
    /**
     * 知识点在章节中的顺序
     */
    private Integer pointOrder;
    
    /**
     * 是否为核心知识点 (1-是，0-否)
     */
    private Boolean isCore;
    
    /**
     * 知识点重要性描述 (重点/一般)
     */
    private String pointImportance;
    
    /**
     * 章节知识点关联创建时间
     */
    private LocalDateTime relationCreatedAt;
    
    @Override
    public String toString() {
        return "章节知识点信息{" +
                "课程ID=" + courseId +
                ", 章节ID=" + chapterId +
                ", 章节名称='" + chapterName + '\'' +
                ", 章节顺序=" + chapterOrder +
                ", 章节难度='" + chapterDifficulty + '\'' +
                ", 知识点ID=" + pointId +
                ", 知识点名称='" + pointName + '\'' +
                ", 知识点描述='" + pointDescription + '\'' +
                ", 知识点难度='" + pointDifficulty + '\'' +
                ", 关键词='" + keywords + '\'' +
                ", 知识点顺序=" + pointOrder +
                ", 是否核心知识点=" + (isCore ? "是" : "否") +
                ", 重要性='" + pointImportance + '\'' +
                ", 关联创建时间=" + relationCreatedAt +
                '}';
    }
}