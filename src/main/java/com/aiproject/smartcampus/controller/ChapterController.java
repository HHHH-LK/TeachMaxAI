package com.aiproject.smartcampus.controller;

import com.aiproject.smartcampus.commons.client.Result;
import com.aiproject.smartcampus.pojo.dto.*;
import com.aiproject.smartcampus.pojo.vo.*;
import com.aiproject.smartcampus.service.ChapterService;
import dev.langchain4j.agent.tool.P;
import lombok.RequiredArgsConstructor;
import org.apache.ibatis.annotations.Param;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @program: ss
 * @description: 章节管理
 * @author: lk_hhh
 * @create: 2025-07-04 15:52
 **/

@RestController
@RequestMapping("/chapter")
@RequiredArgsConstructor
public class ChapterController {

    private final ChapterService chapterService;


    /**
     * 根据课程id查询章节查询章节信息以及学生章节学习情况
     */
    @GetMapping("/selectchapter")
    public Result<List<CourseChapterVO>> selectChapterByCrouseId(@RequestParam(value = "courseId") String courseId) {

        return chapterService.selectChapterByCrouseId(courseId);
    }

    /**
     * 根据章节id查询所有知识点信息
     */
    @GetMapping("/selectAllKnowledgeBychaptId")
    public Result<List<ChapterKnowledgePointVO>> selectAllKnowledgeChaptId(@RequestParam(value = "chaptId") String chaptId) {

        return chapterService.selectAllKnowledgeByChaptId(chaptId);

    }

    /**
     * 根据章节获取学习资源
     */
    @GetMapping("/selectAllMaterialBypointId")
    public Result<KnowledgePointMaterialSimpleSpliderVO> selectAllMaterialBypointId(@RequestParam(value = "chapterId") String chapterId, @RequestParam(value = "courseId") String courseId) {

        return chapterService.selectAllMaterialBypointId(chapterId, courseId);

    }

    /**
     * 根据资料id来进行获取学习资源的详细信息
     */
    @GetMapping("/selectOneBymaterialId")
    public Result<MaterialDetailSeparatedVO> selectOneBymaterialId(@RequestParam(value = "materialId") String materialId) {

        return chapterService.selectMaterialByMaterialId(materialId);

    }


    /**
     * 根据章节获取相关测试题
     */
    @GetMapping("/selectAllTextByChapterId")
    public Result<List<ChapterQuestionDetailVO>> selectAllTextByChaperId(@RequestParam(value = "chapterId") String chapterId) {

        return chapterService.getAllTextByChapterId(chapterId);
    }


    /**
     * 开始进行章节学习
     */

    @PostMapping("/chapterstudy/start")
    public Result study(@RequestBody StudentStudyDTO studentStudyDTO) {

        return chapterService.startStudy(studentStudyDTO);
    }

    /**
     * 退出章节学习，还未完成
     */
    @PostMapping("/chapterstudy/end")
    public Result end(@RequestBody StudentStudyDTO studentStudyDTO) {

        return chapterService.endStudy(studentStudyDTO);

    }

    /**
     * 完成章节学习
     */
    @PostMapping("/chapter/finsh")
    public Result finsh(@RequestBody StudentStudyDTO studentStudyDTO) {

        return chapterService.finsh(studentStudyDTO);
    }

    /**
     * 章节学习进度分析
     */
    @PostMapping("/setlearningprogress")
    public Result setlearningprogress(@RequestParam(value = "chapterId") String chapterId, @RequestParam("courseId") String courseId) {

        return chapterService.getlearningprogress(chapterId, courseId);
    }


    /**
     * 查询学生历史错题
     */
    @GetMapping("/getAllNotcoreectTest")
    public Result getAllNotcoreectTest(@RequestParam(value = "courseId") String courseId) {

        return chapterService.getAllUncorrectTest(courseId);
    }

    /**
     * 提交章节测试
     */

    @PostMapping("/setStudentAwser")
    public Result setStudentAwser(@RequestBody StudentTextAnswerDTO studentTextAnswerDTO) {

        return chapterService.setStudentAwser(studentTextAnswerDTO);
    }


    /**
     * 章节测试题评判
     */
    @GetMapping("/ju/test")
    public Result juTest(@RequestBody StudentTextAnswerDTO studentTextAnswerDTO) {

        return chapterService.juTest(studentTextAnswerDTO);
    }


}