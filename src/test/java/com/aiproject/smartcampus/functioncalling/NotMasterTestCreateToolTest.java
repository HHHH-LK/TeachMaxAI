package com.aiproject.smartcampus.functioncalling;

import com.aiproject.smartcampus.model.functioncalling.NotMasterTestCreateTool;
import com.aiproject.smartcampus.mapper.StudentKnowledgeMasteryMapper;
import com.aiproject.smartcampus.pojo.bo.KnowledgepointBO;
import com.aiproject.smartcampus.pojo.bo.TestTaskBO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class NotMasterTestCreateToolTest {

    @Autowired
    private NotMasterTestCreateTool notMasterTestCreateTool;
    @Autowired
    private StudentKnowledgeMasteryMapper studentKnowledgeMasteryMapper;

    @Test
    void createTest(){

        TestTaskBO testTaskBO = new TestTaskBO();
        testTaskBO.setStudentId(1);
        testTaskBO.setContent("请帮我生成更加适合我的");
        testTaskBO.setChapterId(1);
        testTaskBO.setCourseId(1);
        notMasterTestCreateTool.setTestTaskBO(testTaskBO);
        notMasterTestCreateTool.setSimpleKnowledgeAnalysisBOList(new ArrayList<>());
        notMasterTestCreateTool.run();
        String result = notMasterTestCreateTool.getResult();
        System.out.println(result);


    }

    @Test
    void createTest2(){

        List<KnowledgepointBO> notMasterKnowledgepoints = studentKnowledgeMasteryMapper.getNotMasterKnowledgepoints(1, 1);
        System.out.printf("notMasterKnowledgepoints=%s",notMasterKnowledgepoints);

    }




}