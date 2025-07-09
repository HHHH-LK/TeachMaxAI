package com.aiproject.smartcampus.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @program: ss
 * @description:学生练习答案dto
 * @author: lk_hhh
 * @create: 2025-07-09 14:09
 **/

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StudentTextAnswerDTO {

    private String chapterId;

    private String type;

    private String courseId;

    private List<StudentAnswerDTO> studentAnswerDTOList;


}