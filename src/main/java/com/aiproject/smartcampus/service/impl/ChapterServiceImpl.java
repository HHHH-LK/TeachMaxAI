package com.aiproject.smartcampus.service.impl;

import com.aiproject.smartcampus.commons.client.Result;
import com.aiproject.smartcampus.commons.utils.UserToTypeUtils;
import com.aiproject.smartcampus.exception.StudentExpection;
import com.aiproject.smartcampus.mapper.*;
import com.aiproject.smartcampus.pojo.bo.StudentWrongKnowledgeBO;
import com.aiproject.smartcampus.pojo.dto.ChapterTestCreateDTO;
import com.aiproject.smartcampus.pojo.dto.StudentAnswerDTO;
import com.aiproject.smartcampus.pojo.dto.StudentStudyDTO;
import com.aiproject.smartcampus.pojo.dto.StudentTextAnswerDTO;
import com.aiproject.smartcampus.pojo.po.QuestionBank;
import com.aiproject.smartcampus.pojo.po.StudentAnswer;
import com.aiproject.smartcampus.pojo.po.StudentChapterProgress;
import com.aiproject.smartcampus.pojo.vo.*;
import com.aiproject.smartcampus.service.ChapterService;
import com.aliyun.core.utils.StringUtils;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static com.aiproject.smartcampus.contest.ChapterContest.STUDY_START;

/**
 * @program: ss
 * @description: 章节管理实现
 * @author: lk_hhh
 * @create: 2025-07-04 15:53
 **/

@Service
@RequiredArgsConstructor
@Slf4j
public class ChapterServiceImpl implements ChapterService {

    private final ChapterMapper chapterMapper;
    private final UserToTypeUtils userToTypeUtils;
    private final StringRedisTemplate stringRedisTemplate;
    private final StudentChapterProgressMapper studentChapterProgressMapper;
    private final KnowledgePointMapper knowledgePointMapper;
    private final QuestionBankMapper questionBankMapper;
    private final StudentAnswerMapper studentAnswerMapper;

    //查询指定课程中的章节

    @Override
    public Result<List<CourseChapterVO>> selectChapterByCrouseId(String courseId) {

        String studentId = userToTypeUtils.change();
        List<CourseChapterVO> allChapterByCourseId = chapterMapper.getAllChapterByCourseId(courseId, studentId);

        return Result.success(allChapterByCourseId);

    }

    @Override
    public Result<List<ChapterKnowledgePointVO>> selectAllKnowledgeByChaptId(String chaptId) {

        List<ChapterKnowledgePointVO> allKnowledgeByChapterId = chapterMapper.getAllKnowledgeByChapterId(chaptId);
        log.info("章节[{}]知识点数量为:{}", chaptId, allKnowledgeByChapterId.size());

        return Result.success(allKnowledgeByChapterId);
    }

    @Override
    public Result<KnowledgePointMaterialSimpleSpliderVO> selectAllMaterialBypointId(String chapterId, String courseId) {
        try {

            String studentId = userToTypeUtils.change();

            // 2. 查询出所有资源
            List<KnowledgePointMaterialSimpleVO> knowledgePointMaterialVOS =
                    chapterMapper.selectMaterialsByChapterStudentCourse(chapterId, studentId, courseId);

            if (knowledgePointMaterialVOS.isEmpty()) {
                // 返回空的分类结果
                KnowledgePointMaterialSimpleSpliderVO emptyResult = new KnowledgePointMaterialSimpleSpliderVO();
                emptyResult.setExternalList(new ArrayList<>());
                emptyResult.setCourseList(new ArrayList<>());
                emptyResult.setExternalTotal(0);
                emptyResult.setCourseTotal(0);
                emptyResult.setTotal(0);
                return Result.success(emptyResult);
            }

            // 3. 进行分类处理
            KnowledgePointMaterialSimpleSpliderVO result = classifyMaterialsDetailed(knowledgePointMaterialVOS);

            return Result.success(result);

        } catch (Exception e) {
            log.error("查询知识点资料失败，chapter: {},course;{}", chapterId, chapterId, e);
            return Result.error("查询知识点资料失败");
        }
    }


