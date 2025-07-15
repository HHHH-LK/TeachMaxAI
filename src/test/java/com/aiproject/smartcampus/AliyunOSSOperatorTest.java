package com.aiproject.smartcampus;

import com.aiproject.smartcampus.commons.utils.aliyunossutils.AliyunOSSOperator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

@SpringBootTest
public class AliyunOSSOperatorTest {

    @Autowired
    private AliyunOSSOperator aliyunOSSOperator;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Test
    public void testUpload() throws Exception {
        // 这里用一个本地文件路径，测试上传文件
        String filePath = "demo.pptx"; // 请替换成你测试用的文件路径
        byte[] content = Files.readAllBytes(Paths.get(filePath));
        String originalFilename = "demo.pptx";

        String url = aliyunOSSOperator.upload(content, originalFilename);

        System.out.println("上传后的文件访问URL: " + url);

        // 你也可以断言URL是否符合预期格式
        assert url != null && url.startsWith("http");
    }

    @Test
    public void testDeleteFile() {



    }
}
