package com.aiproject.smartcampus.service.impl;

import com.aiproject.smartcampus.commons.client.Result;
import com.aiproject.smartcampus.commons.utils.UserToTypeUtils;
import com.aiproject.smartcampus.commons.utils.math.RoundingUtils;
import com.aiproject.smartcampus.mapper.*;
import com.aiproject.smartcampus.pojo.bo.StudentKnowBO;
import com.aiproject.smartcampus.pojo.bo.StudentWrongKnowledgeBO;
import com.aiproject.smartcampus.pojo.dto.TeacherGetSituationDTO;
import com.aiproject.smartcampus.pojo.dto.TeacherGetStudentDTO;
import com.aiproject.smartcampus.pojo.dto.TeacherQueryDTO;
import com.aiproject.smartcampus.pojo.po.*;
import com.aiproject.smartcampus.pojo.vo.*;
import com.aiproject.smartcampus.service.TeacherService;
import com.aliyun.core.utils.StringUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @program: SmartCampus
 * @description:
 * @author: lk
 * @create: 2025-05-20 09:40
 **/

@Service
@Slf4j
@RequiredArgsConstructor
public class TeacherServiceImpl extends ServiceImpl<TeacherMapper, Teacher> implements TeacherService {

    private final TeacherMapper teacherMapper;
    private final KnowledgePointMapper knowledgePointMapper;
    private final UserToTypeUtils userToTypeUtils;
    private final CourseMapper courseMapper;
    private final CourseEnrollmentMapper courseEnrollmentMapper;
    private final ExamMapper examMapper;
    private final ChapterMapper chapterMapper;
    private final StudentAnswerMapper studentAnswerMapper;
    private final ExamScoreMapper examScoresMapper;
    private final PaperQuestionMapper paperQuestionMapper;
    private final ExamPaperMapper examPaperMapper;
    private final AssignmentMapper assignmentMapper;
    private final CourseEnrollmentMapper enrollmentMapper;
    private final AssignmentSubmissionMapper submissionMapper;
    private final ExamScoreMapper examScoreMapper;
    private final StudentMapper studentMapper;
    private final UserMapper userMapper;
    private final QuestionBankMapper questionBankMapper;


    @Override
    public Result<TeacherQueryDTO> queryTeachersById(Integer userId) {

        try {
            TeacherQueryDTO teacher = teacherMapper.findByUserID(userId);
            if (teacher == null) {
                return Result.error("找不到ID为 " + userId + " 的用户");
            } else {
                return Result.success(teacher);
            }
        } catch (Exception e) {
            // 5. 错误处理
            log.error("查询教师信息失败", e);
            return Result.error("查询教师信息失败: " + e.getMessage());
        }
    }
//
//    @Override
//    public Result updateTeacherStatus(TeacherStatusDTO statusDTO) {
//        return null;
//    }

    @Override
    public Result updateTeacherInfo(Integer userId, TeacherQueryDTO updateDTO) {
        try {
            // 1. 参数校验
            if (updateDTO.getTeacherId() == null) {
                return Result.error("教师ID不能为空");
            }

            // 2. 检查教师是否存在
            TeacherQueryDTO teacher = teacherMapper.findByUserID(userId);

            teacherMapper.updateTeacherProfile(updateDTO); // 使用MapStruct更新字段
            return Result.success("教师信息更新成功");

        } catch (Exception e) {
            log.error("更新教师信息失败", e);
            return Result.error("更新教师信息失败: " + e.getMessage());
        }
    }

    @Override
    public Result<Map<Integer, Double>> getAllClassInfo(String courseId) {
        int pointSize = 124000;

        // 初始化准确率累加 Map 和计数 List
        Map<Integer, Double> pointMap = new ConcurrentHashMap<>(pointSize);
        List<Long> pointNumList = new ArrayList<>(Collections.nCopies(pointSize, 0L));

        for (int i = 0; i < pointSize; i++) {
            pointMap.put(i, 0.0);
        }

        // 查询当前老师 ID
        String teacherId = userToTypeUtils.change();

        // 查询该课程下所有学生 ID
        List<Integer> studentIdList = teacherMapper.selectAllClassStudentInfo(teacherId, courseId);

        if (studentIdList == null || studentIdList.isEmpty()) {
            return Result.success(Collections.emptyMap());
        }

        // 统计学生错误知识点
        for (Integer studentId : studentIdList) {
            List<StudentWrongKnowledgeBO> wrongList = knowledgePointMapper.getStudentWrongKnowledgeByStudentId(String.valueOf(studentId));
            if (wrongList == null) {
                continue;
            }

            for (StudentWrongKnowledgeBO bo : wrongList) {
                Integer pointId = bo.getPointId();
                Double accuracyRate = bo.getAccuracyRate();

                if (pointId == null || pointId < 0 || pointId >= pointSize || accuracyRate == null) {
                    continue;
                }

                // 累加准确率与统计数量
                pointMap.merge(pointId, accuracyRate, Double::sum);
                pointNumList.set(pointId, pointNumList.get(pointId) + 1);
            }
        }

        // 计算平均准确率（过滤无数据的项）
        Map<Integer, Double> avgMap = new HashMap<>();
        for (int i = 0; i < pointSize; i++) {
            long count = pointNumList.get(i);
            if (count > 0) {
                avgMap.put(i, pointMap.get(i) / count);
            }
        }

        return Result.success(avgMap);
    }

