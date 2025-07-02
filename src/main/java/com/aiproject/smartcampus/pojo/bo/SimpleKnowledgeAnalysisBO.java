package com.aiproject.smartcampus.pojo.bo;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 知识点分析VO（基础信息+答题统计）
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@ApiModel(value = "SimpleKnowledgeAnalysisVO", description = "简化版知识点分析信息")
public class SimpleKnowledgeAnalysisBO {

    // ========== 基础信息 ==========
    @ApiModelProperty(value = "知识点ID", example = "1")
    @JsonProperty("pointId")
    private Integer pointId;

    @ApiModelProperty(value = "知识点名称", example = "二次函数")
    @JsonProperty("pointName")
    private String pointName;

    @ApiModelProperty(value = "知识点描述", example = "二次函数的基本性质和图像特征")
    @JsonProperty("description")
    private String description;

    @ApiModelProperty(value = "难度等级", example = "medium", allowableValues = "easy,medium,hard")
    @JsonProperty("difficultyLevel")
    private String difficultyLevel;

    @ApiModelProperty(value = "关键词", example = "二次函数,抛物线,顶点,对称轴")
    @JsonProperty("keywords")
    private String keywords;

    // ========== 答题统计 ==========
    @ApiModelProperty(value = "已答题目数", example = "15")
    @JsonProperty("totalAnswered")
    private Integer totalAnswered;

    @ApiModelProperty(value = "答对题目数", example = "12")
    @JsonProperty("correctCount")
    private Integer correctCount;

    @ApiModelProperty(value = "答错题目数", example = "3")
    @JsonProperty("wrongCount")
    private Integer wrongCount;

    @ApiModelProperty(value = "正确率/通过率", example = "80.0")
    @JsonProperty("accuracyRate")
    private Double accuracyRate;

    @Override
    public String toString() {
        return "SimpleKnowledgeAnalysisVO{" +
                "pointId=" + pointId +
                ", pointName='" + pointName + '\'' +
                ", description='" + description + '\'' +
                ", difficultyLevel='" + difficultyLevel + '\'' +
                ", totalAnswered=" + totalAnswered +
                ", correctCount=" + correctCount +
                ", wrongCount=" + wrongCount +
                ", accuracyRate=" + accuracyRate +
                '}';
    }
}