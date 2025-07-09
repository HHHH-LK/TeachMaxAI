package com.aiproject.smartcampus.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @program: ss
 * @description: 学生做答接受类
 * @author: lk_hhh
 * @create: 2025-07-07 08:58
 **/

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StudentExamAnswerDTO {

    String examId;
    List<StudentAnswerDTO>  studentExamAnswerDTOList;


}