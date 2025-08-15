package com.aiproject.smartcampus.pojo.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * @program: ss
 * @description: 学生学习
 * @author: lk_hhh
 * @create: 2025-07-08 11:27
 **/

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StudentStudyDTO {

    private String chapterId;

    private String nowmaterialId;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime studyTime;

    private String courseId;

}