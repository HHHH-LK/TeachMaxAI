package com.aiproject.smartcampus.mapper;

import com.aiproject.smartcampus.pojo.vo.ChapterQuestionDetailVO;
import com.aiproject.smartcampus.pojo.vo.KnowledgePointMaterialSimpleVO;
import com.aiproject.smartcampus.pojo.vo.KnowledgePointMaterialVO;
import com.aiproject.smartcampus.pojo.vo.WrongQuestionVO;
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


    @Test
    void getChaptersByCourseId() {
    }

    @Test
    void getChapterCountByCourseId() {
    }

    @Test
    void getNextChapterOrder() {
    }

    @Test
    void getChaptersByDifficulty() {
    }

    @Test
    void getChaptersByOrderRange() {
    }

    @Test
    void getAllChapterByCourseId() {
    }

    @Test
    void getAllKnowledgeByChapterId() {
    }

    @Test
    void selectMaterialsByChapterStudentCourse() {
    }

    @Test
    void selectChapterMaterialById() {
    }

    @Test
    void getAllTextByChapterId() {

        List<ChapterQuestionDetailVO> chapterTestQuestions = chapterMapper.getChapterTestQuestions(1, 1, 1);
        System.out.println(chapterTestQuestions);

    }

    @Test
    void getALlFinishMasterial() {
    }

    @Test
    void getChapterProgressRateAsDouble() {
    }

    @Test
    void getAllNuCorrectTest() {

        List<WrongQuestionVO> allNuCorrectTest = chapterMapper.getAllNuCorrectTest("1", "1");
        for (WrongQuestionVO wrongQuestionVO : allNuCorrectTest) {
            System.out.println(wrongQuestionVO);
        }
    }

    @Test
    void selectTestByType() {
    }

    @Test
    void getallChapterKnowleageByIscore() {
    }

    @Test
    void getChapterNameById() {
        String questionContent = chapterMapper.getQuestionContent(1);
        System.out.println(questionContent);
    }
}