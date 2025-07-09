package com.aiproject.smartcampus.service.impl;

import com.aiproject.smartcampus.pojo.dto.StudentAnswerDTO;
import com.aiproject.smartcampus.pojo.dto.StudentExamAnswerDTO;
import com.aiproject.smartcampus.service.StudentService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;


@SpringBootTest
@Slf4j
class StudentServiceImplTest {

    @Autowired
    StudentService studentService;

    @Test
    void getStudentById() {
        String s = studentService.academicAnalysis("1");
        log.info("<UNK>:{}", s);
    }

    @Test
    void getStudentById2() {
        StudentAnswerDTO answer1 = StudentAnswerDTO.builder()
                .questionId(101)
                .studentAnswer(" A ")
                .build();

        StudentAnswerDTO answer2 = StudentAnswerDTO.builder()
                .questionId(102)
                .studentAnswer("\"B,C\"")
                .build();

        StudentAnswerDTO answer3 = StudentAnswerDTO.builder()
                .questionId(103)
                .studentAnswer(" true ")
                .build();

        StudentAnswerDTO answer4 = StudentAnswerDTO.builder()
                .questionId(104)
                .studentAnswer(" 过程是这样的：首先…… ")
                .build();

        StudentExamAnswerDTO exam20250707001 = new StudentExamAnswerDTO(
                "10000",
                Arrays.asList(answer1, answer2, answer3, answer4)
        );

        studentService.finshExam(exam20250707001);
    }

}