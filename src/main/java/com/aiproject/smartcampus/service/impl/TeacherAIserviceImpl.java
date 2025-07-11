package com.aiproject.smartcampus.service.impl;

import com.aiproject.smartcampus.commons.client.Result;
import com.aiproject.smartcampus.commons.utils.UserToTypeUtils;
import com.aiproject.smartcampus.mapper.*;
import com.aiproject.smartcampus.model.functioncalling.toolutils.ClassInfoAsicToolUtils;
import com.aiproject.smartcampus.model.functioncalling.toolutils.ExamCreaterToolUtils;
import com.aiproject.smartcampus.model.functioncalling.toolutils.ExamMarkingToolUtils;
import com.aiproject.smartcampus.model.functioncalling.toolutils.LessonPlanCreateToolUtils;
import com.aiproject.smartcampus.pojo.bo.StudentWrongKnowledgeBO;
import com.aiproject.smartcampus.pojo.dto.ChapterKnowledgePointDTO;
import com.aiproject.smartcampus.pojo.dto.TeacherGetSituationDTO;
import com.aiproject.smartcampus.pojo.dto.TeacherInfoDTO;
import com.aiproject.smartcampus.pojo.po.LessonPlan;
import com.aiproject.smartcampus.pojo.po.Teacher;
import com.aiproject.smartcampus.pojo.vo.ExamCreationResult;
import com.aiproject.smartcampus.service.TeacherAIservice;
import com.aiproject.smartcampus.service.TeacherService;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @program: ss
 * @description:
 * @author: lk_hhh
 * @create: 2025-07-11 04:23
 **/

@Service
@RequiredArgsConstructor
@Slf4j
public class TeacherAIserviceImpl implements TeacherAIservice {

    private final ExamMarkingToolUtils examMarkingToolUtils;
    private final ExamCreaterToolUtils examCreaterToolUtils;
    private final TeacherService teacherService;
    private final ClassInfoAsicToolUtils classInfoAsicToolUtils;
    private final CourseMapper courseMapper;
    private final TeacherMapper teacherMapper;
    private final LessonPlanCreateToolUtils lessonPlanCreateToolUtils;
    private final ChapterMapper chapterMapper;
    private final UserToTypeUtils userToTypeUtils;
    private final LessonPlanMapper lessonPlanMapper;

    @Override
    public Result aiMarkingExam(String studentId, String examId) {

        String mark = examMarkingToolUtils.mark(studentId, examId);

        return Result.success(mark);

    }

    /**
     * 智能创建试卷
     *
     * @param content  试卷要求描述
     * @param courseId 课程ID
     * @return 创建结果
     */
    @Override
    public ExamCreationResult createIntelligentExam(String content, String courseId) {
        try {
            log.info("开始创建智能试卷: courseId={}, content={}", courseId, content);

            // 调用工具类创建试卷
            ExamCreationResult result = examCreaterToolUtils.createExam(content, courseId);

            if (result.getSuccess()) {
                log.info("智能试卷创建成功: examId={}", result.getExamId());

                return result;
            } else {
                log.error("智能试卷创建失败: {}", result.getErrorMessage());
                return result;
            }

        } catch (Exception e) {
            log.error("创建智能试卷异常: courseId={}", courseId, e);
            return ExamCreationResult.failure("系统异常：" + e.getMessage());
        }
    }

