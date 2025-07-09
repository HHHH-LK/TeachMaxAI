package com.aiproject.smartcampus.mapper;

import com.aiproject.smartcampus.commons.utils.JsonUtils;
import com.aiproject.smartcampus.pojo.bo.ExamCorrectAnswer;
import com.aiproject.smartcampus.pojo.po.QuestionBank;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.sql.CommonDataSource;

import static org.junit.jupiter.api.Assertions.*;


@SpringBootTest
@Slf4j
class QuestionBankMapperTest {

    @Autowired
    private QuestionBankMapper questionBankMapper;
    @Autowired
    private CommonDataSource commonDataSource;

    @Test
    void questionBankMapperTest() throws Exception {
        QuestionBank questionBank = questionBankMapper.selectById(3);
        String correctAnswer = questionBank.getCorrectAnswer();
        log.info("correctAnswer:{}", correctAnswer);
        ExamCorrectAnswer jsonObject =(ExamCorrectAnswer) JsonUtils.toJsonObject(correctAnswer, ExamCorrectAnswer.class);
        log.info("jsonObject:{}", jsonObject.toString());

    }

    @Test
    void questionBankMapperTest2() throws Exception {

        Object ss=true;
        String ss1 = (String) ss;
        log.info("ss1:{}", ss1);

    }

}