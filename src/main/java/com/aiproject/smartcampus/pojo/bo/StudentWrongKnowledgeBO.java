package com.aiproject.smartcampus.pojo.bo;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 学生错误知识点信息VO
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@ApiModel(value = "StudentWrongKnowledgeVO", description = "学生错误知识点信息")
public class StudentWrongKnowledgeBO {

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

    @ApiModelProperty(value = "课程名称", example = "高中数学")
    @JsonProperty("courseName")
    private String courseName;

    @ApiModelProperty(value = "答错题目数量", example = "5")
    @JsonProperty("wrongAnswerCount")
    private Integer wrongAnswerCount;

    @ApiModelProperty(value = "正确率", example = "65.50")
    @JsonProperty("accuracyRate")
    private Double accuracyRate;

    @Override
    public String toString() {
        return "StudentWrongKnowledgeVO{" +
                "pointId=" + pointId +
                ", pointName='" + pointName + '\'' +
                ", description='" + description + '\'' +
                ", difficultyLevel='" + difficultyLevel + '\'' +
                ", keywords='" + keywords + '\'' +
                ", courseName='" + courseName + '\'' +
                ", wrongAnswerCount=" + wrongAnswerCount +
                ", accuracyRate=" + accuracyRate +
                '}';
    }
}