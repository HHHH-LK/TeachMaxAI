package com.aiproject.smartcampus.commons.utils.aliyunossutils;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 阿里云OSS配置属性类
 */
@Data
@Component
@ConfigurationProperties(prefix = "aliyun.oss")
public class AliyunOSSProperties {

    /**
     * OSS访问端点
     */
    private String endpoint;

    /**
     * OSS存储桶名称
     */
    private String bucketName;

    /**
     * OSS所在区域
     */
    private String region;

    /**
     * 访问密钥ID
     */
    private String accessKeyId;

    /**
     * 访问密钥Secret
     */
    private String accessKeySecret;
}