    @Override
    public Result<String> aiclassAiayaisc(String courseId) {

        //获取班级详细情况
        TeacherGetSituationDTO teacherGetSituationDTOResult = teacherService.GetAllSituation(Integer.valueOf(courseId)).getData();
        String classSituation = teacherGetSituationDTOResult.toString();

        //获取班级高频错误知识点分布
        List<StudentWrongKnowledgeBO> wrongKnowledgeList = teacherService.getTheMaxUncorrectPoint(courseId).getData();
        StringBuffer sb = new StringBuffer();
        sb.append("接下来是班级高频错误知识点\n");
        for (int i = 1; i < wrongKnowledgeList.size(); i++) {
            sb.append("班级出错率第" + i + "高的知识点:");
            sb.append(wrongKnowledgeList.get(i - 1).toString());
            sb.append("\n");
        }

        StringBuffer sb2 = new StringBuffer();
        sb2.append("班级详细成绩信息情况为" + classSituation);
        sb2.append("\n");
        sb2.append(sb.toString());
        String result = sb2.toString();

        //基于ai智能生成学情报告
        String classInfoAsic = classInfoAsicToolUtils.createClassInfoAsic(result);

        return Result.success(classInfoAsic);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<String> TeacherTextCreate(String content, String courseId, String chapterId) {

        //前置条件 查询课程章节重要知识点
        List<ChapterKnowledgePointDTO> chapterKnowledgePointDTOS = chapterMapper.getallChapterKnowleageByIscore(courseId, chapterId);
        //基于courseId 查询对应点课程名字
        String courseNameByid = courseMapper.findCourseNameByid(courseId);
        //基于章节id查询对应的章节name
        String chapterNameById = chapterMapper.getChapterNameById(chapterId);

        //汇总信息
        StringBuffer sb = new StringBuffer("课程" + courseNameByid + "对应章节" + chapterNameById + "下有以下知识点");
        for (ChapterKnowledgePointDTO chapterKnowledgePointDTO : chapterKnowledgePointDTOS) {
            sb.append(chapterKnowledgePointDTO.toString());
            sb.append("\n");
        }
        String classAndChapterInfo = sb.toString();


        //获取班级高频错误知识点分布
        StringBuffer sb1 = new StringBuffer();
        List<StudentWrongKnowledgeBO> wrongKnowledgeList = teacherService.getTheMaxUncorrectPoint(courseId).getData();
        sb1.append("接下来是班级高频错误知识点\n");
        for (int i = 1; i < wrongKnowledgeList.size(); i++) {
            sb1.append("班级出错率第" + i + "高的知识点:");
            sb1.append(wrongKnowledgeList.get(i - 1).toString());
            sb1.append("\n");
        }
        String wrongKnowledgeInfo = sb1.toString();


        //获取教师信息
        //todo 测试数据
        /*String teacherId = userToTypeUtils.change();*/
        String teacherId = "1";
        TeacherInfoDTO teacherInfoById = teacherMapper.findTeacherInfoById(teacherId);
        String teacerInfo = teacherInfoById.toString();

        //后续可以添加更多约束信息用来强化生成教案的质量

        //生成教案
        String lessonPlan = lessonPlanCreateToolUtils.createLessonPlan(content, classAndChapterInfo, wrongKnowledgeInfo, teacerInfo);
        //生成教案标题
        String planTitle = teacherInfoById.getRealName() + "老师" + courseNameByid + "课程" + chapterNameById + "章节" + "教案";

        //入库存储
        LessonPlan lessonPlanInfo = new LessonPlan();
        lessonPlanInfo.setCourseId(Integer.valueOf(courseId));
        lessonPlanInfo.setTeacherId(Integer.valueOf(teacherId));
        lessonPlanInfo.setChapterId(Integer.valueOf(chapterId));
        lessonPlanInfo.setPlanTitle(planTitle);
        lessonPlanInfo.setPlanContent(lessonPlan);
        int insert = lessonPlanMapper.insert(lessonPlanInfo);
        if(insert<1){
            log.error("教案存入失败");
            throw new RuntimeException("教案存入失败");
        }

        return Result.success(lessonPlan);
    }

    /**
     * 获取试卷JSON内容
     *
     * @param content  试卷要求
     * @param courseId 课程ID
     * @return 试卷JSON字符串
     */
    public String getExamPaperJson(String content, String courseId) {
        ExamCreationResult result = createIntelligentExam(content, courseId);

        if (result.getSuccess()) {
            return result.getExamPaperJson();
        } else {
            log.error("获取试卷JSON失败: {}", result.getErrorMessage());
            return "{}";
        }
    }

    /**
     * 获取创建报告
     *
     * @param content  试卷要求
     * @param courseId 课程ID
     * @return 创建报告文本
     */
    public String getCreationReport(String content, String courseId) {
        ExamCreationResult result = createIntelligentExam(content, courseId);

        if (result.getSuccess()) {
            return result.getCreationReport();
        } else {
            return "创建失败：" + result.getErrorMessage();
        }
    }
}