    @Override
    public Result<MaterialDetailSeparatedVO> selectMaterialByMaterialId(String materialId) {
        try {

            String studentId = userToTypeUtils.change();


            // 2. 查询资料详细信息
            MaterialDetailRawVO rawData = chapterMapper.selectChapterMaterialById(materialId, studentId);

            if (rawData == null) {
                return Result.error("资料不存在");
            }

            // 3. 构建分离的详细信息VO
            MaterialDetailSeparatedVO result = MaterialDetailSeparatedVO.builder()
                    .materialId(rawData.getMaterialId())
                    .materialTitle(rawData.getMaterialTitle())
                    .materialDescription(rawData.getMaterialDescription())
                    .materialType(rawData.getMaterialType())
                    .estimatedTime(rawData.getEstimatedTime())
                    .difficultyLevel(rawData.getDifficultyLevel())
                    .isDownloadable(rawData.getIsDownloadable())
                    .tags(rawData.getTags())
                    .materialSource(rawData.getMaterialSource())
                    .createdAt(rawData.getCreatedAt())
                    .updatedAt(rawData.getUpdatedAt())
                    .courseId(rawData.getCourseId())
                    .courseName(rawData.getCourseName())
                    .semester(rawData.getSemester())
                    .chapterId(rawData.getChapterId())
                    .chapterName(rawData.getChapterName())
                    .chapterOrder(rawData.getChapterOrder())
                    .creatorName(rawData.getCreatorName())
                    .build();

            // 4.1 处理课件资源
            if (rawData.getCoursewareResourceId() != null &&
                    (rawData.getFileName() != null || rawData.getFileUrl() != null)) {

                CoursewareResourceVO coursewareResource = CoursewareResourceVO.builder()
                        .resourceId(rawData.getCoursewareResourceId())
                        .fileName(rawData.getFileName())
                        .fileUrl(rawData.getFileUrl())
                        .fileSizeMb(rawData.getFileSizeMb())
                        .fileType(rawData.getFileType())
                        .downloadCount(rawData.getDownloadCount())
                        .isPublic(rawData.getIsPublic())
                        .isDownloadable(rawData.getIsDownloadable())
                        .build();

                result.setCoursewareResource(coursewareResource);
            }

            // 4.2 处理外部资源
            if (rawData.getExternalResourceUrl() != null &&
                    !rawData.getExternalResourceUrl().trim().isEmpty()) {

                ExternalResourceVO externalResource = ExternalResourceVO.builder()
                        .resourceUrl(rawData.getExternalResourceUrl())
                        .resourceType(determineExternalResourceType(rawData.getExternalResourceUrl()))
                        .build();

                result.setExternalResource(externalResource);
            }
            return Result.success(result);
        } catch (Exception e) {
            log.error("查询资料详细信息失败，materialId: {}", materialId, e);
            return Result.error("查询资料详细信息失败");
        }

    }

    @Override
    public Result<List<ChapterQuestionDetailVO>> getAllTextByChapterId(String chapterId, String courseId) {

        String studentId = "1";

        try {
            List<ChapterQuestionDetailVO> allTextByChapterId = chapterMapper.getChapterTestQuestions(
                    Integer.valueOf(chapterId),
                    Integer.valueOf(studentId),
                    Integer.valueOf(courseId)
            );

            // 🔧 添加调试日志，验证数据是否正确读取
            log.info("查询到 {} 道题目", allTextByChapterId != null ? allTextByChapterId.size() : 0);

            if (allTextByChapterId != null && !allTextByChapterId.isEmpty()) {
                // 🔧 检查第一道题的内容是否正确读取
                ChapterQuestionDetailVO firstQuestion = allTextByChapterId.get(0);
                log.info("第一道题目内容: {}",
                        firstQuestion.getQuestionContent() != null ?
                                firstQuestion.getQuestionContent().substring(0, Math.min(50, firstQuestion.getQuestionContent().length())) + "..." :
                                "null");
            }

            if (allTextByChapterId == null || allTextByChapterId.isEmpty()) {
                log.info("测试题为空");
                return Result.success(new ArrayList<>());
            }

            // 获取对应的测试题
            List<ChapterQuestionDetailVO> theExamTest = getTheExamTest(allTextByChapterId);
            log.info("筛选后的测试题数量: {}", theExamTest.size());

            // 为每道题目设计权重
            List<ChapterQuestionDetailVO> chapterQuestionDetailVOSWithT = setTToAllQuestion(theExamTest);

            // 进行排序
            List<ChapterQuestionDetailVO> theCorrectTextList = chapterQuestionDetailVOSWithT.stream()
                    .sorted((s, q) -> s.getT() - q.getT())
                    .toList();

            log.info("最终返回题目数量: {}", theCorrectTextList.size());
            return Result.success(theCorrectTextList);

        } catch (Exception e) {
            log.error("获取章节测试题失败", e);
            return Result.error("获取章节测试题失败: " + e.getMessage());
        }
    }

