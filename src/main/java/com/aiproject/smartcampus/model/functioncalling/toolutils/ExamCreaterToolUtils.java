package com.aiproject.smartcampus.model.functioncalling.toolutils;

import com.aiproject.smartcampus.mapper.CourseMapper;
import com.aiproject.smartcampus.mapper.ExamMapper;
import com.aiproject.smartcampus.mapper.KnowledgePointMapper;
import com.aiproject.smartcampus.mapper.QuestionBankMapper;
import com.aiproject.smartcampus.pojo.po.Exam;
import com.aiproject.smartcampus.pojo.po.ExamPaper;
import com.aiproject.smartcampus.pojo.po.PaperQuestion;
import com.aiproject.smartcampus.pojo.po.QuestionBank;
import com.aiproject.smartcampus.pojo.vo.ExamCreationRequestVO;
import com.aiproject.smartcampus.pojo.vo.ExamCreationResult;
import com.aiproject.smartcampus.pojo.vo.GeneratedQuestionVO;
import com.aiproject.smartcampus.pojo.vo.KnowledgePointVO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 智能生成试卷工具类 - 修复枚举映射问题
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExamCreaterToolUtils {

    private final ChatLanguageModel chatLanguageModel;
    private final ExamMapper examMapper;
    private final QuestionBankMapper questionBankMapper;
    private final KnowledgePointMapper knowledgePointMapper;
    private final ObjectMapper objectMapper;
    private final CourseMapper courseMapper;

    /**
     * 智能创建试卷主方法
     */
    @Transactional
    public ExamCreationResult createExam(String content, String courseId) {
        LocalDateTime startTime = LocalDateTime.now();

        try {
            log.info("开始智能创建试卷: courseId={}, 要求={}", courseId, content);

            // 1. 解析试卷要求
            ExamCreationRequestVO request = parseExamRequirements(content, courseId);
            log.info("解析试卷要求完成: {}", request.getExamTitle());

            // 2. 获取课程知识点信息
            List<KnowledgePointVO> knowledgePoints = getKnowledgePoints(courseId);
            log.info("获取知识点完成，数量: {}", knowledgePoints.size());

            // 3. 智能生成题目
            List<GeneratedQuestionVO> generatedQuestions = generateQuestions(request, knowledgePoints);
            log.info("生成题目完成，数量: {}", generatedQuestions.size());

            if (generatedQuestions.isEmpty()) {
                return ExamCreationResult.failure("无法生成题目，请检查课程信息或重新描述需求");
            }

            // 4. 保存题目到题库
            List<Integer> questionIds = saveQuestionsToBank(generatedQuestions, courseId);
            log.info("保存题目到题库完成，成功数量: {}", questionIds.size());

            if (questionIds.isEmpty()) {
                return ExamCreationResult.failure("保存题目到题库失败");
            }

            // 5. 创建考试和试卷
            Integer examId = createExamAndPaper(request, questionIds);
            log.info("创建考试和试卷完成: examId={}", examId);

            // 6. 生成试卷JSON
            String examPaperJson = generateExamPaperJson(examId, generatedQuestions);

            // 7. 生成创建报告
            String report = generateCreationReport(examId, request, generatedQuestions, startTime);

            log.info("智能创建试卷完成: examId={}, 题目数={}", examId, generatedQuestions.size());

            return ExamCreationResult.success(examId, examPaperJson, report);

        } catch (Exception e) {
            log.error("智能创建试卷失败: courseId={}", courseId, e);
            return ExamCreationResult.failure("创建试卷失败：" + e.getMessage());
        }
    }

    /**
     * 题目类型映射方法 - 解决枚举不匹配问题
     */
    private QuestionBank.QuestionType mapQuestionType(String aiQuestionType) {
        if (aiQuestionType == null) {
            log.warn("题目类型为空，使用默认类型: SINGLE_CHOICE");
            return QuestionBank.QuestionType.SINGLE_CHOICE;
        }

        try {
            // 首先尝试直接转换（处理已经是正确格式的情况）
            return QuestionBank.QuestionType.valueOf(aiQuestionType.toUpperCase());
        } catch (IllegalArgumentException e) {
            // 如果直接转换失败，进行映射转换
            return switch (aiQuestionType.toLowerCase()) {
                case "single_choice", "singlechoice", "single-choice" -> QuestionBank.QuestionType.SINGLE_CHOICE;
                case "multiple_choice", "multiplechoice", "multiple-choice" ->
                        QuestionBank.QuestionType.MULTIPLE_CHOICE;
                case "true_false", "truefalse", "true-false", "judgment" -> QuestionBank.QuestionType.TRUE_FALSE;
                case "fill_blank", "fillblank", "fill-blank", "fill_in_blank", "fill_in_the_blank" ->
                        QuestionBank.QuestionType.FILL_BLANK;
                case "short_answer", "shortanswer", "short-answer", "essay" -> QuestionBank.QuestionType.SHORT_ANSWER;
                default -> {
                    log.warn("未知的题目类型: {}, 使用默认类型: SINGLE_CHOICE", aiQuestionType);
                    yield QuestionBank.QuestionType.SINGLE_CHOICE;
                }
            };
        }
    }

    /**
     * 难度级别映射方法
     */
    private QuestionBank.DifficultyLevel mapDifficultyLevel(String aiDifficultyLevel) {
        if (aiDifficultyLevel == null) {
            log.warn("难度级别为空，使用默认难度: MEDIUM");
            return QuestionBank.DifficultyLevel.MEDIUM;
        }

        try {
            return QuestionBank.DifficultyLevel.valueOf(aiDifficultyLevel.toUpperCase());
        } catch (IllegalArgumentException e) {
            return switch (aiDifficultyLevel.toLowerCase()) {
                case "easy", "简单", "低" -> QuestionBank.DifficultyLevel.EASY;
                case "medium", "中等", "中" -> QuestionBank.DifficultyLevel.MEDIUM;
                case "hard", "difficult", "困难", "高" -> QuestionBank.DifficultyLevel.HARD;
                default -> {
                    log.warn("未知的难度级别: {}, 使用默认难度: MEDIUM", aiDifficultyLevel);
                    yield QuestionBank.DifficultyLevel.MEDIUM;
                }
            };
        }
    }

    /**
     * 生成试卷JSON
     */
    private String generateExamPaperJson(Integer examId, List<GeneratedQuestionVO> questions) {
        try {
            Map<String, Object> examPaper = new HashMap<>();

            // 试卷基本信息
            examPaper.put("examId", examId);
            examPaper.put("generateTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            examPaper.put("questionCount", questions.size());

            // 题目列表
            List<Map<String, Object>> questionList = new ArrayList<>();
            for (int i = 0; i < questions.size(); i++) {
                GeneratedQuestionVO question = questions.get(i);
                Map<String, Object> questionMap = new HashMap<>();

                questionMap.put("questionOrder", i + 1);
                questionMap.put("questionType", question.getQuestionType());
                questionMap.put("questionContent", question.getQuestionContent());
                questionMap.put("difficultyLevel", question.getDifficultyLevel());
                questionMap.put("scorePoints", question.getScorePoints());
                questionMap.put("knowledgePointName", question.getKnowledgePointName());
                questionMap.put("explanation", question.getExplanation());

                // 处理选项（如果有）
                if (question.getQuestionOptions() != null && !question.getQuestionOptions().isEmpty()) {
                    try {
                        Object options = objectMapper.readValue(question.getQuestionOptions(), Object.class);
                        questionMap.put("options", options);
                    } catch (Exception e) {
                        log.warn("解析题目选项失败，使用原始字符串: {}", e.getMessage());
                        questionMap.put("options", question.getQuestionOptions());
                    }
                }

                // 处理正确答案
                if (question.getCorrectAnswer() != null && !question.getCorrectAnswer().isEmpty()) {
                    try {
                        Object correctAnswer = objectMapper.readValue(question.getCorrectAnswer(), Object.class);
                        questionMap.put("correctAnswer", correctAnswer);
                    } catch (Exception e) {
                        log.warn("解析正确答案失败，使用原始字符串: {}", e.getMessage());
                        questionMap.put("correctAnswer", question.getCorrectAnswer());
                    }
                }

                questionList.add(questionMap);
            }

            examPaper.put("questions", questionList);

            // 统计信息
            Map<String, Object> statistics = generateStatistics(questions);
            examPaper.put("statistics", statistics);

            return objectMapper.writeValueAsString(examPaper);

        } catch (Exception e) {
            log.error("生成试卷JSON失败", e);
            return "{}";
        }
    }

    /**
     * 生成统计信息
     */
    private Map<String, Object> generateStatistics(List<GeneratedQuestionVO> questions) {
        Map<String, Object> stats = new HashMap<>();

        // 题型统计
        Map<String, Long> typeCount = questions.stream()
                .collect(Collectors.groupingBy(GeneratedQuestionVO::getQuestionType, Collectors.counting()));
        stats.put("questionTypeDistribution", typeCount);

        // 难度统计
        Map<String, Long> difficultyCount = questions.stream()
                .collect(Collectors.groupingBy(GeneratedQuestionVO::getDifficultyLevel, Collectors.counting()));
        stats.put("difficultyDistribution", difficultyCount);

        // 知识点统计
        Map<String, Long> knowledgePointCount = questions.stream()
                .filter(q -> q.getKnowledgePointName() != null)
                .collect(Collectors.groupingBy(GeneratedQuestionVO::getKnowledgePointName, Collectors.counting()));
        stats.put("knowledgePointDistribution", knowledgePointCount);

        // 总分统计
        BigDecimal totalScore = questions.stream()
                .map(GeneratedQuestionVO::getScorePoints)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        stats.put("totalScore", totalScore);

        return stats;
    }

    /**
     * 解析试卷要求
     */
    private ExamCreationRequestVO parseExamRequirements(String content, String courseId) {
        try {
            String prompt = buildRequirementParsePrompt(content, courseId);
            String aiResponse = chatLanguageModel.chat(prompt);
            log.info("AI解析试卷要求响应长度: {}", aiResponse != null ? aiResponse.length() : 0);

            // 解析AI响应
            String jsonPart = extractJSON(aiResponse);
            JsonNode node = objectMapper.readTree(jsonPart);

            ExamCreationRequestVO request = new ExamCreationRequestVO();
            request.setCourseId(Integer.parseInt(courseId));
            request.setExamTitle(node.get("examTitle").asText());
            request.setExamDescription(node.get("examDescription").asText());
            request.setDurationMinutes(node.get("durationMinutes").asInt());
            request.setTotalScore(new BigDecimal(node.get("totalScore").asText()));
            request.setDifficultyDistribution(parseDistribution(node.get("difficultyDistribution")));

            Map<String, Integer> questionTypeDistribution = new HashMap<>();
            node.get("questionTypeDistribution").fields().forEachRemaining(entry -> {
                questionTypeDistribution.put(entry.getKey(), entry.getValue().asInt());
            });
            request.setQuestionTypeDistribution(questionTypeDistribution);
            request.setKnowledgePointFocus(parseStringArray(node.get("knowledgePointFocus")));

            return request;

        } catch (Exception e) {
            log.error("解析试卷要求失败", e);
            // 返回默认配置
            return createDefaultRequest(courseId);
        }
    }

    /**
     * 构建需求解析提示词
     */
    private String buildRequirementParsePrompt(String content, String courseId) {
        return String.format("""
                请作为一位经验丰富的教师，分析以下试卷创建要求，并提取关键信息：
                                
                课程ID: %s
                试卷要求: %s
                                
                请分析并提取以下信息：
                1. 考试标题（如果未明确指定，请根据内容生成合适的标题）
                2. 考试描述
                3. 考试时长（分钟）
                4. 总分
                5. 难度分布（简单、中等、困难的题目比例）
                6. 题型分布（单选、多选、判断、填空、简答题的数量或比例）
                7. 重点知识点（需要重点考察的知识领域）
                                
                请以JSON格式返回分析结果：
                {
                    "examTitle": "考试标题",
                    "examDescription": "考试描述",
                    "durationMinutes": 考试时长分钟数,
                    "totalScore": 总分,
                    "difficultyDistribution": {
                        "easy": 简单题比例(0-1),
                        "medium": 中等题比例(0-1),
                        "hard": 困难题比例(0-1)
                    },
                    "questionTypeDistribution": {
                        "single_choice": 单选题数量,
                        "multiple_choice": 多选题数量,
                        "true_false": 判断题数量,
                        "fill_blank": 填空题数量,
                        "short_answer": 简答题数量
                    },
                    "knowledgePointFocus": ["重点知识点1", "重点知识点2"]
                }
                """, courseId, content);
    }

    /**
     * 获取课程知识点信息
     */
    private List<KnowledgePointVO> getKnowledgePoints(String courseId) {
        try {
            return knowledgePointMapper.getCourseKnowledgePoints(Integer.parseInt(courseId));
        } catch (Exception e) {
            log.error("获取课程知识点失败: courseId={}", courseId, e);
            return new ArrayList<>();
        }
    }

    /**
     * 智能生成题目
     */
    private List<GeneratedQuestionVO> generateQuestions(ExamCreationRequestVO request,
                                                        List<KnowledgePointVO> knowledgePoints) {
        List<GeneratedQuestionVO> allQuestions = new ArrayList<>();

        // 按题型生成题目
        for (Map.Entry<String, Integer> entry : request.getQuestionTypeDistribution().entrySet()) {
            String questionType = entry.getKey();
            Integer count = entry.getValue();

            if (count > 0) {
                log.info("开始生成{}类型题目，数量: {}", questionType, count);
                List<GeneratedQuestionVO> typeQuestions = generateQuestionsByType(
                        questionType, count, request, knowledgePoints);
                allQuestions.addAll(typeQuestions);
                log.info("完成生成{}类型题目，实际数量: {}", questionType, typeQuestions.size());
            }
        }

        return allQuestions;
    }

    /**
     * 按题型生成题目
     */
    private List<GeneratedQuestionVO> generateQuestionsByType(String questionType, Integer count,
                                                              ExamCreationRequestVO request,
                                                              List<KnowledgePointVO> knowledgePoints) {
        try {
            String prompt = buildQuestionGenerationPrompt(questionType, count, request, knowledgePoints) + "请严格按照题目类型来生成，确保题目类型和题目内容相匹配";
            String aiResponse = chatLanguageModel.chat(prompt);
            log.info("生成{}类型题目AI响应长度: {}", questionType, aiResponse != null ? aiResponse.length() : 0);

            // 解析生成的题目
            return parseGeneratedQuestions(aiResponse, questionType);

        } catch (Exception e) {
            log.error("生成{}题目失败", questionType, e);
            return new ArrayList<>();
        }
    }

    /**
     * 构建题目生成提示词
     */
    private String buildQuestionGenerationPrompt(String questionType, Integer count,
                                                 ExamCreationRequestVO request,
                                                 List<KnowledgePointVO> knowledgePoints) {
        StringBuilder knowledgePointInfo = new StringBuilder();
        for (KnowledgePointVO kp : knowledgePoints) {
            knowledgePointInfo.append(String.format("- %s (难度:%s): %s\n",
                    kp.getPointName(), kp.getDifficultyLevel(), kp.getDescription()));
        }

        String typeDesc = getQuestionTypeDescription(questionType);
        String formatExample = getQuestionFormatExample(questionType);

        return String.format("""
                        请作为一位专业的教师，为以下课程生成%s：
                                                
                        考试信息：
                        - 考试标题: %s
                        - 考试描述: %s
                        - 题目类型: %s
                        - 需要生成: %d题
                        - 重点知识点: %s
                                                
                        课程知识点信息：
                        %s
                                                
                        生成要求：
                        1. 题目内容要准确、专业，符合课程要求
                        2. 难度要合理分布，覆盖不同知识点
                        3. 选项要有迷惑性但不能误导学生
                        4. 答案要准确无误
                        5. 提供详细的答案解析
                                                
                        %s
                                                
                        请生成%d道题目，以JSON数组格式返回：
                        [
                            %s
                        ]
                        """,
                typeDesc,
                request.getExamTitle(),
                request.getExamDescription(),
                typeDesc,
                count,
                String.join(", ", request.getKnowledgePointFocus()),
                knowledgePointInfo.toString(),
                formatExample,
                count,
                getQuestionJSONTemplate(questionType));
    }

    /**
     * 获取题型描述
     */
    private String getQuestionTypeDescription(String questionType) {
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
                return "题目";
        }
    }

    /**
     * 获取题目格式示例
     */
    private String getQuestionFormatExample(String questionType) {
        switch (questionType) {
            case "single_choice":
                return """
                        单选题格式要求：
                        - 题干要清晰明确
                        - 提供4个选项（A、B、C、D）
                        - 只有一个正确答案
                        - 选项要有一定迷惑性
                        """;
            case "multiple_choice":
                return """
                        多选题格式要求：
                        - 题干要明确说明是多选题
                        - 提供4-6个选项
                        - 可以有2-4个正确答案
                        - 错误选项要有迷惑性
                        """;
            case "true_false":
                return """
                        判断题格式要求：
                        - 陈述要明确，避免模糊表达
                        - 只有对错两种答案
                        - 避免绝对化词语
                        """;
            case "fill_blank":
                return """
                        填空题格式要求：
                        - 用___表示空白处
                        - 空白处应该是关键词或核心概念
                        - 答案要唯一或有标准答案
                        """;
            case "short_answer":
                return """
                        简答题格式要求：
                        - 问题要开放但有明确要求
                        - 答案要有要点性
                        - 提供评分标准
                        """;
            default:
                return "";
        }
    }

    /**
     * 获取题目JSON模板
     */
    private String getQuestionJSONTemplate(String questionType) {
        switch (questionType) {
            case "single_choice":
            case "multiple_choice":
                return """
                        {
                            "questionContent": "题目内容",
                            "options": [
                                {"label": "A", "content": "选项A内容", "isCorrect": false},
                                {"label": "B", "content": "选项B内容", "isCorrect": true},
                                {"label": "C", "content": "选项C内容", "isCorrect": false},
                                {"label": "D", "content": "选项D内容", "isCorrect": false}
                            ],
                            "correctAnswer": ["B"],
                            "explanation": "答案解析",
                            "difficultyLevel": "medium",
                            "scorePoints": 5.0,
                            "knowledgePointName": "相关知识点名称"
                        }
                        """;
            case "true_false":
                return """
                        {
                            "questionContent": "判断题内容",
                            "options": [
                                {"label": "A", "content": "正确", "isCorrect": true},
                                {"label": "B", "content": "错误", "isCorrect": false}
                            ],
                            "correctAnswer": ["A"],
                            "explanation": "答案解析",
                            "difficultyLevel": "easy",
                            "scorePoints": 5.0,
                            "knowledgePointName": "相关知识点名称"
                        }
                        """;
            case "fill_blank":
                return """
                        {
                            "questionContent": "填空题内容，包含___空白处",
                            "correctAnswer": ["标准答案"],
                            "explanation": "答案解析",
                            "difficultyLevel": "medium",
                            "scorePoints": 5.0,
                            "knowledgePointName": "相关知识点名称"
                        }
                        """;
            case "short_answer":
                return """
                        {
                            "questionContent": "简答题问题",
                            "correctAnswer": ["参考答案要点1", "参考答案要点2"],
                            "explanation": "评分标准和参考答案",
                            "difficultyLevel": "hard",
                            "scorePoints": 5.0,
                            "knowledgePointName": "相关知识点名称"
                        }
                        """;
            default:
                return "{}";
        }
    }

    /**
     * 解析生成的题目
     */
    private List<GeneratedQuestionVO> parseGeneratedQuestions(String aiResponse, String questionType) {
        try {
            // 提取JSON数组部分
            String jsonArray = extractJSONArray(aiResponse);
            JsonNode questionsNode = objectMapper.readTree(jsonArray);

            List<GeneratedQuestionVO> questions = new ArrayList<>();
            for (JsonNode questionNode : questionsNode) {
                GeneratedQuestionVO question = new GeneratedQuestionVO();
                question.setQuestionType(questionType);
                question.setQuestionContent(questionNode.get("questionContent").asText());
                question.setDifficultyLevel(questionNode.get("difficultyLevel").asText());
                question.setScorePoints(new BigDecimal(questionNode.get("scorePoints").asText()));
                question.setExplanation(questionNode.get("explanation").asText());
                question.setKnowledgePointName(questionNode.get("knowledgePointName").asText());

                // 处理选项
                if (questionNode.has("options")) {
                    question.setQuestionOptions(questionNode.get("options").toString());
                }

                // 处理正确答案
                if (questionNode.has("correctAnswer")) {
                    question.setCorrectAnswer(questionNode.get("correctAnswer").toString());
                }

                questions.add(question);
            }

            return questions;

        } catch (Exception e) {
            log.error("解析生成的题目失败", e);
            return new ArrayList<>();
        }
    }

    /**
     * 保存题目到题库 - 修复枚举映射问题
     */
    private List<Integer> saveQuestionsToBank(List<GeneratedQuestionVO> questions, String courseId) {
        List<Integer> questionIds = new ArrayList<>();

        for (GeneratedQuestionVO question : questions) {
            try {
                // 查找或创建知识点
                Integer pointId = findOrCreateKnowledgePoint(question.getKnowledgePointName(), courseId);

                // 创建题目对象 - 使用映射方法解决枚举问题
                QuestionBank questionBank = new QuestionBank();
                questionBank.setCourseId(Integer.parseInt(courseId));
                questionBank.setPointId(pointId);

                // 使用映射方法转换题目类型
                QuestionBank.QuestionType mappedType = mapQuestionType(question.getQuestionType());
                questionBank.setQuestionType(mappedType);

                questionBank.setQuestionContent(question.getQuestionContent());
                questionBank.setQuestionOptions(question.getQuestionOptions());
                questionBank.setCorrectAnswer(question.getCorrectAnswer());
                questionBank.setExplanation(question.getExplanation());

                // 使用映射方法转换难度级别
                QuestionBank.DifficultyLevel mappedDifficulty = mapDifficultyLevel(question.getDifficultyLevel());
                questionBank.setDifficultyLevel(mappedDifficulty);

                questionBank.setScorePoints(question.getScorePoints());
                questionBank.setCreatedBy(1); // 系统创建
                questionBank.setCreatedAt(LocalDateTime.now());

                // 保存到数据库
                questionBankMapper.insert(questionBank);
                questionIds.add(questionBank.getQuestionId());

                log.debug("保存题目到题库成功: questionId={}, type={}, difficulty={}",
                        questionBank.getQuestionId(), mappedType, mappedDifficulty);

            } catch (Exception e) {
                log.error("保存题目到题库失败: questionType={}, difficultyLevel={}",
                        question.getQuestionType(), question.getDifficultyLevel(), e);
                // 继续处理其他题目，不因单个题目失败而中断整个流程
            }
        }

        return questionIds;
    }

    /**
     * 查找或创建知识点
     */
    private Integer findOrCreateKnowledgePoint(String pointName, String courseId) {
        try {
            // 先查找是否存在
            Integer pointId = knowledgePointMapper.findByNameAndCourse(pointName, Integer.parseInt(courseId));

            if (pointId != null) {
                return pointId;
            }

            // 不存在则创建新知识点
            return knowledgePointMapper.createKnowledgePoint(pointName, Integer.parseInt(courseId), "AI生成", null);

        } catch (Exception e) {
            log.error("查找或创建知识点失败: pointName={}", pointName, e);
            return null;
        }
    }

    /**
     * 创建考试和试卷
     */
    private Integer createExamAndPaper(ExamCreationRequestVO request, List<Integer> questionIds) {
        try {
            // 创建考试
            Exam exam = new Exam();
            exam.setCourseId(request.getCourseId());
            //todo
            String courseName = courseMapper.getCourseByid(request.getCourseId());

            exam.setTitle(courseName + "试卷");
            exam.setExamDate(LocalDateTime.now().plusDays(7)); // 默认一周后
            exam.setDurationMinutes(request.getDurationMinutes());
            exam.setMaxScore(request.getTotalScore());
            exam.setStatus(Exam.ExamStatus.DRAFT); // 草稿状态
            exam.setCreatedAt(LocalDateTime.now());

            examMapper.insert(exam);
            Integer examId = exam.getExamId();

            // 创建试卷
            ExamPaper paper = new ExamPaper();
            paper.setExamId(examId);
            paper.setPaperTitle(courseName + "试卷");
            paper.setTotalScore(request.getTotalScore());
            paper.setQuestionCount(questionIds.size());

            examMapper.insertExamPaper(paper);
            Integer paperId = paper.getPaperId();

            // 添加题目到试卷
            for (int i = 0; i < questionIds.size(); i++) {
                PaperQuestion paperQuestion = new PaperQuestion();
                paperQuestion.setPaperId(paperId);
                Integer id = questionIds.get(i);
                paperQuestion.setQuestionId(id);
                paperQuestion.setQuestionOrder(i + 1);
                //查询出题目分值
                BigDecimal scorePoints = questionBankMapper.selectById(id).getScorePoints();
                paperQuestion.setCustomScore(scorePoints);

                examMapper.insertPaperQuestion(paperQuestion);
            }

            log.info("创建考试和试卷成功: examId={}, paperId={}, 题目数={}",
                    examId, paperId, questionIds.size());

            return examId;

        } catch (Exception e) {
            log.error("创建考试和试卷失败", e);
            throw new RuntimeException("创建考试失败", e);
        }
    }

    /**
     * 生成创建报告
     */
    private String generateCreationReport(Integer examId, ExamCreationRequestVO request,
                                          List<GeneratedQuestionVO> questions, LocalDateTime startTime) {
        StringBuilder report = new StringBuilder();
        LocalDateTime endTime = LocalDateTime.now();

        report.append("=== 智能试卷创建报告 ===\n");
        report.append(String.format("考试ID: %d\n", examId));
        report.append(String.format("考试标题: %s\n", request.getExamTitle()));
        report.append(String.format("课程ID: %d\n", request.getCourseId()));
        report.append(String.format("考试时长: %d分钟\n", request.getDurationMinutes()));
        report.append(String.format("总分: %.1f分\n", request.getTotalScore()));
        report.append(String.format("题目总数: %d题\n", questions.size()));
        report.append(String.format("创建时间: %s\n", endTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))));
        report.append(String.format("耗时: %d秒\n", java.time.Duration.between(startTime, endTime).getSeconds()));
        report.append("\n");

        // 分题型统计
        Map<String, List<GeneratedQuestionVO>> typeGroups = questions.stream()
                .collect(Collectors.groupingBy(GeneratedQuestionVO::getQuestionType));

        report.append("=== 题型分布 ===\n");
        for (Map.Entry<String, List<GeneratedQuestionVO>> entry : typeGroups.entrySet()) {
            String type = entry.getKey();
            List<GeneratedQuestionVO> typeQuestions = entry.getValue();
            BigDecimal typeScore = typeQuestions.stream()
                    .map(GeneratedQuestionVO::getScorePoints)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            report.append(String.format("%s: %d题, %.1f分\n",
                    getQuestionTypeDescription(type), typeQuestions.size(), typeScore));
        }

        // 难度分布
        Map<String, List<GeneratedQuestionVO>> difficultyGroups = questions.stream()
                .collect(Collectors.groupingBy(GeneratedQuestionVO::getDifficultyLevel));

        report.append("\n=== 难度分布 ===\n");
        for (Map.Entry<String, List<GeneratedQuestionVO>> entry : difficultyGroups.entrySet()) {
            String difficulty = entry.getKey();
            List<GeneratedQuestionVO> difficultyQuestions = entry.getValue();

            report.append(String.format("%s: %d题 (%.1f%%)\n",
                    getDifficultyDescription(difficulty),
                    difficultyQuestions.size(),
                    difficultyQuestions.size() * 100.0 / questions.size()));
        }

        // 知识点覆盖
        Set<String> knowledgePoints = questions.stream()
                .map(GeneratedQuestionVO::getKnowledgePointName)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        report.append("\n=== 知识点覆盖 ===\n");
        report.append(String.format("涉及知识点: %d个\n", knowledgePoints.size()));
        for (String point : knowledgePoints) {
            long count = questions.stream()
                    .filter(q -> point.equals(q.getKnowledgePointName()))
                    .count();
            report.append(String.format("- %s: %d题\n", point, count));
        }

        report.append("\n=== 使用说明 ===\n");
        report.append("1. 试卷已创建为草稿状态，请在后台进行最终确认\n");
        report.append("2. 可以调整题目分值和考试时间\n");
        report.append("3. 确认无误后可发布考试\n");
        report.append("4. 建议在正式考试前进行测试\n");

        return report.toString();
    }

    /**
     * 获取难度描述
     */
    private String getDifficultyDescription(String difficulty) {
        switch (difficulty) {
            case "easy":
                return "简单";
            case "medium":
                return "中等";
            case "hard":
                return "困难";
            default:
                return difficulty;
        }
    }

    /**
     * 创建默认请求
     */
    private ExamCreationRequestVO createDefaultRequest(String courseId) {
        ExamCreationRequestVO request = new ExamCreationRequestVO();
        request.setCourseId(Integer.parseInt(courseId));
        request.setExamTitle("智能生成考试");
        request.setExamDescription("AI智能生成的考试");
        request.setDurationMinutes(90);
        request.setTotalScore(new BigDecimal("100"));

        // 默认难度分布
        Map<String, Double> difficultyDist = new HashMap<>();
        difficultyDist.put("easy", 0.3);
        difficultyDist.put("medium", 0.5);
        difficultyDist.put("hard", 0.2);
        request.setDifficultyDistribution(difficultyDist);

        // 默认题型分布
        Map<String, Integer> typeDist = new HashMap<>();
        typeDist.put("single_choice", 10);
        typeDist.put("multiple_choice", 5);
        typeDist.put("true_false", 5);
        typeDist.put("fill_blank", 3);
        typeDist.put("short_answer", 2);
        request.setQuestionTypeDistribution(typeDist);

        request.setKnowledgePointFocus(Arrays.asList("基础概念", "核心知识"));

        return request;
    }

    /**
     * 解析分布数据
     */
    private Map<String, Double> parseDistribution(JsonNode node) {
        Map<String, Double> distribution = new HashMap<>();
        if (node != null) {
            node.fields().forEachRemaining(entry -> {
                distribution.put(entry.getKey(), entry.getValue().asDouble());
            });
        }
        return distribution;
    }

    /**
     * 解析字符串数组
     */
    private List<String> parseStringArray(JsonNode node) {
        List<String> result = new ArrayList<>();
        if (node != null && node.isArray()) {
            for (JsonNode item : node) {
                result.add(item.asText());
            }
        }
        return result;
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
        return "{}";
    }

    /**
     * 从文本中提取JSON数组部分
     */
    private String extractJSONArray(String text) {
        if (text == null) {
            return "[]";
        }

        int start = text.indexOf("[");
        int end = text.lastIndexOf("]");
        if (start != -1 && end != -1 && start < end) {
            return text.substring(start, end + 1);
        }
        return "[]";
    }
}