package com.aiproject.smartcampus.mapper;

import com.aiproject.smartcampus.pojo.bo.StudentWrongKnowledgeBO;
import com.aiproject.smartcampus.pojo.vo.KnowledgePointSimpleVO;
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

    @Test
    void getStudentWrongKnowledgeByCourseId() {
        KnowledgePointSimpleVO knowledgeInformationByPointId = knowledgePointMapper.getKnowledgeInformationByPointId("4", "1");
        System.out.printf("knowledgeInformationByPointId=%s\n", knowledgeInformationByPointId);

    }

    @Test
    void getStudentWrongKnowledgeByPointId() {

        KnowledgePointSimpleVO knowledgeInformationByPointId = knowledgePointMapper.getKnowledgeInformationByPointId("43", "1");

        System.out.printf("knowledgeInformationByPointId=%s\n", knowledgeInformationByPointId);
    }

}