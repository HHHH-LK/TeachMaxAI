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

    /**
     * 转换为中文描述（更易读的评介信息）
     */
    public String toChineseString() {
        StringBuilder sb = new StringBuilder();
        sb.append("【知识点分析】\n");
        sb.append("知识点ID：").append(pointId).append("\n");
        sb.append("知识点名称：").append(pointName).append("\n");
        sb.append("知识点描述：").append(description != null ? description : "无").append("\n");
        sb.append("难度等级：").append(
                "easy".equalsIgnoreCase(difficultyLevel) ? "简单" :
                        "medium".equalsIgnoreCase(difficultyLevel) ? "中等" :
                                "hard".equalsIgnoreCase(difficultyLevel) ? "困难" : difficultyLevel
        ).append("\n");
        sb.append("关键词：").append(keywords != null ? keywords : "无").append("\n\n");

        sb.append("【答题统计】\n");
        sb.append("已答题目数：").append(totalAnswered != null ? totalAnswered : 0).append("\n");
        sb.append("答对题目数：").append(correctCount != null ? correctCount : 0).append("\n");
        sb.append("答错题目数：").append(wrongCount != null ? wrongCount : 0).append("\n");
        sb.append("正确率：").append(accuracyRate != null ? String.format("%.2f%%", accuracyRate) : "无数据").append("\n\n");

        // -------- 评介逻辑 --------
        sb.append("【综合评介】\n");
        if (accuracyRate == null) {
            sb.append("暂无数据，无法评估。\n");
        } else if (accuracyRate >= 85) {
            sb.append("表现优秀，对该知识点掌握较好。\n");
        } else if (accuracyRate >= 60) {
            sb.append("掌握情况一般，仍需适当复习和练习。\n");
        } else {
            sb.append("掌握较差，需要重点加强训练。\n");
        }

        return sb.toString();
    }


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