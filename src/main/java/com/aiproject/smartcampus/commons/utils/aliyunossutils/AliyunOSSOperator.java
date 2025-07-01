package com.aiproject.smartcampus.commons.utils.aliyunossutils;

import com.aliyun.oss.*;
import com.aliyun.oss.common.auth.CredentialsProvider;
import com.aliyun.oss.common.auth.DefaultCredentialProvider;
import com.aliyun.oss.common.comm.SignVersion;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Slf4j
@Component
public class AliyunOSSOperator {

    @Autowired
    private AliyunOSSProperties aliyunOSSProperties;

    /**
     * 上传文件到阿里云OSS
     * @param content 文件内容字节数组
     * @param originalFilename 原始文件名
     * @return 文件访问URL
     * @throws Exception 上传异常
     */
    public String upload(byte[] content, String originalFilename) throws Exception {
        // 参数校验
        validateUploadParameters(content, originalFilename);

        String endpoint = aliyunOSSProperties.getEndpoint();
        String bucketName = aliyunOSSProperties.getBucketName();
        String region = aliyunOSSProperties.getRegion();
        String accessKeyId = aliyunOSSProperties.getAccessKeyId();
        String accessKeySecret = aliyunOSSProperties.getAccessKeySecret();

        log.info("开始上传文件到OSS: 原始文件名={}, 文件大小={}字节", originalFilename, content.length);

        // 生成对象路径
        String objectName = generateObjectName(originalFilename);
        log.debug("生成的对象路径: {}", objectName);

        // 创建凭证提供者
        CredentialsProvider credentialsProvider = new DefaultCredentialProvider(accessKeyId, accessKeySecret);

        // 创建OSS客户端配置
        ClientBuilderConfiguration clientBuilderConfiguration = new ClientBuilderConfiguration();
        clientBuilderConfiguration.setSignatureVersion(SignVersion.V4);

        // 创建OSS客户端
        OSS ossClient = OSSClientBuilder.create()
                .endpoint(endpoint)
                .credentialsProvider(credentialsProvider)
                .clientConfiguration(clientBuilderConfiguration)
                .region(region)
                .build();

        try {
            // 检查Bucket是否存在
            if (!ossClient.doesBucketExist(bucketName)) {
                log.warn("Bucket {} 不存在，尝试创建", bucketName);
                ossClient.createBucket(bucketName);
                log.info("成功创建Bucket: {}", bucketName);
            }

            // 上传文件
            ossClient.putObject(bucketName, objectName, new ByteArrayInputStream(content));
            log.info("文件上传成功: {}", objectName);

        } catch (Exception e) {
            log.error("文件上传失败: 原始文件名={}, 对象路径={}", originalFilename, objectName, e);
            throw new RuntimeException("文件上传失败: " + e.getMessage(), e);
        } finally {
            // 关闭客户端
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }

        // 生成文件访问URL
        String fileUrl = generateFileUrl(endpoint, bucketName, objectName);
        log.info("文件上传完成，访问URL: {}", fileUrl);

        return fileUrl;
    }

    /**
     * 删除OSS文件
     * @param objectName 对象名称
     * @return 删除结果
     */
    public boolean deleteFile(String objectName) {
        if (!StringUtils.hasText(objectName)) {
            log.warn("删除文件失败：对象名称为空");
            return false;
        }

        String endpoint = aliyunOSSProperties.getEndpoint();
        String bucketName = aliyunOSSProperties.getBucketName();
        String region = aliyunOSSProperties.getRegion();
        String accessKeyId = aliyunOSSProperties.getAccessKeyId();
        String accessKeySecret = aliyunOSSProperties.getAccessKeySecret();

        CredentialsProvider credentialsProvider = new DefaultCredentialProvider(accessKeyId, accessKeySecret);
        ClientBuilderConfiguration clientBuilderConfiguration = new ClientBuilderConfiguration();
        clientBuilderConfiguration.setSignatureVersion(SignVersion.V4);

        OSS ossClient = OSSClientBuilder.create()
                .endpoint(endpoint)
                .credentialsProvider(credentialsProvider)
                .clientConfiguration(clientBuilderConfiguration)
                .region(region)
                .build();

        try {
            if (ossClient.doesObjectExist(bucketName, objectName)) {
                ossClient.deleteObject(bucketName, objectName);
                log.info("成功删除文件: {}", objectName);
                return true;
            } else {
                log.warn("文件不存在: {}", objectName);
                return false;
            }
        } catch (Exception e) {
            log.error("删除文件失败: {}", objectName, e);
            return false;
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }
    }

    /**
     * 验证上传参数
     */
    private void validateUploadParameters(byte[] content, String originalFilename) {
        if (content == null || content.length == 0) {
            throw new IllegalArgumentException("文件内容不能为空");
        }

        if (!StringUtils.hasText(originalFilename)) {
            throw new IllegalArgumentException("原始文件名不能为空");
        }

        // 验证配置是否完整
        if (!StringUtils.hasText(aliyunOSSProperties.getEndpoint())) {
            throw new IllegalStateException("OSS endpoint 配置缺失");
        }

        if (!StringUtils.hasText(aliyunOSSProperties.getBucketName())) {
            throw new IllegalStateException("OSS bucketName 配置缺失");
        }

        if (!StringUtils.hasText(aliyunOSSProperties.getAccessKeyId())) {
            throw new IllegalStateException("OSS accessKeyId 配置缺失");
        }

        if (!StringUtils.hasText(aliyunOSSProperties.getAccessKeySecret())) {
            throw new IllegalStateException("OSS accessKeySecret 配置缺失");
        }
    }

    /**
     * 生成对象名称（文件路径）
     */
    private String generateObjectName(String originalFilename) {
        // 获取文件扩展名
        String extension = "";
        if (originalFilename.lastIndexOf(".") > 0) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        // 生成日期目录：yyyy/MM
        String dir = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM"));

        // 生成唯一文件名
        String newFileName = UUID.randomUUID().toString().replace("-", "") + extension;

        return dir + "/" + newFileName;
    }

    /**
     * 生成文件访问URL
     */
    private String generateFileUrl(String endpoint, String bucketName, String objectName) {
        // 确保endpoint以https://开头
        if (!endpoint.startsWith("http://") && !endpoint.startsWith("https://")) {
            endpoint = "https://" + endpoint;
        }

        // 移除endpoint中的协议部分，然后重新组装
        String protocol = endpoint.split("://")[0];
        String domain = endpoint.split("://")[1];

        return protocol + "://" + bucketName + "." + domain + "/" + objectName;
    }

    /**
     * 从完整URL中提取对象名称
     * @param fileUrl 完整的文件访问URL
     * @return 对象名称
     */
    public String extractObjectNameFromUrl(String fileUrl) {
        if (!StringUtils.hasText(fileUrl)) {
            return null;
        }

        try {
            // 找到域名后的第一个'/'位置
            int protocolIndex = fileUrl.indexOf("://");
            if (protocolIndex == -1) {
                return null;
            }

            String afterProtocol = fileUrl.substring(protocolIndex + 3);
            int firstSlashIndex = afterProtocol.indexOf('/');
            if (firstSlashIndex == -1) {
                return null;
            }

            return afterProtocol.substring(firstSlashIndex + 1);
        } catch (Exception e) {
            log.error("从URL提取对象名称失败: {}", fileUrl, e);
            return null;
        }
    }
}