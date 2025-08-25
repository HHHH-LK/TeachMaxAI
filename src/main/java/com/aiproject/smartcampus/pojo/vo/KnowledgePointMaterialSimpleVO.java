package com.aiproject.smartcampus.pojo.vo;

import lombok.Data;
import java.util.Set;

@Data
public class KnowledgePointMaterialSimpleVO {
    private Integer materialId;
    private String materialTitle;
    private String materialDescription;
    private String materialType;
    private String materialTypeCn;
    private String externalResourceUrl;


}