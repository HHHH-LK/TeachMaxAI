package com.aiproject.smartcampus.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @program: ss
 * @description: ai生成章节测试题
 * @author: lk_hhh
 * @create: 2025-07-08 11:33
 **/

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChapterTestCreateDTO {

    private String chapterId;

    private String require;



}