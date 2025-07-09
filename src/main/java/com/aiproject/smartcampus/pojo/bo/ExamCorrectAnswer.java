package com.aiproject.smartcampus.pojo.bo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @program: SmartCampus
 * @description: 考试正确答案封装类，用于 JSON 反序列化
 * @author: lk
 * @create: 2025-07-07
 **/

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ExamCorrectAnswer {

    @JsonProperty("answer")
    private String answer;

    @JsonProperty("explanation")
    private String explanation;
}
