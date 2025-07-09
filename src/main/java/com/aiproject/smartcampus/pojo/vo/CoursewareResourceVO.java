package com.aiproject.smartcampus.pojo.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 课件资源VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoursewareResourceVO {
    
    private Integer resourceId;
    private String fileName;
    private String fileUrl;
    private BigDecimal fileSizeMb;
    private String fileType;
    private Integer downloadCount;
    private Integer isPublic;
    private Integer isDownloadable;
    
    /**
     * 获取文件大小描述
     */
    public String getFileSizeDesc() {
        if (fileSizeMb == null) {
            return "未知大小";
        }
        return fileSizeMb.toString() + " MB";
    }
    
    /**
     * 判断是否允许下载
     */
    public boolean isDownloadAllowed() {
        return isDownloadable != null && isDownloadable == 1;
    }
    
    /**
     * 判断是否公开
     */
    public boolean isPublicResource() {
        return isPublic != null && isPublic == 1;
    }
    
    /**
     * 获取文件扩展名
     */
    public String getFileExtension() {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
    }
}