    @Override
    public Result startStudy(StudentStudyDTO studentStudyDTO) {

        String studentId = userToTypeUtils.change();
        String key = STUDY_START + studentId + "_" + studentStudyDTO.getNowmaterialId() + "_" + studentStudyDTO.getChapterId();

        LocalDateTime studyTime = studentStudyDTO.getStudyTime();

        stringRedisTemplate.opsForValue().set(key, studyTime.toString());

        return Result.success();
    }

    //todo 添加学习进度管理
    @Override
    public Result endStudy(StudentStudyDTO studentStudyDTO) {

        String studentId = userToTypeUtils.change();
        String chapterId = studentStudyDTO.getChapterId();
        String nowmaterialId = studentStudyDTO.getNowmaterialId();
        String courseId = studentStudyDTO.getCourseId();

        String key = STUDY_START + studentId + "_" + nowmaterialId + "_" + chapterId;

        String studyStartTime = stringRedisTemplate.opsForValue().get(key);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime StartTime = LocalDateTime.parse(studyStartTime, formatter);

        LocalDateTime studyEndTime = studentStudyDTO.getStudyTime();

        //获取学习持续时间
        long hours = Duration.between(StartTime, studyEndTime).toHours();


        //todo 计算学习进度（基于学习时长 + 知识点通过率 + 单个资源完成情况）
        double learningProgress = calculateLearningProgress(studentId, chapterId, courseId, hours);


        LambdaUpdateWrapper<StudentChapterProgress> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(!StringUtils.isBlank(chapterId), StudentChapterProgress::getChapterId, chapterId);
        updateWrapper.eq(!StringUtils.isBlank(studentId), StudentChapterProgress::getStudentId, studentId);
        updateWrapper.eq(!StringUtils.isBlank(nowmaterialId), StudentChapterProgress::getCurrentMaterialId, nowmaterialId);
        updateWrapper.set(StudentChapterProgress::getMasteryLevel, "learning");
        updateWrapper.setSql("study_time = study_time + " + hours);
        updateWrapper.set(StudentChapterProgress::getLastStudyAt, studyEndTime);
        updateWrapper.set(StudentChapterProgress::getProgressRate, learningProgress);

        int update = studentChapterProgressMapper.update(updateWrapper);
        if (update > 0) {
            return Result.success();
        } else {
            log.error("退出学习资源错误");
            return Result.error("系统错误");
        }
    }

    @Override
    public Result finsh(StudentStudyDTO studentStudyDTO) {

        String nowmaterialId = studentStudyDTO.getNowmaterialId();
        String chapterId = studentStudyDTO.getChapterId();
        String studentId = userToTypeUtils.change();

        LambdaUpdateWrapper<StudentChapterProgress> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(!StringUtils.isBlank(chapterId), StudentChapterProgress::getChapterId, chapterId);
        updateWrapper.eq(!StringUtils.isBlank(studentId), StudentChapterProgress::getStudentId, studentId);
        updateWrapper.eq(!StringUtils.isBlank(nowmaterialId), StudentChapterProgress::getCurrentMaterialId, nowmaterialId);
        updateWrapper.set(StudentChapterProgress::getMasteryLevel, "completed");
        updateWrapper.setSql("completed_materials = CONCAT('[" + nowmaterialId + ",', SUBSTRING(completed_materials, 2))");
        int update = studentChapterProgressMapper.update(updateWrapper);

        if (update <= 0) {
            log.error("用户完成课程资源学习错误");
            return Result.error("系统异常");
        }

        return Result.success();
    }


    @Override
    public Result getlearningprogress(String chapterId, String courseId) {

        String studentId = userToTypeUtils.change();

        //进行查寻章节学习进度
        Double chapterProgressRateAsDouble = chapterMapper.getChapterProgressRateAsDouble(chapterId, studentId, chapterId);

        return Result.success(chapterProgressRateAsDouble);
    }

    @Override
    public Result getAllUncorrectTest(String courseId) {

        String studentId = userToTypeUtils.change();

        List<WrongQuestionVO> allNuCorrectTest = chapterMapper.getAllNuCorrectTest(courseId, studentId);

        if (allNuCorrectTest == null || allNuCorrectTest.isEmpty()) {
            log.warn("查询错题结果为null");
            return Result.success(new ArrayList<>());
        }

        //对答题情况进行排序返回
        List<WrongQuestionVO> list = allNuCorrectTest.stream().sorted(
                (a, b) -> a.getAnswerId().compareTo(b.getAnswerId())
        ).toList();


        return Result.success(list);
    }

