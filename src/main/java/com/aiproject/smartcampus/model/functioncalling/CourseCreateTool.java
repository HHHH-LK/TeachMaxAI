package com.aiproject.smartcampus.model.functioncalling;

import com.aiproject.smartcampus.mapper.ChapterKnowledgePointMapper;
import com.aiproject.smartcampus.mapper.ChapterMapper;
import com.aiproject.smartcampus.mapper.CourseMapper;
import com.aiproject.smartcampus.mapper.KnowledgePointMapper;
import com.aiproject.smartcampus.model.functioncalling.toolutils.CreateCourseStructureToolUtils;
import com.aiproject.smartcampus.pojo.po.Chapter;
import com.aiproject.smartcampus.pojo.po.ChapterKnowledgePoint;
import com.aiproject.smartcampus.pojo.po.Course;
import com.aiproject.smartcampus.pojo.po.KnowledgePoint;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Slf4j
@Component
@RequiredArgsConstructor
public class CourseCreateTool implements Tool {

    private final CreateCourseStructureToolUtils createCourseStructureToolUtils;
    private final CourseMapper courseMapper;
    private final ChapterMapper chapterMapper;
    private final KnowledgePointMapper knowledgePointMapper;
    private final ChapterKnowledgePointMapper chapterKnowledgePointMapper;

    // 课程信息
    private Course course;

    // 生成结果
    private String result;

    @Override
    @Transactional
    public void run() {
        log.info("正在创建课程: {}", course != null ? course.getCourseName() : "未知");

        // 参数校验
        if (!validateParameters()) {
            return;
        }

        try {
            // 生成课程结构
            String courseStructure = createCourseStructureToolUtils.createCourseStructure(course);
            result = courseStructure;

            // 解析并保存课程结构
            saveCourseStructure(courseStructure);

            log.info("课程创建成功: {}", course.getCourseName());
            result = "课程创建成功 - " + course.getCourseId();

        } catch (Exception e) {
            log.error("课程创建失败", e);
            result = "课程创建失败: " + e.getMessage();
        }
    }

    /**
     * 保存课程结构到数据库
     */
    private void saveCourseStructure(String courseStructure) throws Exception {
        // 解析AI返回的课程结构
        CreateCourseStructureToolUtils.CourseStructure structure =
                createCourseStructureToolUtils.parseAIResponse(courseStructure);

        // 设置课程状态为活跃
        course.setStatus(Course.CourseStatus.ACTIVE);
        course.setCreatedAt(LocalDateTime.now());

        // 保存课程主体
        courseMapper.insert(course);
        Integer courseId = course.getCourseId();

        // 保存章节和知识点
        for (CreateCourseStructureToolUtils.ChapterStructure chapter : structure.getChapters()) {
            // 保存章节
            Chapter chapterEntity = new Chapter();
            chapterEntity.setCourseId(courseId);
            chapterEntity.setChapterName(chapter.getChapterName());
            chapterEntity.setDescription(chapter.getChapterDescription());
            chapterEntity.setChapterOrder(chapter.getChapterOrder());
            chapterEntity.setCreatedAt(LocalDateTime.now());
            chapterEntity.setUpdatedAt(LocalDateTime.now());
            chapterMapper.insert(chapterEntity);
            Integer chapterId = chapterEntity.getChapterId();

            // 保存知识点和关联关系
            List<KnowledgePoint> savedPoints = new ArrayList<>();
            for (int i = 0; i < chapter.getKnowledgePoints().size(); i++) {
                CreateCourseStructureToolUtils.KnowledgePointStructure point = chapter.getKnowledgePoints().get(i);

                // 保存知识点
                KnowledgePoint pointEntity = new KnowledgePoint();
                pointEntity.setCourseId(courseId);
                pointEntity.setPointName(point.getPointName());
                pointEntity.setDescription(point.getDescription());
                pointEntity.setDifficultyLevel(point.getDifficultyLevel());
                pointEntity.setKeywords(point.getKeywords());
                knowledgePointMapper.insert(pointEntity);
                savedPoints.add(pointEntity);
            }

            // 保存章节知识点关联
            saveChapterKnowledgePoints(chapterId, savedPoints);

            log.info("章节[{}]保存完成，包含{}个知识点",
                    chapter.getChapterName(),
                    chapter.getKnowledgePoints().size());
        }
    }

    /**
     * 保存章节知识点关联关系
     */
    private void saveChapterKnowledgePoints(Integer chapterId, List<KnowledgePoint> knowledgePoints) {
        List<ChapterKnowledgePoint> relations = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (int i = 0; i < knowledgePoints.size(); i++) {
            KnowledgePoint point = knowledgePoints.get(i);

            ChapterKnowledgePoint relation = new ChapterKnowledgePoint();
            relation.setChapterId(chapterId);
            relation.setPointId(point.getPointId());
            relation.setPointOrder(i + 1); // 顺序从1开始
            relation.setIsCore(true); // 默认都是核心知识点
            relation.setCreatedAt(now);
            relation.setUpdatedAt(now);

            relations.add(relation);
        }

        // 批量保存关联关系
        chapterKnowledgePointMapper.batchInsert(relations);
    }

    /**
     * 参数校验
     */
    private boolean validateParameters() {
        if (course == null) {
            log.error("课程信息不能为空");
            result = "课程信息不能为空";
            return false;
        }

        if (!StringUtils.hasText(course.getCourseName())) {
            log.error("课程名称不能为空");
            result = "课程名称不能为空";
            return false;
        }

        return true;
    }
}