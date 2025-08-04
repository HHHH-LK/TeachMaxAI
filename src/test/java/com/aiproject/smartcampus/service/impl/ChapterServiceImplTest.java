package com.aiproject.smartcampus.service.impl;

import com.aiproject.smartcampus.commons.client.Result;
import com.aiproject.smartcampus.mapper.ChapterMapper;
import com.aiproject.smartcampus.pojo.po.Chapter;
import com.aiproject.smartcampus.pojo.vo.ChapterQuestionDetailVO;
import com.aiproject.smartcampus.pojo.vo.CourseChapterVO;
import com.aiproject.smartcampus.pojo.vo.KnowledgePointMaterialSimpleSpliderVO;
import com.aiproject.smartcampus.pojo.vo.MaterialDetailSeparatedVO;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Slf4j
class ChapterServiceImplTest {

    @Autowired
    private ChapterMapper chapterMapper;
    @Autowired
    private ChapterServiceImpl chapterServiceImpl;


    @Test
    void getAllChapterByCourseId(){
        List<CourseChapterVO> allChapterByCourseId = chapterMapper.getAllChapterByCourseId("1", "1");
        System.out.println(allChapterByCourseId);
    }

    @Test
    void getChapterByCourseId(){
        List<Chapter> chaptersByCourseId = chapterMapper.getChaptersByCourseId(1);
        System.out.println(chaptersByCourseId);
    }

    @Test
    void getChapterById(){

        Result<MaterialDetailSeparatedVO> materialDetailSeparatedVOResult = chapterServiceImpl.selectMaterialByMaterialId("1");
        log.info("materialDetailSeparatedVOResult:{}",materialDetailSeparatedVOResult.getData());

    }

    @Test
    void getChapterByChapterId(){
        Result<KnowledgePointMaterialSimpleSpliderVO> knowledgePointMaterialSimpleSpliderVOResult = chapterServiceImpl.selectAllMaterialBypointId("1","1");
        KnowledgePointMaterialSimpleSpliderVO data = knowledgePointMaterialSimpleSpliderVOResult.getData();
        log.info("knowledgePointMaterialSimpleSpliderVOResult:{}",data);
        log.info("外部资源"+data.getExternalList()+"数量"+data.getExternalList().size());
        log.info("课程资源"+data.getCourseList()+"数量"+data.getCourseList().size());
        log.info("总数量"+data.getTotal());


    }


    @Test
    void getChapterByCourseId2(){

        Result<List<ChapterQuestionDetailVO>> allTextByChapterId = chapterServiceImpl.getAllTextByChapterId("1", "1");
        for (ChapterQuestionDetailVO chapterQuestionDetailVO : allTextByChapterId.getData()) {
            System.out.println(chapterQuestionDetailVO);
        }


    }

    @Test
    void getChapterByCourseId3(){



    }



}