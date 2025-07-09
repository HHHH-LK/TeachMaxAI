package com.aiproject.smartcampus.mapper;

import com.aiproject.smartcampus.pojo.vo.ChapterQuestionDetailVO;
import com.aiproject.smartcampus.pojo.vo.KnowledgePointMaterialSimpleVO;
import com.aiproject.smartcampus.pojo.vo.KnowledgePointMaterialVO;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;


@SpringBootTest
@Slf4j
class ChapterMapperTest {

    @Autowired
    private ChapterMapper chapterMapper;

    @Test
    void selectTestByTypeTest() {

        List<ChapterQuestionDetailVO> chapterQuestionDetailVOS = chapterMapper.selectTestByType("1", "1", "test");
        System.out.println(chapterQuestionDetailVOS.size());


    }


}