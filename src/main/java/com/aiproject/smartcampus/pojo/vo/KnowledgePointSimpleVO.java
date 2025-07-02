package com.aiproject.smartcampus.pojo.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 知识点简化信息VO
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@ApiModel(value = "KnowledgePointSimpleVO", description = "知识点简化信息")
public class KnowledgePointSimpleVO {

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

    // ========== 课程信息 ==========
    @ApiModelProperty(value = "课程ID", example = "1")
    @JsonProperty("courseId")
    private Integer courseId;

    @ApiModelProperty(value = "课程名称", example = "高中数学")
    @JsonProperty("courseName")
    private String courseName;

    @ApiModelProperty(value = "学期", example = "2024秋季")
    @JsonProperty("semester")
    private String semester;

    // ========== 学生掌握情况 ==========
    @ApiModelProperty(value = "掌握程度", example = "learning", allowableValues = "not_learned,learning,mastered")
    @JsonProperty("masteryLevel")
    private String masteryLevel;

    @ApiModelProperty(value = "练习得分", example = "75.5")
    @JsonProperty("practiceScore")
    private Double practiceScore;

    @ApiModelProperty(value = "练习次数", example = "5")
    @JsonProperty("practiceCount")
    private Integer practiceCount;

    @ApiModelProperty(value = "掌握情况最后更新时间")
    @JsonProperty("masteryLastUpdated")
    private LocalDateTime masteryLastUpdated;

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

    @ApiModelProperty(value = "正确率", example = "80.0")
    @JsonProperty("accuracyRate")
    private Double accuracyRate;


    @Override
    public String toString() {
        return "KnowledgePointSimpleVO{" +
                "pointId=" + pointId +
                ", pointName='" + pointName + '\'' +
                ", description='" + description + '\'' +
                ", difficultyLevel='" + difficultyLevel + '\'' +
                ", courseName='" + courseName + '\'' +
                ", masteryLevel='" + masteryLevel + '\'' +
                ", practiceScore=" + practiceScore +
                ", totalAnswered=" + totalAnswered +
                ", accuracyRate=" + accuracyRate +
                '}';
    }
}