    @Override
    public Result setStudentAwser(StudentTextAnswerDTO studentTextAnswerDTO) {

        // 参数校验
        List<StudentAnswerDTO> studentAnswerDTOList = studentTextAnswerDTO.getStudentAnswerDTOList();
        if (studentAnswerDTOList == null || studentAnswerDTOList.isEmpty()) {
            log.warn("学生未作答");
            return Result.success();
        }

        String studentId = userToTypeUtils.change();
        String chapterId = studentTextAnswerDTO.getChapterId();
        String courseId = studentTextAnswerDTO.getCourseId();
        String type = studentTextAnswerDTO.getType();

        // 一次性转换为Map，提高查询效率
        Map<Integer, StudentAnswerDTO> dtoMap = studentAnswerDTOList.stream()
                .collect(Collectors.toMap(StudentAnswerDTO::getQuestionId, dto -> dto));

        // 查询出试题
        List<ChapterQuestionDetailVO> chapterQuestionDetailVOS = chapterMapper.selectTestByType(chapterId, courseId, type);

        // 对试卷进行排版
        List<ChapterQuestionDetailVO> sortedQuestions = setTToAllQuestion(chapterQuestionDetailVOS).stream()
                .sorted((a, b) -> a.getT() - b.getT())
                .toList();

        // 创建答题结果列表
        List<StudentAnswerResultVO> questionResults = new ArrayList<>();

        int questionIndex = 0;

        // 遍历题目列表处理学生答题
        for (ChapterQuestionDetailVO chapterQuestionDetailVO : sortedQuestions) {

            Integer questionId = chapterQuestionDetailVO.getQuestionId();
            String correctAnswer = chapterQuestionDetailVO.getCorrectAnswer();

            // 校验题目是否存在
            QuestionBank questionBank = questionBankMapper.selectById(questionId);
            if (questionBank == null) {
                log.error("题目{}不存在", questionId);
                throw new StudentExpection("考试题目不存在");
            }

            log.info("提取出学生的第{}题目信息", questionIndex++);

            // 创建单题结果VO
            StudentAnswerResultVO studentAnswerResultVO = new StudentAnswerResultVO();
            BeanUtils.copyProperties(chapterQuestionDetailVO, studentAnswerResultVO);

            // 获取学生答案
            StudentAnswerDTO studentAnswerDTO = dtoMap.get(questionId);

            if (studentAnswerDTO != null) {
                String studentAnswer = studentAnswerDTO.getFormattedAnswer();

                // 判断答案是否正确
                boolean isCorrect = studentAnswer != null && studentAnswer.equals(correctAnswer);

                // 设置结果信息
                studentAnswerResultVO.setStudentAnswer(studentAnswer);
                studentAnswerResultVO.setAnsweredAt(LocalDateTime.now());
                studentAnswerResultVO.setIsCorrect(isCorrect);
                studentAnswerResultVO.setScoreEarned(studentAnswerResultVO.calculateScoreEarned());

                // 保存学生答题记录
                StudentAnswer studentAnswers = new StudentAnswer();
                studentAnswers.setStudentId(Integer.valueOf(studentId));
                studentAnswers.setQuestionId(questionId);
                studentAnswers.setStudentAnswer(studentAnswerDTO.getFormattedAnswer());
                studentAnswers.setIsCorrect(isCorrect);
                studentAnswers.setScoreEarned(studentAnswerResultVO.getScoreEarned());

                studentAnswerMapper.insert(studentAnswers);

            } else {
                // 学生未答题
                studentAnswerResultVO.setStudentAnswer(null);
                studentAnswerResultVO.setIsCorrect(false);
                studentAnswerResultVO.setScoreEarned(BigDecimal.ZERO);
            }

            // 设置答题状态
            studentAnswerResultVO.setAnswerStatusByCorrectness();
            questionResults.add(studentAnswerResultVO);
        }

        // 创建整体测试结果
        ChapterTestResultVO testResult = ChapterTestResultVO.builder()
                .studentId(studentId)
                .chapterId(chapterId)
                .courseId(courseId)
                .type(type)
                .questionResults(questionResults)
                .testEndTime(LocalDateTime.now())
                .build();

        // 计算统计信息
        testResult.calculateStatistics();

        return Result.success(testResult);
    }

