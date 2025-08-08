package com.aiproject.smartcampus.controller;

import com.aiproject.smartcampus.commons.client.Result;
import com.aiproject.smartcampus.pojo.dto.*;
import com.aiproject.smartcampus.pojo.po.Chapter;
import com.aiproject.smartcampus.pojo.vo.*;
import com.aiproject.smartcampus.service.ChapterService;
import dev.langchain4j.agent.tool.P;
import io.swagger.v3.oas.annotations.Operation;
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
     * 根据课程id查询章节查询章节信息
     */
    @GetMapping("/getChapter")
    @Operation(summary = "根据课程ID获取章节信息", description = "查询指定课程的所有章节信息")
    public Result<List<ChapterTeacherVO>> getChapterByCourseId(@RequestParam(value = "courseId") String courseId) {

        return chapterService.getChapterByCourseId(courseId);
    }


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
    public Result<List<ChapterQuestionDetailVO>> selectAllTextByChaperId(@RequestParam(value = "chapterId") String chapterId, @RequestParam(value = "courseId") String courseId) {

        return chapterService.getAllTextByChapterId(chapterId,courseId);
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
    @PostMapping("/ju/test")
    public Result juTest(@RequestBody StudentTextAnswerDTO studentTextAnswerDTO) {

        return chapterService.juTest(studentTextAnswerDTO);
    }

    /**
     * 修改章节名称
     */
    @PostMapping("/updateChapterName")
    @Operation(summary = "修改章节名称", description = "根据章节ID修改章节名称")
    public Result<String> updateChapterName(@RequestBody Chapter chapter) {
        return chapterService.updateChapterName(chapter);
    }

    /**
     * 删除章节
     */
    @DeleteMapping("/deleteChapter")
    @Operation(summary = "删除章节", description = "根据章节ID删除指定章节")
    public Result<String> deleteChapter(@RequestParam(value = "chapterId") String chapterId) {
        return chapterService.deleteChapter(chapterId);
    }

    /**
     * 添加章节
     */
    @PostMapping("/addChapter")
    @Operation(summary = "添加章节", description = "根据课程ID添加新章节")
    public Result<String> addChapter(@RequestBody Chapter chapter) {
        return chapterService.addChapter(chapter);
    }


}