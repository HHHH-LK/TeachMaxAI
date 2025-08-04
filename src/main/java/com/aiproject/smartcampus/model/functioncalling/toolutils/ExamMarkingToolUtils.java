package com.aiproject.smartcampus.model.functioncalling.toolutils;

import com.aiproject.smartcampus.mapper.ExamMapper;
import com.aiproject.smartcampus.mapper.KnowledgePointMapper;
import com.aiproject.smartcampus.pojo.vo.ExamQuestionVO;
import com.aiproject.smartcampus.pojo.vo.StudentExamAnswerVO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * AI自动评卷工具类
 *
 * @author lk_hhh
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ExamMarkingToolUtils {

    private final ExamMapper examMapper;
    private final KnowledgePointMapper knowledgePointMapper;
    private final ChatLanguageModel chatLanguageModel;
    private final ObjectMapper objectMapper;

    /**
     * AI自动评卷主方法 - 增加最终成绩更新
     */
    @Transactional
    public String mark(String studentId, String examId) {
        try {
            log.info("开始为学生ID: {} 的考试ID: {} 进行AI评卷", studentId, examId);

            // 1. 获取考试题目和学生答案
            List<ExamQuestionVO> allExamQuestions = examMapper.getAllExamQuestions(examId);
            List<StudentExamAnswerVO> allStudentAnswer = examMapper.getAllStudentAnwer(examId, studentId);

            if (allExamQuestions.isEmpty()) {
                return "考试题目为空，无法进行评卷";
            }

            log.info("试卷总题数: {}, 学生已作答题数: {}", allExamQuestions.size(), allStudentAnswer.size());

            // 2. 创建题目映射和学生答案映射
            Map<Integer, ExamQuestionVO> questionMap = allExamQuestions.stream()
                    .collect(Collectors.toMap(ExamQuestionVO::getQuestionId, q -> q));

            Map<Integer, StudentExamAnswerVO> answerMap = allStudentAnswer.stream()
                    .collect(Collectors.toMap(StudentExamAnswerVO::getQuestionId, a -> a));

            // 3. 对所有题目进行评分
            List<QuestionMarkingResult> markingResults = new ArrayList<>();
            BigDecimal totalScore = BigDecimal.ZERO;
            BigDecimal totalMaxScore = BigDecimal.ZERO;
            List<WrongKnowledgePoint> wrongKnowledgePoints = new ArrayList<>();

            for (ExamQuestionVO question : allExamQuestions) {
                StudentExamAnswerVO studentAnswer = answerMap.get(question.getQuestionId());
                QuestionMarkingResult result = markSingleQuestion(question, studentAnswer);
                markingResults.add(result);

                totalScore = totalScore.add(result.getScoreEarned());
                totalMaxScore = totalMaxScore.add(result.getMaxScore());

                // 收集错误知识点
                if (!result.getIsCorrect() && question.getPointId() != null) {
                    wrongKnowledgePoints.add(new WrongKnowledgePoint(
                            question.getPointId(),
                            calculateAccuracyRate(result)
                    ));
                }
            }

            // 4. 处理未作答题目
            insertUnansweredQuestions(examId, studentId, questionMap, answerMap);

            // 5. 更新所有题目的评分结果
            updateAllStudentAnswers(markingResults, examId, studentId);

            // 6. 更新考试成绩表
            updateExamScore(examId, studentId, totalScore);

            // 7. 保存学生错误知识点
            saveWrongKnowledgePoints(studentId, wrongKnowledgePoints);

            // 8. 更新课程最终成绩
            updateCourseFinalGrade(examId, studentId);

            // 9. 生成评卷报告
            String report = generateMarkingReport(examId, studentId, markingResults, totalScore, totalMaxScore, wrongKnowledgePoints);

            log.info("学生ID: {} 的考试ID: {} AI评卷完成，总分: {}/{}，已作答: {}/{}题",
                    studentId, examId, totalScore, totalMaxScore, allStudentAnswer.size(), allExamQuestions.size());
            return report;

        } catch (Exception e) {
            log.error("AI评卷过程中发生错误", e);
            return "评卷失败：" + e.getMessage();
        }
    }

    /**
     * 更新课程最终成绩到course_enrollments表
     */
    private void updateCourseFinalGrade(String examId, String studentId) {
        try {
            // 1. 获取课程ID
            Integer courseId = examMapper.getCourseIdByExamId(Integer.parseInt(examId));
            if (courseId == null) {
                log.warn("未找到考试对应的课程ID: examId={}", examId);
                return;
            }

            // 2. 计算最终成绩策略
            BigDecimal finalGrade = calculateFinalGradeStrategy(courseId, Integer.parseInt(studentId));

            // 3. 更新course_enrollments表
            int updateCount = examMapper.updateStudentFinalGrade(courseId, Integer.parseInt(studentId), finalGrade);

            if (updateCount > 0) {
                log.info("更新课程最终成绩成功: courseId={}, studentId={}, finalGrade={}",
                        courseId, studentId, finalGrade);
            } else {
                log.warn("更新课程最终成绩失败，未找到选课记录: courseId={}, studentId={}",
                        courseId, studentId);
            }

        } catch (Exception e) {
            log.error("更新课程最终成绩失败: examId={}, studentId={}", examId, studentId, e);
        }
    }

    /**
     * 计算最终成绩的策略
     * 可以根据业务需求调整计算方法
     */
    private BigDecimal calculateFinalGradeStrategy(Integer courseId, Integer studentId) {
        try {
            // 策略1: 使用平均考试成绩作为最终成绩
            BigDecimal averageScore = examMapper.calculateStudentAverageScore(courseId, studentId);

            if (averageScore != null) {
                log.debug("使用平均成绩作为最终成绩: courseId={}, studentId={}, averageScore={}",
                        courseId, studentId, averageScore);
                return averageScore;
            }

            // 策略2: 如果没有考试成绩，返回0
            log.debug("未找到考试成绩，最终成绩设为0: courseId={}, studentId={}", courseId, studentId);
            return BigDecimal.ZERO;

        } catch (Exception e) {
            log.error("计算最终成绩时发生错误: courseId={}, studentId={}", courseId, studentId, e);
            return BigDecimal.ZERO;
        }
    }

    /**
     * 插入未作答题目的记录到student_answers表
     */
    private void insertUnansweredQuestions(String examId, String studentId,
                                           Map<Integer, ExamQuestionVO> questionMap,
                                           Map<Integer, StudentExamAnswerVO> answerMap) {
        List<Integer> unansweredQuestionIds = questionMap.keySet().stream()
                .filter(questionId -> !answerMap.containsKey(questionId))
                .collect(Collectors.toList());

        if (unansweredQuestionIds.isEmpty()) {
            log.info("所有题目都已作答，无需插入记录");
            return;
        }

        log.info("发现{}道未作答题目，准备插入记录: {}", unansweredQuestionIds.size(), unansweredQuestionIds);

        for (Integer questionId : unansweredQuestionIds) {
            try {
                examMapper.insertUnansweredQuestion(
                        Integer.parseInt(examId),
                        Integer.parseInt(studentId),
                        questionId,
                        "", // 空的学生答案
                        false, // 未作答算错误
                        BigDecimal.ZERO // 0分
                );
                log.debug("插入未作答题目记录成功: questionId={}", questionId);
            } catch (Exception e) {
                log.error("插入未作答题目记录失败: questionId={}", questionId, e);
            }
        }
    }

    /**
     * 更新所有学生答案（包括未作答的）
     */
    private void updateAllStudentAnswers(List<QuestionMarkingResult> results, String examId, String studentId) {

        int successCount = 0;
        int failCount = 0;

        for (QuestionMarkingResult result : results) {
            try {
                int updateCount = examMapper.updateStudentAnswer(
                        Integer.parseInt(examId),
                        Integer.parseInt(studentId),
                        result.getQuestionId(),
                        result.getIsCorrect(),
                        result.getScoreEarned()
                );

                if (updateCount > 0) {
                    successCount++;
                    log.debug("更新学生答案成功: questionId={}, isCorrect={}, score={}",
                            result.getQuestionId(), result.getIsCorrect(), result.getScoreEarned());
                } else {
                    failCount++;
                    log.warn("更新学生答案失败，未找到记录: examId={}, studentId={}, questionId={}",
                            examId, studentId, result.getQuestionId());
                }
            } catch (Exception e) {
                failCount++;
                log.error("更新学生答案异常: examId={}, studentId={}, questionId={}",
                        examId, studentId, result.getQuestionId(), e);
            }
        }

        log.info("学生答案更新完成: 成功{}题, 失败{}题", successCount, failCount);
    }

    /**
     * 对单个题目进行AI评分 - 修改版本
     * 使用StudentExamAnswerVO中的maxScore作为题目满分
     */
    private QuestionMarkingResult markSingleQuestion(ExamQuestionVO question, StudentExamAnswerVO studentAnswer) {
        QuestionMarkingResult result = new QuestionMarkingResult();
        result.setQuestionId(question.getQuestionId());
        result.setQuestionType(question.getQuestionType());
        result.setPointId(question.getPointId());

        // 关键修改：优先使用StudentExamAnswerVO中的maxScore
        BigDecimal maxScore = getQuestionMaxScore(question, studentAnswer);
        result.setMaxScore(maxScore);

        // 如果学生没有作答
        if (studentAnswer == null || studentAnswer.getStudentAnswer() == null ||
                studentAnswer.getStudentAnswer().trim().isEmpty()) {
            result.setScoreEarned(BigDecimal.ZERO);
            result.setIsCorrect(false);
            result.setAiFeedback("未作答");
            return result;
        }

        result.setStudentAnswer(studentAnswer.getStudentAnswer());

        try {
            switch (question.getQuestionType()) {
                case "single_choice":
                case "multiple_choice":
                case "true_false":
                    return markObjectiveQuestion(question, studentAnswer, result, maxScore);
                case "fill_blank":
                    return markFillBlankQuestion(question, studentAnswer, result, maxScore);
                case "short_answer":
                    return markSubjectiveQuestion(question, studentAnswer, result, maxScore);
                default:
                    result.setScoreEarned(BigDecimal.ZERO);
                    result.setIsCorrect(false);
                    result.setAiFeedback("未知题目类型");
                    return result;
            }
        } catch (Exception e) {
            log.error("评分单个题目时发生错误: {}", e.getMessage());
            result.setScoreEarned(BigDecimal.ZERO);
            result.setIsCorrect(false);
            result.setAiFeedback("评分过程中发生错误");
            return result;
        }
    }

    /**
     * 获取题目最大分值的优先级策略
     */
    private BigDecimal getQuestionMaxScore(ExamQuestionVO question, StudentExamAnswerVO studentAnswer) {
        // 优先级1: 使用StudentExamAnswerVO中的maxScore（这是试卷中实际设定的分值）
        if (studentAnswer != null && studentAnswer.getMaxScore() != null &&
                studentAnswer.getMaxScore().compareTo(BigDecimal.ZERO) > 0) {
            return studentAnswer.getMaxScore();
        }

        // 优先级2: 使用ExamQuestionVO中的自定义分值
        if (question.getCustomScore() != null && question.getCustomScore().compareTo(BigDecimal.ZERO) > 0) {
            return question.getCustomScore();
        }

        // 优先级3: 使用题库中的默认分值
        if (question.getScorePoints() != null && question.getScorePoints().compareTo(BigDecimal.ZERO) > 0) {
            return question.getScorePoints();
        }

        // 默认分值
        return BigDecimal.ONE;
    }

    /**
     * 评判客观题（单选、多选、判断题）- 修改版本
     */
    private QuestionMarkingResult markObjectiveQuestion(ExamQuestionVO question,
                                                        StudentExamAnswerVO studentAnswer,
                                                        QuestionMarkingResult result,
                                                        BigDecimal maxScore) {
        try {
            String correctAnswer = question.getCorrectAnswer();
            String studentAnswerText = studentAnswer.getStudentAnswer();

            boolean isCorrect = false;

            // 处理不同格式的答案
            if (isJsonFormat(correctAnswer) && isJsonFormat(studentAnswerText)) {
                // JSON格式比较
                JsonNode correctNode = objectMapper.readTree(correctAnswer);
                JsonNode studentNode = objectMapper.readTree(studentAnswerText);
                isCorrect = compareAnswers(correctNode, studentNode, question.getQuestionType());
            } else {
                // 字符串格式比较
                isCorrect = compareStringAnswers(correctAnswer, studentAnswerText, question.getQuestionType());
            }

            result.setIsCorrect(isCorrect);
            // 使用传入的maxScore计算得分
            result.setScoreEarned(isCorrect ? maxScore : BigDecimal.ZERO);
            result.setAiFeedback(isCorrect ? "答案正确" : "答案错误，正确答案为：" + formatAnswer(correctAnswer));

            return result;
        } catch (Exception e) {
            log.error("评判客观题时发生错误: {}", e.getMessage());
            result.setIsCorrect(false);
            result.setScoreEarned(BigDecimal.ZERO);
            result.setAiFeedback("答案格式错误");
            return result;
        }
    }

    /**
     * 评判填空题 - 修改版本
     */
    private QuestionMarkingResult markFillBlankQuestion(ExamQuestionVO question,
                                                        StudentExamAnswerVO studentAnswer,
                                                        QuestionMarkingResult result,
                                                        BigDecimal maxScore) {
        try {
            String correctAnswer = question.getCorrectAnswer();
            String studentAnswerText = studentAnswer.getStudentAnswer();

            // 构建AI评分提示词
            String prompt = buildFillBlankPrompt(question, studentAnswerText, correctAnswer, maxScore);

            // 调用AI模型进行评分
            String aiResponse = chatLanguageModel.chat(prompt);

            // 解析AI评分结果
            AIMarkingResponse aiResult = parseAIResponse(aiResponse);

            result.setIsCorrect(aiResult.isCorrect());
            // 使用传入的maxScore计算得分
            result.setScoreEarned(aiResult.getScore().multiply(maxScore));
            result.setAiFeedback(aiResult.getFeedback());

            return result;
        } catch (Exception e) {
            log.error("AI评判填空题时发生错误: {}", e.getMessage());
            // 降级到简单字符串比较
            boolean isCorrect = isAnswerSimilar(question.getCorrectAnswer(), studentAnswer.getStudentAnswer());
            result.setIsCorrect(isCorrect);
            result.setScoreEarned(isCorrect ? maxScore : BigDecimal.ZERO);
            result.setAiFeedback(isCorrect ? "答案正确" : "答案不匹配，请检查");
            return result;
        }
    }

    /**
     * 评判主观题（简答题）- 修改版本
     */
    private QuestionMarkingResult markSubjectiveQuestion(ExamQuestionVO question,
                                                         StudentExamAnswerVO studentAnswer,
                                                         QuestionMarkingResult result,
                                                         BigDecimal maxScore) {
        try {
            String correctAnswer = question.getCorrectAnswer();
            String studentAnswerText = studentAnswer.getStudentAnswer();

            // 构建AI评分提示词
            String prompt = buildSubjectivePrompt(question, studentAnswerText, correctAnswer, maxScore);

            // 调用AI模型进行评分
            String aiResponse = chatLanguageModel.chat(prompt);

            // 解析AI评分结果
            AIMarkingResponse aiResult = parseAIResponse(aiResponse);

            result.setIsCorrect(aiResult.getScore().compareTo(new BigDecimal("0.6")) >= 0);
            // 使用传入的maxScore计算得分
            result.setScoreEarned(aiResult.getScore().multiply(maxScore));
            result.setAiFeedback(aiResult.getFeedback());

            return result;
        } catch (Exception e) {
            log.error("AI评判主观题时发生错误: {}", e.getMessage());
            // 降级到基础评分
            BigDecimal basicScore = calculateBasicSubjectiveScore(question.getCorrectAnswer(), studentAnswer.getStudentAnswer());
            result.setIsCorrect(basicScore.compareTo(new BigDecimal("0.6")) >= 0);
            result.setScoreEarned(basicScore.multiply(maxScore));
            result.setAiFeedback("AI评分异常，采用基础评分规则");
            return result;
        }
    }

    /**
     * 判断是否为JSON格式
     */
    private boolean isJsonFormat(String text) {
        if (text == null || text.trim().isEmpty()) {
            return false;
        }
        try {
            objectMapper.readTree(text);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 字符串格式答案比较
     */
    private boolean compareStringAnswers(String correct, String student, String questionType) {
        if (correct == null || student == null) {
            return false;
        }

        // 标准化处理
        String normalizedCorrect = normalizeAnswer(correct);
        String normalizedStudent = normalizeAnswer(student);

        if ("multiple_choice".equals(questionType)) {
            // 多选题可能有多个答案，用逗号分隔
            Set<String> correctSet = Arrays.stream(normalizedCorrect.split(","))
                    .map(String::trim)
                    .collect(Collectors.toSet());
            Set<String> studentSet = Arrays.stream(normalizedStudent.split(","))
                    .map(String::trim)
                    .collect(Collectors.toSet());
            return correctSet.equals(studentSet);
        } else {
            // 单选题和判断题直接比较
            return normalizedCorrect.equals(normalizedStudent);
        }
    }

    /**
     * 答案标准化处理
     */
    private String normalizeAnswer(String answer) {
        if (answer == null) {
            return "";
        }
        return answer.trim().toUpperCase().replaceAll("\\s+", "");
    }

    /**
     * 构建填空题AI评分提示词 - 修改版本
     */
    private String buildFillBlankPrompt(ExamQuestionVO question, String studentAnswer, String correctAnswer, BigDecimal maxScore) {
        return String.format("""
                        请作为一位严格的老师，对以下填空题进行评分：
                        
                        题目内容：%s
                        标准答案：%s
                        学生答案：%s
                        知识点：%s
                        题目满分：%.1f分
                        
                        评分要求：
                        1. 完全正确得1分，部分正确得0.5分，完全错误得0分
                        2. 考虑答案的语义相似性，不仅仅是字面匹配
                        3. 忽略标点符号和空格的差异
                        4. 数字答案要求精确匹配
                        5. 专业术语需要准确
                        
                        评分说明：
                        - 返回的score是0-1之间的比例，最终得分将乘以题目满分%.1f分
                        - 例如：score=0.8表示得分为%.1f分
                        
                        请以JSON格式返回评分结果：
                        {
                            "score": 评分(0-1之间的小数),
                            "isCorrect": 是否基本正确(true/false),
                            "feedback": "详细的评分说明和建议"
                        }
                        """, question.getQuestionContent(), correctAnswer, studentAnswer,
                question.getPointName() != null ? question.getPointName() : "未指定",
                maxScore, maxScore, maxScore.multiply(new BigDecimal("0.8")));
    }

    /**
     * 构建主观题AI评分提示词 - 修改版本
     */
    private String buildSubjectivePrompt(ExamQuestionVO question, String studentAnswer, String correctAnswer, BigDecimal maxScore) {
        return String.format("""
                        请作为一位资深的老师，对以下简答题进行评分：
                        
                        题目内容：%s
                        参考答案：%s
                        学生答案：%s
                        知识点：%s
                        难度等级：%s
                        题目满分：%.1f分
                        
                        评分标准：
                        1. 答案要点完整性（40%%）
                        2. 逻辑表达清晰性（30%%）
                        3. 专业术语准确性（20%%）
                        4. 举例或论证合理性（10%%）
                        
                        评分要求：
                        - 满分为1.0，最低分为0.0
                        - 考虑答案的深度和广度
                        - 即使表达方式不同，但意思正确的给予相应分数
                        - 鼓励创新思维，合理的扩展给予加分
                        - 字数过少或答非所问要扣分
                        
                        评分说明：
                        - 返回的score是0-1之间的比例，最终得分将乘以题目满分%.1f分
                        - score>=0.6认为及格，score>=0.8认为优秀
                        
                        请以JSON格式返回评分结果：
                        {
                            "score": 评分(0-1之间的小数),
                            "isCorrect": 是否达到及格标准(score>=0.6),
                            "feedback": "详细的评分说明，包括得分点、扣分点和改进建议"
                        }
                        """, question.getQuestionContent(), correctAnswer, studentAnswer,
                question.getPointName() != null ? question.getPointName() : "未指定",
                question.getDifficultyLevelDesc(), maxScore, maxScore);
    }

    /**
     * 解析AI评分响应
     */
    private AIMarkingResponse parseAIResponse(String aiResponse) {
        try {
            // 提取JSON部分
            String jsonPart = extractJSON(aiResponse);
            JsonNode node = objectMapper.readTree(jsonPart);

            AIMarkingResponse response = new AIMarkingResponse();
            response.setScore(new BigDecimal(node.get("score").asText()));
            response.setCorrect(node.get("isCorrect").asBoolean());
            response.setFeedback(node.get("feedback").asText());

            // 验证评分范围
            if (response.getScore().compareTo(BigDecimal.ZERO) < 0) {
                response.setScore(BigDecimal.ZERO);
            }
            if (response.getScore().compareTo(BigDecimal.ONE) > 0) {
                response.setScore(BigDecimal.ONE);
            }

            return response;
        } catch (Exception e) {
            log.error("解析AI响应时发生错误: {}", e.getMessage());
            // 返回默认结果
            AIMarkingResponse response = new AIMarkingResponse();
            response.setScore(BigDecimal.ZERO);
            response.setCorrect(false);
            response.setFeedback("AI响应解析失败，建议人工复核");
            return response;
        }
    }

    /**
     * 从文本中提取JSON部分
     */
    private String extractJSON(String text) {
        if (text == null) {
            return "{}";
        }

        int start = text.indexOf("{");
        int end = text.lastIndexOf("}");
        if (start != -1 && end != -1 && start < end) {
            return text.substring(start, end + 1);
        }

        // 如果没有找到JSON，尝试构造一个默认响应
        return "{\"score\": 0, \"isCorrect\": false, \"feedback\": \"AI响应格式异常\"}";
    }

    /**
     * 比较客观题答案
     */
    private boolean compareAnswers(JsonNode correct, JsonNode student, String questionType) {
        if ("multiple_choice".equals(questionType)) {
            // 多选题需要完全匹配所有选项
            return correct.equals(student);
        } else {
            // 单选题和判断题直接比较
            return correct.equals(student);
        }
    }

    /**
     * 格式化答案用于显示
     */
    private String formatAnswer(String answer) {
        if (answer == null) {
            return "";
        }

        if (isJsonFormat(answer)) {
            try {
                JsonNode node = objectMapper.readTree(answer);
                if (node.isArray()) {
                    List<String> answers = new ArrayList<>();
                    for (JsonNode item : node) {
                        answers.add(item.asText());
                    }
                    return String.join(", ", answers);
                } else {
                    return node.asText();
                }
            } catch (Exception e) {
                return answer;
            }
        } else {
            return answer;
        }
    }

    /**
     * 简单答案相似性判断
     */
    private boolean isAnswerSimilar(String correct, String student) {
        if (correct == null || student == null) {
            return false;
        }

        String normalizedCorrect = normalizeAnswer(correct);
        String normalizedStudent = normalizeAnswer(student);

        // 完全匹配
        if (normalizedCorrect.equals(normalizedStudent)) {
            return true;
        }

        // 包含关系判断（适用于填空题）
        return normalizedCorrect.contains(normalizedStudent) || normalizedStudent.contains(normalizedCorrect);
    }

    /**
     * 基础主观题评分
     */
    private BigDecimal calculateBasicSubjectiveScore(String correctAnswer, String studentAnswer) {
        if (studentAnswer == null || studentAnswer.trim().isEmpty()) {
            return BigDecimal.ZERO;
        }

        if (correctAnswer == null || correctAnswer.trim().isEmpty()) {
            return new BigDecimal("0.5"); // 没有标准答案时给基础分
        }

        // 基于长度和关键词匹配的简单评分
        String[] correctWords = correctAnswer.toLowerCase().split("\\s+");
        String[] studentWords = studentAnswer.toLowerCase().split("\\s+");

        long matchCount = Arrays.stream(correctWords)
                .filter(word -> Arrays.asList(studentWords).contains(word))
                .count();

        double similarity = (double) matchCount / correctWords.length;

        if (similarity >= 0.8) {
            return new BigDecimal("0.9");
        }
        if (similarity >= 0.6) {
            return new BigDecimal("0.7");
        }
        if (similarity >= 0.4) {
            return new BigDecimal("0.5");
        }
        if (similarity >= 0.2) {
            return new BigDecimal("0.3");
        }
        return new BigDecimal("0.1");
    }

    /**
     * 计算准确率（用于知识点掌握度）
     */
    private BigDecimal calculateAccuracyRate(QuestionMarkingResult result) {
        if (result.getMaxScore().compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return result.getScoreEarned().divide(result.getMaxScore(), 4, RoundingMode.HALF_UP);
    }

    /**
     * 保存学生错误知识点
     */
    private void saveWrongKnowledgePoints(String studentId, List<WrongKnowledgePoint> wrongPoints) {
        for (WrongKnowledgePoint point : wrongPoints) {
            try {
                // 根据准确率确定掌握程度
                String masteryLevel = determineMasteryLevel(point.getAccuracyRate());

                // 更新或插入学生知识掌握表
                knowledgePointMapper.updateStudentKnowledgeMastery(
                        Integer.parseInt(studentId),
                        point.getPointId(),
                        masteryLevel, // 掌握程度
                        point.getAccuracyRate(),
                        1 // 练习次数+1
                );
            } catch (Exception e) {
                log.error("保存错误知识点失败: studentId={}, pointId={}", studentId, point.getPointId(), e);
            }
        }
    }

    /**
     * 根据准确率确定掌握程度
     */
    private String determineMasteryLevel(BigDecimal accuracyRate) {
        if (accuracyRate == null) {
            return "not_learned";
        }

        if (accuracyRate.compareTo(new BigDecimal("0.8")) >= 0) {
            return "mastered";      // 准确率 >= 80% 为已掌握
        } else if (accuracyRate.compareTo(new BigDecimal("0.6")) >= 0) {
            return "learning";      // 准确率 >= 60% 为学习中
        } else {
            return "not_learned";   // 准确率 < 60% 为未掌握
        }
    }

    /**
     * 更新学生答案表
     */
    private void updateStudentAnswers(List<QuestionMarkingResult> results, String examId, String studentId) {
        int successCount = 0;
        int failCount = 0;

        for (QuestionMarkingResult result : results) {
            try {
                int updateCount = examMapper.updateStudentAnswer(
                        Integer.parseInt(examId),
                        Integer.parseInt(studentId),
                        result.getQuestionId(),
                        result.getIsCorrect(),
                        result.getScoreEarned()
                );

                if (updateCount > 0) {
                    successCount++;
                } else {
                    failCount++;
                    log.warn("更新学生答案失败，未找到记录: examId={}, studentId={}, questionId={}",
                            examId, studentId, result.getQuestionId());
                }
            } catch (Exception e) {
                failCount++;
                log.error("更新学生答案失败: examId={}, studentId={}, questionId={}",
                        examId, studentId, result.getQuestionId(), e);
            }
        }

        log.info("学生答案更新完成: 成功{}题, 失败{}题", successCount, failCount);
    }

    /**
     * 更新考试成绩表
     */
    private void updateExamScore(String examId, String studentId, BigDecimal totalScore) {
        try {
            int updateCount = examMapper.updateExamScore(
                    Integer.parseInt(examId),
                    Integer.parseInt(studentId),
                    totalScore,
                    LocalDateTime.now()
            );

            if (updateCount > 0) {
                log.info("更新考试成绩成功: examId={}, studentId={}, score={}",
                        examId, studentId, totalScore);
            } else {
                log.warn("更新考试成绩失败，未找到记录: examId={}, studentId={}", examId, studentId);
            }
        } catch (Exception e) {
            log.error("更新考试成绩失败: examId={}, studentId={}", examId, studentId, e);
        }
    }

    /**
     * 生成评卷报告 - 完整版本
     */
    private String generateMarkingReport(String examId, String studentId,
                                         List<QuestionMarkingResult> results,
                                         BigDecimal totalScore, BigDecimal totalMaxScore,
                                         List<WrongKnowledgePoint> wrongPoints) {
        StringBuilder report = new StringBuilder();

        // 基本信息
        BigDecimal percentage = totalMaxScore.compareTo(BigDecimal.ZERO) > 0 ?
                totalScore.divide(totalMaxScore, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100")) :
                BigDecimal.ZERO;

        // 统计作答情况
        long answeredCount = results.stream()
                .filter(r -> r.getStudentAnswer() != null && !r.getStudentAnswer().trim().isEmpty())
                .count();
        long unansweredCount = results.size() - answeredCount;

        long correctCount = results.stream()
                .filter(r -> Boolean.TRUE.equals(r.getIsCorrect()))
                .count();

        long wrongCount = results.stream()
                .filter(r -> Boolean.FALSE.equals(r.getIsCorrect()) &&
                        r.getStudentAnswer() != null && !r.getStudentAnswer().trim().isEmpty())
                .count();

        report.append("=== AI自动评卷报告 ===\n");
        report.append(String.format("考试ID: %s\n", examId));
        report.append(String.format("学生ID: %s\n", studentId));
        report.append(String.format("总分: %.2f/%.2f (%.2f%%)\n", totalScore, totalMaxScore, percentage));
        report.append(String.format("题目总数: %d题\n", results.size()));
        report.append(String.format("已作答: %d题 (正确: %d, 错误: %d)\n", answeredCount, correctCount, wrongCount));
        report.append(String.format("未作答: %d题 (计0分)\n", unansweredCount));
        report.append(String.format("总正确率: %.2f%%\n", results.size() > 0 ? (correctCount * 100.0 / results.size()) : 0));
        report.append(String.format("评卷时间: %s\n", LocalDateTime.now()));
        report.append("\n");

        // 分题型统计
        Map<String, List<QuestionMarkingResult>> typeGroups = results.stream()
                .collect(Collectors.groupingBy(QuestionMarkingResult::getQuestionType));

        report.append("=== 分题型得分情况 ===\n");
        for (Map.Entry<String, List<QuestionMarkingResult>> entry : typeGroups.entrySet()) {
            String type = entry.getKey();
            List<QuestionMarkingResult> typeResults = entry.getValue();

            BigDecimal typeScore = typeResults.stream()
                    .map(QuestionMarkingResult::getScoreEarned)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal typeMaxScore = typeResults.stream()
                    .map(QuestionMarkingResult::getMaxScore)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            long typeCorrectCount = typeResults.stream()
                    .filter(r -> Boolean.TRUE.equals(r.getIsCorrect()))
                    .count();
            long typeAnsweredCount = typeResults.stream()
                    .filter(r -> r.getStudentAnswer() != null && !r.getStudentAnswer().trim().isEmpty())
                    .count();
            long typeUnansweredCount = typeResults.size() - typeAnsweredCount;

            report.append(String.format("%s: %.2f/%.2f (%.1f%%)\n",
                    getQuestionTypeDesc(type), typeScore, typeMaxScore,
                    typeMaxScore.compareTo(BigDecimal.ZERO) > 0 ?
                            typeScore.divide(typeMaxScore, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100")).doubleValue() : 0));
            report.append(String.format("  - 已答: %d题 (正确: %d), 未答: %d题\n",
                    typeAnsweredCount, typeCorrectCount, typeUnansweredCount));
        }

        // 错误知识点统计
        if (!wrongPoints.isEmpty()) {
            report.append("\n=== 需要加强的知识点 ===\n");
            Map<Integer, List<WrongKnowledgePoint>> pointGroups = wrongPoints.stream()
                    .collect(Collectors.groupingBy(WrongKnowledgePoint::getPointId));

            for (Map.Entry<Integer, List<WrongKnowledgePoint>> entry : pointGroups.entrySet()) {
                Integer pointId = entry.getKey();
                List<WrongKnowledgePoint> pointList = entry.getValue();
                BigDecimal avgAccuracy = pointList.stream()
                        .map(WrongKnowledgePoint::getAccuracyRate)
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                        .divide(new BigDecimal(pointList.size()), 4, RoundingMode.HALF_UP);

                report.append(String.format("知识点ID: %d，平均掌握度: %.2f%%，错误次数: %d\n",
                        pointId, avgAccuracy.multiply(new BigDecimal("100")), pointList.size()));
            }
        }

        report.append("\n=== 详细评分情况 ===\n");
        // 按题目ID排序
        results.sort(Comparator.comparing(QuestionMarkingResult::getQuestionId));

        for (int i = 0; i < results.size(); i++) {
            QuestionMarkingResult result = results.get(i);
            String status = "";
            if (result.getStudentAnswer() == null || result.getStudentAnswer().trim().isEmpty()) {
                status = "❌未答";
            } else if (Boolean.TRUE.equals(result.getIsCorrect())) {
                status = "✅正确";
            } else {
                status = "❌错误";
            }

            report.append(String.format("第%d题(ID:%d,%s): %.2f/%.2f %s\n",
                    i + 1,
                    result.getQuestionId(),
                    getQuestionTypeDesc(result.getQuestionType()),
                    result.getScoreEarned(),
                    result.getMaxScore(),
                    status));

            if (result.getAiFeedback() != null && !result.getAiFeedback().isEmpty() &&
                    !"未作答".equals(result.getAiFeedback())) {
                report.append(String.format("  反馈: %s\n", result.getAiFeedback()));
            }
        }

        return report.toString();
    }

    private String getQuestionTypeDesc(String questionType) {
        switch (questionType) {
            case "single_choice":
                return "单选题";
            case "multiple_choice":
                return "多选题";
            case "true_false":
                return "判断题";
            case "fill_blank":
                return "填空题";
            case "short_answer":
                return "简答题";
            default:
                return questionType;
        }
    }

    /**
     * 单题评分结果内部类
     */
    @lombok.Data
    public static class QuestionMarkingResult {
        private Integer questionId;
        private String questionType;
        private String studentAnswer;
        private Boolean isCorrect;
        private BigDecimal scoreEarned;
        private BigDecimal maxScore;
        private String aiFeedback;
        private Integer pointId; // 知识点ID
    }

    /**
     * AI评分响应内部类
     */
    @lombok.Data
    public static class AIMarkingResponse {
        private BigDecimal score;
        private boolean correct;
        private String feedback;
    }

    /**
     * 错误知识点内部类
     */
    @lombok.Data
    @lombok.AllArgsConstructor
    public static class WrongKnowledgePoint {
        private Integer pointId;
        private BigDecimal accuracyRate;
    }
}