    @Override
    public Result juTest(StudentTextAnswerDTO studentTextAnswerDTO) {

        String studentId = userToTypeUtils.change();
        String chapterId = studentTextAnswerDTO.getChapterId();
        String courseId = studentTextAnswerDTO.getCourseId();
        String type = studentTextAnswerDTO.getType();
        List<StudentAnswerDTO> studentAnswerDTOList = studentTextAnswerDTO.getStudentAnswerDTOList();

        // 一次性转换为Map，后续查询更高效
        Map<Integer, StudentAnswerDTO> dtoMap = studentAnswerDTOList.stream()
                .collect(Collectors.toMap(StudentAnswerDTO::getQuestionId, dto -> dto));

        // 查询出试题
        List<ChapterQuestionDetailVO> chapterQuestionDetailVOS = chapterMapper.selectTestByType(chapterId, courseId, type);

        // 对试卷进行排版
        List<ChapterQuestionDetailVO> sortedQuestions = setTToAllQuestion(chapterQuestionDetailVOS).stream()
                .sorted((a, b) -> a.getT() - b.getT())
                .toList();

        // 创建答题结果列表
        List<StudentAnswerResultVO> questionResults = new ArrayList<>();

        // 遍历题目列表获取用户的答题情况
        for (ChapterQuestionDetailVO chapterQuestionDetailVO : sortedQuestions) {

            // 创建单题结果VO
            StudentAnswerResultVO studentAnswerResultVO = new StudentAnswerResultVO();

            // 复制题目基本信息
            BeanUtils.copyProperties(chapterQuestionDetailVO, studentAnswerResultVO);

            // 获取题目ID和正确答案
            Integer questionId = chapterQuestionDetailVO.getQuestionId();
            String correctAnswer = chapterQuestionDetailVO.getCorrectAnswer();

            // 获取用户的答案
            StudentAnswerDTO studentAnswerDTO = dtoMap.get(questionId);

            if (studentAnswerDTO != null) {
                String studentAnswer = studentAnswerDTO.getStudentAnswer();
                studentAnswerResultVO.setStudentAnswer(studentAnswer);
                studentAnswerResultVO.setAnsweredAt(LocalDateTime.now()); // 或从DTO中获取真实时间

                // 判断答案是否正确
                boolean isCorrect = studentAnswer != null && studentAnswer.equals(correctAnswer);
                studentAnswerResultVO.setIsCorrect(isCorrect);
                // 计算得分
                studentAnswerResultVO.setScoreEarned(studentAnswerResultVO.calculateScoreEarned());

                // 更新数据库中的答题记录
                LambdaUpdateWrapper<StudentAnswer> updateWrapper = new LambdaUpdateWrapper<>();
                updateWrapper.eq(StudentAnswer::getStudentId, studentId);
                updateWrapper.eq(StudentAnswer::getQuestionId, questionId);
                updateWrapper.set(StudentAnswer::getIsCorrect, isCorrect);
                updateWrapper.set(StudentAnswer::getScoreEarned, studentAnswerResultVO.getScoreEarned());
                studentAnswerMapper.update(null, updateWrapper);

            } else {
                // 学生未答题
                studentAnswerResultVO.setStudentAnswer(null);
                studentAnswerResultVO.setIsCorrect(false);
                studentAnswerResultVO.setScoreEarned(BigDecimal.ZERO);
            }

            // 设置答题状态
            studentAnswerResultVO.setAnswerStatusByCorrectness();
            questionResults.add(studentAnswerResultVO);
        }

        // 创建整体测试结果
        ChapterTestResultVO testResult = ChapterTestResultVO.builder()
                .studentId(studentId)
                .chapterId(chapterId)
                .courseId(courseId)
                .type(type)
                .questionResults(questionResults)
                .testEndTime(LocalDateTime.now())
                .build();

        // 计算统计信息
        testResult.calculateStatistics();

        return Result.success(testResult);
    }


    /**
     * 获取对应的题型
     */
    private List<ChapterQuestionDetailVO> getTheExamTest(List<ChapterQuestionDetailVO> chapterQuestionDetailVOS) {

        List<ChapterQuestionDetailVO> result = chapterQuestionDetailVOS.stream()
                .filter(a -> "测试题".equals(a.getChapterQuestionTypeCn()))
                .toList();

        log.debug("筛选测试题: 原始数量={}, 筛选后数量={}", chapterQuestionDetailVOS.size(), result.size());
        return result;
    }

