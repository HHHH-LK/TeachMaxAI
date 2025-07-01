package com.aiproject.smartcampus.functioncalling;

import com.aiproject.smartcampus.functioncalling.toolutils.NotMasterTestCreatetoolUtils;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @program: SmartCampus
 * @description: 未掌握知识点测试生成
 * @author: lk_hhh
 * @create: 2025-07-01 10:26
 **/

@Data
@Slf4j
@Component
public class NotMasterTestCreateTool implements Tool {

    private final NotMasterTestCreatetoolUtils notMasterTestCreatetoolUtils;
    private Integer studentId;
    private Integer courseId;
    private String result;

    @Override
    public void run() {
        log.info("正在进行创建测试题");
        String test = notMasterTestCreatetoolUtils.createTest(String.valueOf(studentId), courseId);
        log.info("test:{}", test);
        log.info("测试题已经生成成功，生成结果为{}",test);
        result = test;

    }

}