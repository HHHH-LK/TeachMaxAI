package com.aiproject.smartcampus.functioncalling;

import com.aiproject.smartcampus.functioncalling.toolutils.NotMasterTestCreatetoolUtils;
import com.aiproject.smartcampus.mapper.QuestionBankMapper;
import com.aiproject.smartcampus.mapper.StudentKnowledgeMasteryMapper;
import com.aiproject.smartcampus.pojo.bo.KnowledgepointBO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class NotMasterTestCreateToolTest {

    @Autowired
    private NotMasterTestCreateTool notMasterTestCreateTool;
    @Autowired
    private StudentKnowledgeMasteryMapper studentKnowledgeMasteryMapper;

    @Test
    void createTest(){

        notMasterTestCreateTool.setStudentId(1);
        notMasterTestCreateTool.setCourseId(1);
        notMasterTestCreateTool.run();


    }

    @Test
    void createTest2(){

        List<KnowledgepointBO> notMasterKnowledgepoints = studentKnowledgeMasteryMapper.getNotMasterKnowledgepoints(1, 1);
        System.out.printf("notMasterKnowledgepoints=%s",notMasterKnowledgepoints);

    }




}