    /**
     * 为每个题型设置权重，便于后续的题目排版
     */
    private List<ChapterQuestionDetailVO> setTToAllQuestion(List<ChapterQuestionDetailVO> allTextByChapterId) {

        for (ChapterQuestionDetailVO vo : allTextByChapterId) {
            int weight = vo.getQuestionTypeWeight() * 10 + vo.getDifficultyWeight();
            vo.setT(weight);

            log.debug("题目ID: {}, 类型权重: {}, 难度权重: {}, 最终权重: {}",
                    vo.getQuestionId(), vo.getQuestionTypeWeight(), vo.getDifficultyWeight(), weight);
        }
        return allTextByChapterId;
    }


    /**
     * 分类和统计
     */
    private KnowledgePointMaterialSimpleSpliderVO classifyMaterialsDetailed(List<KnowledgePointMaterialSimpleVO> materials) {

        Map<Boolean, List<KnowledgePointMaterialSimpleVO>> classifiedMaterials = materials.stream()
                .collect(Collectors.partitioningBy(material -> isExternalResource(material.getMaterialType())));

        List<KnowledgePointMaterialSimpleVO> externalList = classifiedMaterials.get(true);
        List<KnowledgePointMaterialSimpleVO> courseList = classifiedMaterials.get(false);

        externalList.sort(Comparator.comparing(KnowledgePointMaterialSimpleVO::getMaterialTitle));
        courseList.sort(Comparator.comparing(KnowledgePointMaterialSimpleVO::getMaterialTitle));

        // 构建返回结果
        KnowledgePointMaterialSimpleSpliderVO result = new KnowledgePointMaterialSimpleSpliderVO();
        result.setExternalList(externalList);
        result.setCourseList(courseList);
        result.setExternalTotal(externalList.size());
        result.setCourseTotal(courseList.size());
        result.setTotal(materials.size());

        return result;
    }

    /**
     * 判断是否为外部资源
     *
     * @param materialType 资料类型
     * @return true-外部资源，false-课程资源
     */
    private boolean isExternalResource(String materialType) {
        // 1. 空值校验
        if (materialType == null || materialType.trim().isEmpty()) {
            return false;
        }

        // 2. 转换为小写，便于比较
        String lowerType = materialType.toLowerCase().trim();

        // 3. 定义外部资源类型集合
        Set<String> externalTypes = Set.of(
                "link",        // 链接
                "reference",   // 参考资料
                "external",    // 外部资源
                "url",         // 网址
                "website",     // 网站
                "online"       // 在线资源
        );

        // 4. 定义课程资源类型集合
        Set<String> courseTypes = Set.of(
                "courseware",  // 课件
                "video",       // 视频
                "document",    // 文档
                "exercise",    // 练习
                "supplement",  // 补充资料
                "material",    // 教材
                "slides",      // 幻灯片
                "pdf",         // PDF文档
                "word",        // Word文档
                "ppt",         // PPT文档
                "excel"        // Excel文档
        );

        // 5. 精确匹配判断
        if (externalTypes.contains(lowerType)) {
            return true;
        }

        if (courseTypes.contains(lowerType)) {
            return false;
        }

        // 6. 模糊匹配判断（处理复合类型名称）
        // 检查是否包含外部资源关键词
        for (String externalKeyword : externalTypes) {
            if (lowerType.contains(externalKeyword)) {
                return true;
            }
        }

        // 7. 特殊情况处理
        // 检查是否包含明显的外部资源标识
        if (lowerType.contains("http") ||
                lowerType.contains("www") ||
                lowerType.contains("链接") ||
                lowerType.contains("外部") ||
                lowerType.contains("第三方") ||
                lowerType.contains("推荐") ||
                lowerType.contains("扩展")) {
            return true;
        }

        // 8. 检查是否包含明显的课程资源标识
        if (lowerType.contains("课件") ||
                lowerType.contains("教材") ||
                lowerType.contains("讲义") ||
                lowerType.contains("课程") ||
                lowerType.contains("内部")) {
            return false;
        }

        // 9. 默认情况：未知类型默认为课程资源
        // 这样可以确保大部分教学资料被正确分类
        return false;
    }

