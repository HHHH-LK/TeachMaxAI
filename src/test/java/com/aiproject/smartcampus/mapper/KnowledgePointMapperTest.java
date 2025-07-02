package com.aiproject.smartcampus.mapper;

import com.aiproject.smartcampus.pojo.bo.StudentWrongKnowledgeBO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;


@SpringBootTest
class KnowledgePointMapperTest {

    @Autowired
    private KnowledgePointMapper knowledgePointMapper;

    @Test
    void getStudentWrongKnowledgeByStudentId() {
        List<StudentWrongKnowledgeBO> studentWrongKnowledgeByStudentId = knowledgePointMapper.getStudentWrongKnowledgeByStudentId("1");
        System.out.printf("studentWrongKnowledgeByStudentId=%s\n", studentWrongKnowledgeByStudentId);
    }

}