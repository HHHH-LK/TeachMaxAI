package com.aiproject.smartcampus.mapper;

import com.aiproject.smartcampus.pojo.vo.StudentWrongQuestionVO;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;


@SpringBootTest
@Slf4j
class StudentMapperTest {

    @Autowired
    private StudentMapper studentMapper;

    @Test
    void getStudentByStudentNumber() {
        List<StudentWrongQuestionVO> studentWrongQuestionVOS = studentMapper.selectWrongQuestion("1","1");
        StringBuffer sb = new StringBuffer();
        for (StudentWrongQuestionVO studentWrongQuestionVO : studentWrongQuestionVOS) {
            sb.append(studentWrongQuestionVO.toString());
            sb.append('\n');
        }

        log.info("studentWrongQuestionVOS = {}", sb.toString());

    }


}