    /**
     * 判断外部资源类型
     */
    private String determineExternalResourceType(String url) {
        if (url == null) {
            return "unknown";
        }

        String lowerUrl = url.toLowerCase();
        if (lowerUrl.contains("youtube.com") || lowerUrl.contains("youtu.be")) {
            return "youtube";
        } else if (lowerUrl.contains("bilibili.com")) {
            return "bilibili";
        } else if (lowerUrl.contains(".pdf")) {
            return "pdf";
        } else if (lowerUrl.contains("github.com")) {
            return "github";
        } else if (lowerUrl.contains("baidu.com") || lowerUrl.contains("google.com")) {
            return "search";
        } else {
            return "website";
        }
    }

    /**
     * 基于学习时长 + 知识点通过率 + 单个资源完成情况
     */
    public double calculateLearningProgress(String studentId, String chapterId, String courseId, double hours) {
        try {
            // ========== 权重配置（可以根据需要调整） ==========
            double knowledgeWeight = 0.4;    // 知识点准确率权重40%
            double completionWeight = 0.4;   // 资源完成率权重40%
            double timeWeight = 0.2;         // 时间效率权重20%

            // ========== 1. 计算知识点平均通过率 ==========
            List<StudentWrongKnowledgeBO> studentWrongKnowledgeByStudentId =
                    knowledgePointMapper.getStudentWrongKnowledgeByStudentId(studentId);

            double aveAccuracyRate = 0.0;
            boolean hasKnowledgeData = false;

            if (studentWrongKnowledgeByStudentId != null && !studentWrongKnowledgeByStudentId.isEmpty()) {
                // 过滤掉null值，计算有效准确率
                double total = studentWrongKnowledgeByStudentId.stream()
                        .filter(item -> item.getAccuracyRate() != null)
                        .mapToDouble(StudentWrongKnowledgeBO::getAccuracyRate)
                        .sum();

                long validCount = studentWrongKnowledgeByStudentId.stream()
                        .filter(item -> item.getAccuracyRate() != null)
                        .count();

                if (validCount > 0) {
                    aveAccuracyRate = total / validCount;
                    hasKnowledgeData = true;
                    log.info("学生{}知识点准确率: {}%, 有效知识点数: {}", studentId, aveAccuracyRate, validCount);
                }
            }

            // 如果没有知识点数据，设置默认值并调整权重
            if (!hasKnowledgeData) {
                aveAccuracyRate = 85.0; // 默认85%准确率（相对保守）
                knowledgeWeight = 0.2;  // 降低知识点权重
                completionWeight = 0.6; // 提高完成率权重
                timeWeight = 0.2;       // 时间权重保持不变
                log.info("学生{}暂无知识点数据，使用默认准确率85%，调整权重配置", studentId);
            }

            // ========== 2. 计算资源完成情况 ==========
            List<KnowledgePointMaterialSimpleVO> allMaterials =
                    chapterMapper.selectMaterialsByChapterStudentCourse(chapterId, studentId, courseId);
            List<StudentChapterResourceVO> finishedMaterials =
                    chapterMapper.getALlFinishMasterial(studentId, chapterId, courseId);

            int totalMaterialCount = allMaterials != null ? allMaterials.size() : 0;
            int finishedMaterialCount = finishedMaterials != null ? finishedMaterials.size() : 0;

            // 计算完成率（百分比）
            double completionRate = 0.0;
            if (totalMaterialCount > 0) {
                completionRate = ((double) finishedMaterialCount / totalMaterialCount) * 100;
            }

            log.info("学生{}资源完成情况: {}/{}，完成率: {}%",
                    studentId, finishedMaterialCount, totalMaterialCount, completionRate);

            // ========== 3. 计算预估学习时长和时间效率 ==========
            double estimatedHours = 0.0;
            if (allMaterials != null) {
                estimatedHours = finishedMaterials.stream()
                        .filter(material -> material.getEstimatedTime() != null)
                        .mapToDouble(material -> material.getEstimatedTime() / 60.0) // 转换为小时
                        .sum();
            }

            // 计算时间效率得分
            double timeScore = calculateTimeEfficiencyScore(hours, estimatedHours);

            log.info("学生{}时间效率: 实际{}小时, 预估{}小时, 效率得分: {}",
                    studentId, hours, estimatedHours, timeScore);

            // ========== 4. 综合计算学习进度 ==========
            double learningProgress =
                    aveAccuracyRate * knowledgeWeight +
                            completionRate * completionWeight +
                            timeScore * timeWeight;

            // 确保结果在0-100范围内
            learningProgress = Math.max(0, Math.min(100, learningProgress));

            // ========== 5. 输出详细信息（便于调试和监控） ==========
            String progressLevel = getProgressLevel(learningProgress);

            log.info("学生{}学习进度计算完成:", studentId);
            log.info("  - 知识点准确率: {}% (权重{}%)", aveAccuracyRate, knowledgeWeight * 100);
            log.info("  - 资源完成率: {}% (权重{}%)", completionRate, completionWeight * 100);
            log.info("  - 时间效率得分: {}% (权重{}%)", timeScore, timeWeight * 100);
            log.info("  - 综合进度: {}% (等级: {})", learningProgress, progressLevel);

            return learningProgress;

        } catch (Exception e) {
            log.error("计算学习进度失败 - studentId: {}, chapterId: {}, courseId: {}",
                    studentId, chapterId, courseId, e);
            // 返回默认进度值，避免系统崩溃
            return 50.0;
        }
    }

