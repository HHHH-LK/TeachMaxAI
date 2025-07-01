package com.aiproject.smartcampus.pojo.bo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @program: SmartCampus
 * @description: 知识点封装勒
 * @author: lk_hhh
 * @create: 2025-06-29 18:58
 **/

@Data
@AllArgsConstructor
@NoArgsConstructor
public class KnowledgepointBO {
    private Integer pointId;
    private String description;
    private String knowledgepointName;
}