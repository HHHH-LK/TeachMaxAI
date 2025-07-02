package com.aiproject.smartcampus.pojo.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @program: ss
 * @description: 返回类
 * @author: lk_hhh
 * @create: 2025-07-03 00:22
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor

public class StudentWrongKnowledgeVO {

    @ApiModelProperty(value = "知识点ID", example = "1")
    @JsonProperty("pointId")
    private Integer pointId;

    @ApiModelProperty(value = "知识点名称", example = "二次函数")
    @JsonProperty("pointName")
    private String pointName;

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
                ", wrongAnswerCount=" + wrongAnswerCount +
                ", accuracyRate=" + accuracyRate +
                '}';
    }

}