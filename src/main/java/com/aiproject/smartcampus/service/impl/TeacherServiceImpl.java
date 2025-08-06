package com.aiproject.smartcampus.service.impl;

import com.aiproject.smartcampus.commons.client.Result;
import com.aiproject.smartcampus.commons.utils.UserToTypeUtils;
import com.aiproject.smartcampus.mapper.*;
import com.aiproject.smartcampus.pojo.bo.StudentWrongKnowledgeBO;
import com.aiproject.smartcampus.pojo.dto.TeacherGetSituationDTO;
import com.aiproject.smartcampus.pojo.dto.TeacherGetStudentDTO;
import com.aiproject.smartcampus.pojo.dto.TeacherQueryDTO;
import com.aiproject.smartcampus.pojo.po.Course;
import com.aiproject.smartcampus.pojo.po.Exam;
import com.aiproject.smartcampus.pojo.po.Teacher;
import com.aiproject.smartcampus.pojo.vo.ChapterQuestionDetailTeacherVO;
import com.aiproject.smartcampus.pojo.vo.ChapterQuestionDetailVO;
import com.aiproject.smartcampus.pojo.vo.ExamStudentVO;
import com.aiproject.smartcampus.service.TeacherService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
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
    public Result getAllClassInfo(String courseId) {
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
            situationDTO.setAverageScore(averageScore);
            situationDTO.setPassRate(passRate);
            situationDTO.setExcellentRate(excellentRate);
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
