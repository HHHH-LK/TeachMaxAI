package com.aiproject.smartcampus.pojo.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * 章节知识点查询结果实体类
 * 对应查询语句中的字段信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChapterKnowledgePointVO {
    
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
    private String description;
    
    /**
     * 难度等级：easy-简单，medium-中等，hard-困难
     */
    private String difficultyLevel;
    
    /**
     * 关键词，逗号分隔
     */
    private String keywords;
    
    /**
     * 知识点在章节中的顺序
     */
    private Integer pointOrder;
    
    /**
     * 是否为核心知识点：1-是，0-否
     */
    private Integer isCore;
    
    /**
     * 关联关系创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime relationCreatedAt;
    
    /**
     * 获取难度等级中文描述
     * @return 难度等级中文描述
     */
    public String getDifficultyLevelCn() {
        if (difficultyLevel == null) {
            return "";
        }
        switch (difficultyLevel) {
            case "easy":
                return "简单";
            case "medium":
                return "中等";
            case "hard":
                return "困难";
            default:
                return difficultyLevel;
        }
    }
    
    /**
     * 获取是否为核心知识点的描述
     * @return 核心知识点描述
     */
    public String getIsCoreDesc() {
        return isCore != null && isCore == 1 ? "核心知识点" : "普通知识点";
    }
    
    /**
     * 判断是否为核心知识点
     * @return true-是核心知识点，false-不是核心知识点
     */
    public boolean isCorePoint() {
        return isCore != null && isCore == 1;
    }
}