package com.aiproject.smartcampus.pojo.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class KnowledgePointMasteryVO {
    private String pointName;     // 知识点名称
    private double masteryRate;   // 掌握率（0.0 - 1.0）
    private int errorCount;       // 错误次数
    private int correctCount;     // 正确次数
    private int totalCount;       // 总答题次数
}