    @Override
    public Result<List<StudentWrongKnowledgeBO>> getTheMaxUncorrectPoint(String couresId) {

        int pointSize = 124000;
        Map<Integer, Long> pointMap = new ConcurrentHashMap<>(pointSize);
        Map<Integer, StudentWrongKnowledgeBO> studentWrongKnowledgeBOMap = new ConcurrentHashMap<>();
        for (int i = 0; i < pointSize; i++) {
            pointMap.put(i, 0L);
        }

        String teacherId = userToTypeUtils.change();
//        String teacherId = "1";
        // 查询该课程下所有学生 ID
        List<Integer> studentIdList = teacherMapper.selectAllClassStudentInfo(teacherId, couresId);

        for (Integer studentId : studentIdList) {

            List<StudentWrongKnowledgeBO> studentWrongKnowledgeByStudentId = knowledgePointMapper.getStudentWrongKnowledgeByStudentId(String.valueOf(studentId));
            if (studentWrongKnowledgeByStudentId == null || studentWrongKnowledgeByStudentId.isEmpty()) {
                continue;
            }

            //统计每个知识点的错误次数
            for (StudentWrongKnowledgeBO bo : studentWrongKnowledgeByStudentId) {
                Integer pointId = bo.getPointId();
                Integer wrongAnswerCount = bo.getWrongAnswerCount();
                pointMap.put(pointId, pointMap.get(pointId) + wrongAnswerCount);
                studentWrongKnowledgeBOMap.put(pointId, bo);

            }

        }

        List<StudentWrongKnowledgeBO> list = pointMap.entrySet().stream().sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue())).limit(6).map(a -> {
            return studentWrongKnowledgeBOMap.get(a.getKey());
        }).toList();


        return Result.success(list);
    }

    /**
     * 获取教师所教授的课程
     *
     * @param teacherId 教师ID
     * @return 课程列表
     */

    @Override
    public Result<List<Course>> GetAllCourse(Integer teacherId) {
        try {
            // 1. 参数校验
            if (teacherId == null) {
                return Result.error("教师ID不能为空");
            }

            // 2. 查询教师所教授的课程
            List<Course> courses = courseMapper.findCourseByTeacherId(teacherId);
            if (courses == null || courses.isEmpty()) {
                return Result.error("找不到ID为 " + teacherId + " 的教师所教授的课程");
            } else {
                return Result.success(courses);
            }
        } catch (Exception e) {
            log.error("获取教师所教授的课程失败", e);
            return Result.error("获取教师所教授的课程失败: " + e.getMessage());
        }
    }

    // 获取课程整体情况
    @Override
    public Result<TeacherGetSituationDTO> GetAllSituation(Integer courseId) {
        try {
            List<Double> scoreList = courseEnrollmentMapper.getStudentScores(courseId);
            if (scoreList == null || scoreList.isEmpty()) {
                return Result.error("课程ID为 " + courseId + " 的成绩信息为空");
            }

            List<Double> list = scoreList.stream().map(a -> {
                {
                    return a == null ? 0.0 : a;
                }
            }).toList();

            // 计算平均分
            int totalStudents = list.size();
            double averageScore = list.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);

            // 初始化各分数段计数器
            int failCount = 0;      // <60
            int passCount = 0;      // 60-69
            int normalCount = 0;    // 70-79
            int goodCount = 0;      // 80-89
            int excellentCount = 0; // 90-100

            // 手动遍历计算分数分布
            for (Double score : list) {
                if (score >= 90) {
                    excellentCount++;
                } else if (score >= 80) {
                    goodCount++;
                } else if (score >= 70) {
                    normalCount++;
                } else if (score >= 60) {
                    passCount++;
                } else {
                    failCount++;
                }
            }

            // 计算比率
            double passRate = totalStudents > 0 ? (100.0 * (totalStudents - failCount) / totalStudents) : 0.0;
            double excellentRate = totalStudents > 0 ? (100.0 * excellentCount / totalStudents) : 0.0;

            // 填充DTO对象
            TeacherGetSituationDTO situationDTO = new TeacherGetSituationDTO();
            situationDTO.setCourseId(courseId);
            situationDTO.setCourseName(""); // 需要补充课程名称查询逻辑
            situationDTO.setAverageScore(RoundingUtils.round(averageScore));
            situationDTO.setPassRate(RoundingUtils.round(passRate));
            situationDTO.setExcellentRate(RoundingUtils.round(excellentRate));
            situationDTO.setFailNumber(failCount);
            situationDTO.setPassNumber(passCount);
            situationDTO.setNormalNumber(normalCount);
            situationDTO.setGoodNumber(goodCount);
            situationDTO.setExcellentNumber(excellentCount);

            return Result.success(situationDTO);

        } catch (Exception e) {
            log.error("获取成绩信息失败", e);
            return Result.error("获取信息失败" + e.getMessage());
        }
    }

    //获取学生信息
    @Override
    public Result<List<TeacherGetStudentDTO>> getStudentInfo(Integer courseId) {
        try {
            // 1. 参数校验
            if (courseId == null) {
                return Result.error("课程ID不能为空");
            }

            // 2. 查询学生信息
            List<TeacherGetStudentDTO> studentInfoList = courseEnrollmentMapper.getStudentInfo(courseId);
            if (studentInfoList == null || studentInfoList.isEmpty()) {
                return Result.error("找不到课程ID为 " + courseId + " 的学生信息");
            } else {
                return Result.success(studentInfoList);
            }
        } catch (Exception e) {
            log.error("获取学生信息失败", e);
            return Result.error("获取学生信息失败: " + e.getMessage());
        }
    }

    //获取试卷信息
    @Override
    public Result<List<Exam>> getPaper(Integer courseId) {
        try {
            if (courseId == null) {
                return Result.error("课程ID不能为空");
            }
            List<Exam> exam = examMapper.findByCourseId(courseId);
            if (exam == null || exam.isEmpty()) {
                return Result.error("找不到课程ID为 " + courseId + " 的试卷信息");
            }
            return Result.success(exam);
        } catch (Exception e) {

            log.error("获取试卷信息失败", e);
            return Result.error("获取试卷信息失败: " + e.getMessage());
        }
    }

    @Override
    public Result<List<ChapterQuestionDetailTeacherVO>> getHomework(String courseId, String chapterId) {
        try {
            int courseIdInt = Integer.parseInt(courseId);
            int chapterIdInt = Integer.parseInt(chapterId);

            // 1. 查询章节题目
            List<ChapterQuestionDetailTeacherVO> questionList = chapterMapper.getChapterTestQuestionsForTeacher(chapterIdInt, courseIdInt);

            // 2. 添加日志
            log.info("教师查询 - 课程ID={} 章节ID={} 查询到 {} 道测试题", courseId, chapterId, questionList.size());

            if (questionList.isEmpty()) {
                log.info("章节测试题为空");
                return Result.success(questionList);
            }

            // 3. 处理第一题预览
            ChapterQuestionDetailTeacherVO firstQuestion = questionList.get(0);
            String preview = firstQuestion.getQuestionContent() != null && firstQuestion.getQuestionContent().length() > 50 ? firstQuestion.getQuestionContent().substring(0, 50) + "..." : (firstQuestion.getQuestionContent() != null ? firstQuestion.getQuestionContent() : "null");
            log.info("第一道题目内容: {}", preview);

            // 4. 设置默认权重（如果为空）
            questionList.forEach(question -> {
                if (question.getT() == null) {
                    // 根据题目类型设置默认权重
                    switch (question.getQuestionType()) {
                        case "single_choice":
                        case "true_false":
                            question.setT(1); // 单选题和判断题权重为1
                            break;
                        case "multiple_choice":
                            question.setT(2); // 多选题权重为2
                            break;
                        case "fill_blank":
                            question.setT(3); // 填空题权重为3
                            break;
                        case "short_answer":
                            question.setT(5); // 简答题权重为5
                            break;
                        default:
                            question.setT(1);
                    }
                    log.debug("题目ID={} 权重为空，设置默认权重为 {}", question.getQuestionId(), question.getT());
                }
            });

            // 5. 按权重排序
            List<ChapterQuestionDetailTeacherVO> sortedList = questionList.stream().sorted(Comparator.comparingInt(ChapterQuestionDetailTeacherVO::getT)).collect(Collectors.toList());

            log.info("最终返回测试题数量: {}", sortedList.size());
            return Result.success(sortedList);

        } catch (NumberFormatException e) {
            log.error("参数格式错误: courseId={}, chapterId={}", courseId, chapterId, e);
            return Result.error("参数格式错误");
        } catch (Exception e) {
            log.error("获取章节测试题失败", e);
            return Result.error("获取章节测试题失败: " + e.getMessage());
        }
    }

    @Override
    public Result<List<ChapterQuestionDetailVO>> getHomeworkByStudent(String studentId, String courseId, String chapterId) {
        try {
            // 1. 参数转换
            int chapterIdInt = Integer.parseInt(chapterId);
            int studentIdInt = Integer.parseInt(studentId);
            int courseIdInt = Integer.parseInt(courseId);

            // 2. 查询章节题目
            List<ChapterQuestionDetailVO> questions = chapterMapper.getChapterTestQuestions(chapterIdInt, studentIdInt, courseIdInt);

            // 3. 添加日志
            log.info("学生ID={} 课程ID={} 章节ID={} 查询到 {} 道题目", studentId, courseId, chapterId, questions.size());

            if (questions.isEmpty()) {
                log.info("章节测试题为空");
                return Result.success(questions);
            }

            // 4. 记录第一题预览
            ChapterQuestionDetailVO firstQuestion = questions.get(0);
            String preview = firstQuestion.getQuestionContent() != null && firstQuestion.getQuestionContent().length() > 50 ? firstQuestion.getQuestionContent().substring(0, 50) + "..." : firstQuestion.getQuestionContent();
            log.debug("第一题预览: {}", preview);

            // 5. 筛选测试题
            List<ChapterQuestionDetailVO> testQuestions = filterTestQuestions(questions);
            log.info("筛选后的测试题数量: {}", testQuestions.size());

            // 6. 设置权重
            List<ChapterQuestionDetailVO> weightedQuestions = setQuestionWeights(testQuestions);

            // 7. 按权重排序
            List<ChapterQuestionDetailVO> sortedQuestions = weightedQuestions.stream().sorted(Comparator.comparingInt(ChapterQuestionDetailVO::getT)).collect(Collectors.toList());

            log.info("最终返回题目数量: {}", sortedQuestions.size());
            return Result.success(sortedQuestions);

        } catch (NumberFormatException e) {
            log.error("参数格式错误: studentId={}, courseId={}, chapterId={}", studentId, courseId, chapterId, e);
            return Result.error("参数格式错误");
        } catch (Exception e) {
            log.error("获取章节测试题失败", e);
            return Result.error("获取章节测试题失败: " + e.getMessage());
        }
    }

    @Override
    public Result<List<ExamStudentVO>> getExamStudentInfo(String examId) {
//        String teacherId = userToTypeUtils.change();
        String teacherId = "1";
        try {
            // 1. 参数校验
            if (examId == null || examId.isBlank()) {
                return Result.error("考试ID不能为空");
            }

            // 2. 查询考试学生信息
            List<ExamStudentVO> studentInfoList = examMapper.getExamStudentInfo(examId, teacherId);
            if (studentInfoList == null || studentInfoList.isEmpty()) {
                return Result.error("找不到考试ID为 " + examId + " 的学生信息");
            } else {
                return Result.success(studentInfoList);
            }
        } catch (Exception e) {
            log.error("获取考试学生信息失败", e);
            return Result.error("获取考试学生信息失败: " + e.getMessage());
        }
    }


    @Override
    public Result releasePaper(String examId) {
        try {
            // 1. 参数校验
            if (examId == null || examId.isBlank()) {
                return Result.error("考试ID不能为空");
            }

            // 2. 查询考试信息
            Exam exam = examMapper.selectById(examId);
            if (exam == null) {
                return Result.error("找不到考试ID为 " + examId + " 的考试信息");
            }

            // 3. 检查考试是否已发布
            if ("released".equals(exam.getStatus())) {
                return Result.error("考试已发布，无法重复发布");
            }

            // 4. 更新考试状态为已发布
            exam.setStatus(Exam.ExamStatus.fromValue("scheduled"));
            examMapper.updateById(exam);

            log.info("考试ID={} 发布成功", examId);
            return Result.success("考试发布成功");

        } catch (Exception e) {
            log.error("发布试卷失败", e);
            return Result.error("发布试卷失败: " + e.getMessage());
        }
    }

    @Override
    public Result updateExamStatusById(String examId) {
        try {
            // 1. 验证和解析考试ID
            int id;
            try {
                id = Integer.parseInt(examId);
            } catch (NumberFormatException e) {
                return Result.error("无效的考试ID格式");
            }

            // 2. 获取考试信息
            Exam exam = examMapper.selectById(id);
            if (exam == null) {
                return Result.error("未找到考试信息");
            }

            // 3. 检查状态是否已结束
            if (exam.getStatus() == Exam.ExamStatus.COMPLETED) {
                return Result.success("考试已结束，无需更新");
            }

            // 4. 计算考试结束时间
            LocalDateTime examStart = exam.getExamDate();
            LocalDateTime examEnd = examStart.plusMinutes(exam.getDurationMinutes());

            // 5. 获取当前时间并比较
            LocalDateTime now = LocalDateTime.now();
            if (now.isAfter(examEnd) || now.isEqual(examEnd)) {
                // 6. 更新考试状态为 completed
                exam.setStatus(Exam.ExamStatus.COMPLETED);
                int result = examMapper.updateById(exam);

                if (result > 0) {
                    return Result.success("考试状态已更新为结束");
                } else {
                    return Result.error("更新状态失败");
                }
            } else {
                return Result.success("考试尚未结束");
            }
        } catch (Exception e) {
            log.error("更新考试状态失败: {}", e.getMessage(), e);
            return Result.error("服务器内部错误: " + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result deleteExamById(String examId) {
        try {
            // 1. 验证和解析考试ID
            int id;
            try {
                id = Integer.parseInt(examId);
            } catch (NumberFormatException e) {
                return Result.error("无效的考试ID格式");
            }

            // 2. 获取考试信息
            Exam exam = examMapper.selectById(id);
            if (exam == null) {
                return Result.error("未找到考试信息");
            }

            // 3. 检查考试状态
            if (exam.getStatus() != Exam.ExamStatus.DRAFT) {
                return Result.error("只能删除草稿状态的考试");
            }

            // 4. 开启事务，级联删除所有关联数据

            // 4.1 删除学生答题记录
            studentAnswerMapper.deleteByExamId(id);

            // 4.2 删除考试成绩
            examScoresMapper.deleteByExamId(id);

            // 4.3 删除试卷题目关联
            paperQuestionMapper.deleteByExamId(id);

            // 4.4 删除试卷
            examPaperMapper.deleteByExamId(id);

            // 4.5 删除考试记录
            int result = examMapper.deleteById(id);

            if (result > 0) {
                return Result.success("考试及相关数据删除成功");
            } else {
                return Result.error("删除考试失败");
            }
        } catch (Exception e) {
            log.error("删除考试及相关数据失败: {}", e.getMessage(), e);
            return Result.error("服务器内部错误: " + e.getMessage());
        }
    }

    @Override
    public Result<Integer> getUserIdByteacher(Integer teacherId) {

        Integer userIdByTeacherId = teacherMapper.getUserIdByTeacherId(teacherId);

        return Result.success(userIdByTeacherId);

    }

    @Override
    public Result<CourseSummaryVO> getStudentHomework(String courseId) {
        try {
            // 1. 验证课程ID
            if (StringUtils.isBlank(courseId)) {
                return Result.error("课程ID不能为空");
            }

            // 2. 获取课程基本信息
            CourseSummaryVO summary = new CourseSummaryVO();

            // 3. 获取课程总作业数
            Integer totalAssignments = assignmentMapper.countAssignmentsByCourseId(courseId);
            summary.setTotalAssignments(totalAssignments);

            // 4. 获取学生总数
            Integer totalStudents = enrollmentMapper.countEnrollmentsByCourseId(courseId);
            summary.setTotalStudents(totalStudents);

            // 5. 获取所有作业列表
            List<Assignment> assignments = assignmentMapper.selectByCourseId(courseId);

            // 6. 准备作业统计信息
            List<HomeworkVO> homeworkList = new ArrayList<>();
            BigDecimal totalScoreSum = BigDecimal.ZERO;
            int totalSubmissions = 0;

            for (Assignment assignment : assignments) {
                HomeworkVO homeworkVO = new HomeworkVO();

                // 基础信息
                homeworkVO.setAssignmentId(assignment.getAssignmentId());
                homeworkVO.setTitle(assignment.getTitle());
                homeworkVO.setDescription(assignment.getDescription());
                homeworkVO.setDueDate(assignment.getDueDate());
                homeworkVO.setMaxScore(assignment.getMaxScore());
                homeworkVO.setStatus(String.valueOf(assignment.getStatus()).toLowerCase());

                // 获取作业提交数据
                List<AssignmentSubmission> submissions = submissionMapper.findByAssignmentId(assignment.getAssignmentId());

                // 统计提交人数
                int submissionCount = submissions.size();
                homeworkVO.setSubmissionCount(submissionCount);

                // 计算提交率
                if (totalStudents > 0) {
                    BigDecimal rate = new BigDecimal(submissionCount)
                            .divide(new BigDecimal(totalStudents), 4, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100))
                            .setScale(2, RoundingMode.HALF_UP);
                    homeworkVO.setSubmissionRate(rate);
                } else {
                    homeworkVO.setSubmissionRate(BigDecimal.ZERO);
                }

                // 计算平均分和成绩分布
                BigDecimal scoreSum = BigDecimal.ZERO;
                int scoreA = 0, scoreB = 0, scoreC = 0, scoreD = 0, scoreF = 0;

                for (AssignmentSubmission submission : submissions) {
                    if (submission.getScore() != null) {
                        scoreSum = scoreSum.add(submission.getScore());

                        // 成绩分布统计
                        double score = submission.getScore().doubleValue();
                        if (score >= 90) scoreA++;
                        else if (score >= 80) scoreB++;
                        else if (score >= 70) scoreC++;
                        else if (score >= 60) scoreD++;
                        else scoreF++;
                    }
                }

                // 设置平均分
                if (submissionCount > 0) {
                    homeworkVO.setAverageScore(
                            scoreSum.divide(new BigDecimal(submissionCount), 2, RoundingMode.HALF_UP)
                    );
                } else {
                    homeworkVO.setAverageScore(BigDecimal.ZERO);
                }

                // 设置成绩分布
                homeworkVO.setScoreA(scoreA);
                homeworkVO.setScoreB(scoreB);
                homeworkVO.setScoreC(scoreC);
                homeworkVO.setScoreD(scoreD);
                homeworkVO.setScoreF(scoreF);

                // 添加到列表
                homeworkList.add(homeworkVO);

                // 累加总分数和总提交数
                totalScoreSum = totalScoreSum.add(scoreSum);
                totalSubmissions += submissionCount;
            }

            // 7. 计算课程整体平均分
            if (totalSubmissions > 0) {
                summary.setOverallAverageScore(
                        totalScoreSum.divide(new BigDecimal(totalSubmissions), 2, RoundingMode.HALF_UP)
                );
            } else {
                summary.setOverallAverageScore(BigDecimal.ZERO);
            }

            // 8. 计算课程整体提交率
            int totalPossibleSubmissions = totalAssignments * totalStudents;
            if (totalPossibleSubmissions > 0) {
                BigDecimal rate = new BigDecimal(totalSubmissions)
                        .divide(new BigDecimal(totalPossibleSubmissions), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .setScale(2, RoundingMode.HALF_UP);
                summary.setOverallSubmissionRate(rate);
            } else {
                summary.setOverallSubmissionRate(BigDecimal.ZERO);
            }

            // 9. 设置作业列表
            summary.setHomeworkList(homeworkList);

            return Result.success(summary);

        } catch (Exception e) {
            log.error("获取课程作业统计失败: {}", e.getMessage());
            return Result.error("获取课程作业统计失败");
        }
    }

    @Override
    public Result<ExamSummaryVO> getStudentExam(String courseId) {
        try {
            // 1. 验证课程ID
            if (StringUtils.isBlank(courseId)) {
                return Result.error("课程ID不能为空");
            }

            // 2. 创建返回对象
            ExamSummaryVO summary = new ExamSummaryVO();

            // 3. 获取学生总数
            Integer studentCount = enrollmentMapper.countEnrollmentsByCourseId(courseId);
            summary.setStudentCount(studentCount);

            // 4. 获取所有非草稿状态的考试
            List<Exam> exams = examMapper.findByCourseIdAndStatusNotDraft(courseId);
            summary.setExamCount(exams.size());

            // 5. 获取课程所有学生
            List<Student> allStudents = enrollmentMapper.findStudentsByCourseId(courseId);
            Map<Integer, Student> studentMap = allStudents.stream()
                    .collect(Collectors.toMap(Student::getStudentId, Function.identity()));

            // 6. 准备统计数据
            BigDecimal totalScoreSum = BigDecimal.ZERO;
            int totalScoreCount = 0;
            int passCount = 0;
            Map<Integer, Integer> scoreDistribution = new HashMap<>();

            // 7. 准备考试详情列表
            List<ExamDetailVO> examDetails = new ArrayList<>();

            for (Exam exam : exams) {
                ExamDetailVO examDetail = new ExamDetailVO();
                examDetail.setExamId(exam.getExamId());
                examDetail.setTitle(exam.getTitle());
                examDetail.setExamDate(exam.getExamDate());
                examDetail.setMaxScore(exam.getMaxScore());

                // 获取该考试的所有成绩记录
                List<ExamScore> scores = examScoreMapper.findByExamId(exam.getExamId());
                Map<Integer, ExamScore> scoreMap = scores.stream()
                        .collect(Collectors.toMap(ExamScore::getStudentId, Function.identity()));

                // 创建完整的学生成绩列表（包括未作答学生）
                List<StudentExamScoreVO> studentScores = new ArrayList<>();
                BigDecimal examScoreSum = BigDecimal.ZERO;
                int examScoreCount = 0;

                for (Student student : allStudents) {
                    StudentExamScoreVO studentScore = new StudentExamScoreVO();
                    studentScore.setStudentId(student.getStudentId());
                    studentScore.setStudentName(getStudentName(student.getUserId()));
                    studentScore.setStudentNumber(student.getStudentNumber());

                    // 获取学生成绩（如果有）
                    ExamScore score = scoreMap.get(student.getStudentId());
                    if (score != null && score.getScore() != null) {
                        studentScore.setScore(score.getScore());
                        studentScore.setSubmittedAt(score.getSubmittedAt());

                        // 累加考试总分
                        examScoreSum = examScoreSum.add(score.getScore());
                        examScoreCount++;

                        // 累加总统计数据
                        totalScoreSum = totalScoreSum.add(score.getScore());
                        totalScoreCount++;

                        // 统计及格率
                        if (score.getScore().compareTo(BigDecimal.valueOf(60)) >= 0) {
                            passCount++;
                        }

                        // 统计成绩分布
                        double scoreValue = score.getScore().doubleValue();
                        if (scoreValue >= 90) {
                            scoreDistribution.put(0, scoreDistribution.getOrDefault(0, 0) + 1);
                        } else if (scoreValue >= 80) {
                            scoreDistribution.put(1, scoreDistribution.getOrDefault(1, 0) + 1);
                        } else if (scoreValue >= 70) {
                            scoreDistribution.put(2, scoreDistribution.getOrDefault(2, 0) + 1);
                        } else if (scoreValue >= 60) {
                            scoreDistribution.put(3, scoreDistribution.getOrDefault(3, 0) + 1);
                        } else {
                            scoreDistribution.put(4, scoreDistribution.getOrDefault(4, 0) + 1);
                        }
                    } else {
                        // 未作答学生
                        studentScore.setScore(null);
                        studentScore.setSubmittedAt(null);
                    }

                    studentScores.add(studentScore);
                }

                // 计算该考试的平均分（只计算有成绩的学生）
                if (examScoreCount > 0) {
                    examDetail.setAverageScore(
                            examScoreSum.divide(new BigDecimal(examScoreCount), 2, RoundingMode.HALF_UP)
                    );
                } else {
                    examDetail.setAverageScore(BigDecimal.ZERO);
                }

                // 按成绩排序（降序），未作答学生排最后
                studentScores.sort((s1, s2) -> {
                    if (s1.getScore() == null && s2.getScore() == null) return 0;
                    if (s1.getScore() == null) return 1;
                    if (s2.getScore() == null) return -1;
                    return s2.getScore().compareTo(s1.getScore());
                });

                // 计算排名（只计算有成绩的学生）
                int rank = 1;
                BigDecimal prevScore = null;
                int sameScoreCount = 0;

                for (StudentExamScoreVO score : studentScores) {
                    if (score.getScore() == null) {
                        // 未作答学生不参与排名
                        score.setRank(null);
                        continue;
                    }

                    // 处理排名（考虑并列情况）
                    if (prevScore != null && score.getScore().compareTo(prevScore) == 0) {
                        sameScoreCount++;
                    } else {
                        rank += sameScoreCount;
                        sameScoreCount = 1;
                    }

                    score.setRank(rank);
                    prevScore = score.getScore();
                }

                examDetail.setStudentScores(studentScores);
                examDetails.add(examDetail);
            }

            // 8. 计算总平均分（只计算有成绩的学生）
            if (totalScoreCount > 0) {
                summary.setOverallAverageScore(
                        totalScoreSum.divide(new BigDecimal(totalScoreCount), 2, RoundingMode.HALF_UP)
                );
            } else {
                summary.setOverallAverageScore(BigDecimal.ZERO);
            }

            // 9. 计算及格率
            if (totalScoreCount > 0) {
                BigDecimal rate = new BigDecimal(passCount)
                        .divide(new BigDecimal(totalScoreCount), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .setScale(2, RoundingMode.HALF_UP);
                summary.setPassRate(rate);
            } else {
                summary.setPassRate(BigDecimal.ZERO);
            }

            // 10. 设置成绩分布
            summary.setScoreA(scoreDistribution.getOrDefault(0, 0));
            summary.setScoreB(scoreDistribution.getOrDefault(1, 0));
            summary.setScoreC(scoreDistribution.getOrDefault(2, 0));
            summary.setScoreD(scoreDistribution.getOrDefault(3, 0));
            summary.setScoreF(scoreDistribution.getOrDefault(4, 0));

            // 11. 设置考试详情
            summary.setExamDetails(examDetails);

            return Result.success(summary);

        } catch (Exception e) {
            log.error("获取考试统计失败: {}", e.getMessage());
            return Result.error("获取考试统计失败");
        }
    }

    @Override
    public Result<Map<String, KnowledgePointMasteryVO>> getKnowledgePointMastery(String courseId) {
        try {
            // 1. 验证课程ID
            if (StringUtils.isBlank(courseId)) {
                log.warn("课程ID为空");
                return Result.error("课程ID不能为空");
            }

            // 2. 获取课程下的所有知识点
            List<KnowledgePoint> knowledgePoints = knowledgePointMapper.findByCourseId(courseId);
            if (knowledgePoints == null || knowledgePoints.isEmpty()) {
                log.info("课程{}下没有知识点", courseId);
                return Result.success(Collections.emptyMap());
            }

            // 3. 获取所有答题记录（包含知识点信息）
            List<StudentKnowBO> allAnswers = studentAnswerMapper.getAnswerKnowledgeFrequencyByCourseId(courseId);
            if (allAnswers == null || allAnswers.isEmpty()) {
                log.info("课程{}下没有答题记录", courseId);
                return Result.success(Collections.emptyMap());
            }

            // 4. 按知识点分组并统计（并行处理）
            Map<Integer, KnowledgePointStats> statsMap = new ConcurrentHashMap<>();

            allAnswers.parallelStream().forEach(answer -> {
                // 确保知识点ID不为空
                if (answer.getPointId() == null) {
                    log.warn("答题记录缺少知识点ID");
                    return;
                }

                KnowledgePointStats stats = statsMap.computeIfAbsent(answer.getPointId(), k ->
                        new KnowledgePointStats(
                                answer.getPointId(),
                                answer.getPointName(),
                                answer.getCourseName()
                        )
                );

                // 更新统计信息
                stats.incrementTotalAnswerCount();
                if (Boolean.TRUE.equals(answer.getIsCorrect())) {
                    stats.incrementCorrectCount();
                } else {
                    stats.incrementErrorCount(); // 记录错误次数
                }
            });

            // 5. 准备结果Map
            Map<String, KnowledgePointMasteryVO> resultMap = new ConcurrentHashMap<>();

            knowledgePoints.parallelStream().forEach(point -> {
                // 确保知识点有ID
                if (point.getPointId() == null) {
                    log.warn("知识点{}缺少ID", point.getPointName());
                    // 处理复合名称
                    handleCompositeKnowledgePoint(point.getPointName(), null, resultMap);
                    return;
                }

                KnowledgePointStats stats = statsMap.get(point.getPointId());
                // 处理知识点名称（支持单/复合名称）
                handleCompositeKnowledgePoint(point.getPointName(), stats, resultMap);
            });

            return Result.success(resultMap);

        } catch (Exception e) {
            log.error("获取知识点掌握情况失败: {}", e.getMessage(), e);
            return Result.error("获取知识点掌握情况失败");
        }
    }

    /**
     * 处理复合知识点拆分（按逗号和顿号）
     * @param pointName 原始知识点名称
     * @param stats 知识点统计数据（可能为null）
     * @param resultMap 结果Map
     */
    private void handleCompositeKnowledgePoint(String pointName,
                                               KnowledgePointStats stats,
                                               Map<String, KnowledgePointMasteryVO> resultMap) {
        // 如果名称为空直接返回
        if (StringUtils.isBlank(pointName)) {
            return;
        }

        // 按逗号和顿号拆分（兼容中英文标点）
        String[] splitNames = pointName.split("[,、]");

        for (String name : splitNames) {
            String trimmedName = name.trim();
            if (!trimmedName.isEmpty()) {
                resultMap.put(trimmedName, createMasteryVO(trimmedName, stats));
            }
        }
    }

    /**
     * 创建知识点掌握率VO对象
     */
    private KnowledgePointMasteryVO createMasteryVO(String pointName, KnowledgePointStats stats) {
        if (stats == null) {
            return new KnowledgePointMasteryVO(pointName, 0.0, 0, 0, 0);
        }

        double masteryRate = stats.getTotalAnswerCount() > 0 ?
                (double) stats.getCorrectCount() / stats.getTotalAnswerCount() : 0.0;

        return new KnowledgePointMasteryVO(
                pointName,
                masteryRate,
                stats.getErrorCount(),
                stats.getCorrectCount(),
                stats.getTotalAnswerCount()
        );
    }

    // 知识点统计信息
    private static class KnowledgePointStats {
        private final int pointId;
        private final String pointName;
        private final String courseName;
        private final AtomicInteger totalAnswerCount = new AtomicInteger(0);
        private final AtomicInteger correctCount = new AtomicInteger(0);
        private final AtomicInteger errorCount = new AtomicInteger(0);

        public KnowledgePointStats(int pointId, String pointName, String courseName) {
            this.pointId = pointId;
            this.pointName = pointName;
            this.courseName = courseName;
        }

        public void incrementTotalAnswerCount() {
            totalAnswerCount.incrementAndGet();
        }

        public void incrementCorrectCount() {
            correctCount.incrementAndGet();
        }

        public void incrementErrorCount() {
            errorCount.incrementAndGet();
        }

        public int getTotalAnswerCount() {
            return totalAnswerCount.get();
        }

        public int getCorrectCount() {
            return correctCount.get();
        }

        public int getErrorCount() {
            return errorCount.get();
        }
    }

    // 获取学生姓名
    private String getStudentName(Integer userId) {
        // 根据userId查询用户表获取姓名
        User user = userMapper.findById(userId);
        return user != null ? user.getRealName() : "未知";
    }

    // 辅助方法：筛选测试题
    private List<ChapterQuestionDetailVO> filterTestQuestions(List<ChapterQuestionDetailVO> questions) {
        return questions.stream().filter(q -> "test".equals(q.getChapterQuestionType())).collect(Collectors.toList());
    }

    // 辅助方法：设置题目权重
    private List<ChapterQuestionDetailVO> setQuestionWeights(List<ChapterQuestionDetailVO> questions) {
        questions.forEach(question -> {
            if (question.getT() == null) {
                // 根据题目类型设置默认权重
                switch (question.getQuestionType()) {
                    case "single_choice":
                    case "true_false":
                        question.setT(1); // 单选题和判断题权重为1
                        break;
                    case "multiple_choice":
                        question.setT(2); // 多选题权重为2
                        break;
                    case "fill_blank":
                        question.setT(3); // 填空题权重为3
                        break;
                    case "short_answer":
                        question.setT(5); // 简答题权重为5
                        break;
                    default:
                        question.setT(1);
                }
                log.debug("题目ID={} 设置默认权重: {}", question.getQuestionId(), question.getT());
            }
        });
        return questions;
    }
}
