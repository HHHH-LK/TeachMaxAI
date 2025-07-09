package com.aiproject.smartcampus.pojo.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @program: ss
 * @description: 章节信息统计分类
 * @author: lk_hhh
 * @create: 2025-07-08 17:00
 **/

@Data
@AllArgsConstructor
@NoArgsConstructor
public class KnowledgePointMaterialSimpleSpliderVO {

    private List<KnowledgePointMaterialSimpleVO> externalList;
    private List<KnowledgePointMaterialSimpleVO> courseList;
    private Integer externalTotal;
    private Integer courseTotal;
    private Integer total;


}