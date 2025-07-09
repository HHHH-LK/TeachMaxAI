package com.aiproject.smartcampus.pojo.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 外部资源VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExternalResourceVO {
    
    private String resourceUrl;
    private String resourceType;
    
    /**
     * 获取资源类型中文描述
     */
    public String getResourceTypeCn() {
        if (resourceType == null) {
            return "外部链接";
        }
        switch (resourceType) {
            case "youtube": return "YouTube视频";
            case "bilibili": return "B站视频";
            case "pdf": return "PDF文档";
            case "github": return "GitHub项目";
            case "search": return "搜索引擎";
            case "website": return "网站";
            default: return "外部链接";
        }
    }
    
    /**
     * 判断是否为视频资源
     */
    public boolean isVideoResource() {
        return "youtube".equals(resourceType) || "bilibili".equals(resourceType);
    }
    
    /**
     * 判断是否为文档资源
     */
    public boolean isDocumentResource() {
        return "pdf".equals(resourceType);
    }
}