    /**
     * 计算时间效率得分的辅助方法
     */
    private double calculateTimeEfficiencyScore(double actualHours, double estimatedHours) {
        // ========== 时间效率评分标准（可调整） ==========

        if (estimatedHours > 0) {
            // 有预估时长的情况：基于时间比例评分
            double ratio = actualHours / estimatedHours;

            if (ratio >= 0.8 && ratio <= 1.2) {
                return 100.0; // 在预期时间范围内（80%-120%），满分
            } else if (ratio >= 0.6 && ratio < 0.8) {
                return 95.0;  // 用时较少（60%-80%），高效学习
            } else if (ratio >= 0.5 && ratio < 0.6) {
                return 85.0;  // 用时很少（50%-60%），可能学习不够深入
            } else if (ratio > 1.2 && ratio <= 1.5) {
                return 85.0;  // 略微超时（120%-150%）
            } else if (ratio > 1.5 && ratio <= 2.0) {
                return 70.0;  // 超时较多（150%-200%）
            } else if (ratio > 2.0 && ratio <= 3.0) {
                return 60.0;  // 严重超时（200%-300%）
            } else if (ratio > 3.0) {
                return 40.0;  // 极度超时（>300%）
            } else {
                return 50.0;  // 用时过少（<50%），可能没有认真学习
            }

        } else {
            // 没有预估时长的情况：基于绝对学习时长评分
            if (actualHours >= 20) {
                return 100.0; // 学习时间充足（>=20小时）
            } else if (actualHours >= 15) {
                return 90.0;  // 学习时间很好（15-20小时）
            } else if (actualHours >= 10) {
                return 80.0;  // 学习时间较好（10-15小时）
            } else if (actualHours >= 5) {
                return 65.0;  // 学习时间一般（5-10小时）
            } else if (actualHours >= 2) {
                return 50.0;  // 学习时间较少（2-5小时）
            } else if (actualHours >= 1) {
                return 35.0;  // 学习时间很少（1-2小时）
            } else {
                return 20.0;  // 学习时间不足（<1小时）
            }
        }
    }

    /**
     * 根据进度分数获取等级的辅助方法
     */
    private String getProgressLevel(double progress) {
        // ========== 进度等级标准（可调整） ==========
        if (progress >= 90) {
            return "优秀";
        }
        if (progress >= 80) {
            return "良好";
        }
        if (progress >= 70) {
            return "中等";
        }
        if (progress >= 60) {
            return "及格";
        }
        return "待改进";
    }

    /**
     * 获取学习建议的辅助方法（可选）
     */
    private List<String> getLearningAdvice(double knowledgeAccuracy, double completionRate,
                                           double timeScore, double overallProgress) {
        List<String> advice = new ArrayList<>();

        // ========== 学习建议逻辑（可调整） ==========

        if (knowledgeAccuracy < 70) {
            advice.add("建议加强知识点练习，多做相关题目提高准确率");
        }

        if (completionRate < 60) {
            advice.add("建议完成更多学习资源，系统性学习课程内容");
        }

        if (timeScore < 70) {
            if (timeScore < 50) {
                advice.add("学习时间不足，建议增加学习投入");
            } else {
                advice.add("学习效率有待提高，建议优化学习方法");
            }
        }

        if (overallProgress >= 90) {
            advice.add("学习表现优秀，可以尝试挑战更高难度的内容！");
        } else if (overallProgress >= 70) {
            advice.add("学习进度良好，继续保持当前的学习节奏");
        } else if (overallProgress >= 60) {
            advice.add("学习进度尚可，建议加强薄弱环节的学习");
        } else {
            advice.add("学习进度较慢，建议制定详细的学习计划并严格执行");
        }

        return advice;
    }


}


