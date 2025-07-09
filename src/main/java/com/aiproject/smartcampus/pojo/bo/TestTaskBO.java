package com.aiproject.smartcampus.pojo.bo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @program: ss
 * @description: 测试生成任务
 * @author: lk_hhh
 * @create: 2025-07-03 03:00
 **/

@AllArgsConstructor
@NoArgsConstructor
@Data
public class TestTaskBO {

    private Integer studentId;
    private Integer courseId;
    private Integer chapterId;
    private String content;


}