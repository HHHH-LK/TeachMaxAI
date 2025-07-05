package com.aiproject.smartcampus.model.rag;

import com.aiproject.smartcampus.pojo.bo.classprase.Course;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.parser.apache.tika.ApacheTikaDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentByParagraphSplitter;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * @program: SmartCampus
 * @description: 优化后的RAG文档处理器，提升检索精确度和课程信息完整性
 * @author: lk
 * @create: 2025-05-17 16:52
 **/

@Slf4j
@Component
@RequiredArgsConstructor
public class FileloadFunction {

    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore embeddingStore;
    private final ChatLanguageModel chatLanguageModel;
    private final LocalTokenizerFuncation tokenizer;

    // 配置参数
    private static final double COURSE_TABLE_MIN_SCORE = 0.6;
    private static final int SUMMARY_TOKEN_SIZE = 100;
    private static final int CHUNK_SIZE = 700;
    private static final int OVERLAP_SIZE = 100;
    private static final int FINE_CHUNK_SIZE = 200;
    private static final int MAX_RETRY_COUNT = 3; // 最大重试次数

    private final String extractionPrompt = """
            系统提示（System / Assistant 身份设定）：
            你是⼀名高效且严谨的教务助理 AI，专⻔用于解析高校课程表并输出结构化数据。你的目标是：
            1. 识别 Markdown 格式表格中的所有课程信息；
            2. 将每门课按"天-时段-节次"拆分为单条记录；
            3. 严格输出 JSON 数组，不要包含多余文字。
                    
            用户输入（User 提供）：
            以下是一个 Markdown 格式的课程表，请用它来提取课程信息：
            {markdown_table}
                    
            任务说明（Prompt Body）：
            请解析上面提供的 Markdown 表格，按行——即每个"星期 + 时间段 + 节次 + 课程"——生成一条 JSON 记录。输出应满足以下要求：
                    
            1. 输出格式
               - 顶层应为一个 JSON 数组 `[]`，每个元素都是一个 JSON 对象 `{}`。
               - 不要输出任何解释说明或额外字段。
                    
            2. 每条记录字段（请确保所有字段都有值，不能为空）
               - `weekday`：星期几，格式 "星期一" 至 "星期日"。
               - `time_slot`：时间段，取值 "上午"、"下午" 或 "晚上"。
               - `period`：节次，如 "1-2 节"、"3-4 节"……
               - `course_name`：课程名称，不包含后缀的星号或圈号。
               - `course_type`：课程类型，取值 `"理论"`、`"实验"` 或 `"实践"`。
               - `weeks`：上课周次，如 "1-10 周"、"双周"、"14 周"。
               - `campus`：校区名称，如 "校本部"。
               - `location`：具体场地，如 "管理楼305"；若为"未排地点"，填 `null`。
               - `teacher`：授课教师姓名。
               - `class_id`：教学班编号，如 "0007" 或 "0003B"。
               - `students`：教学班组成学号列表，数组形式，如 `["2306804","2306805"]`。
               - `assessment`：考核方式，如 "考试" 或 "考查"。
               - `weekly_hours`：每周学时，数字类型。
               - `credits`：学分，数字类型。
                    
            3. 合并单元格处理
               - 对于 Markdown 中 `rowspan` 或 `colspan` 合并的单元格，需将该单元格内容分别复制到所有被合并的行/列中，确保每条记录都能单独完整地描述一节课。
                    
            4. 示例输出
            [
              {
                "weekday": "星期二",
                "time_slot": "上午",
                "period": "1-2 节",
                "course_name": "计算机组成原理",
                "course_type": "理论",
                "weeks": "1-10 周",
                "campus": "校本部",
                "location": "管理楼305",
                "teacher": "李旎",
                "class_id": "0007",
                "students": ["2306804","2306805"],
                "assessment": "考试",
                "weekly_hours": 4,
                "credits": 3.5
              }
            ]
                    
            请务必确保每个课程记录的所有字段都有正确的值，不要遗漏任何信息。
            """;

    /**
     * 主要文档加载方法
     */
    public void documentsloade() {
        try {
            log.info("开始加载文档...");
            List<Document> documents = FileSystemDocumentLoader.loadDocuments("documents", new ApacheTikaDocumentParser());
            log.info("共加载 {} 个文档", documents.size());

            for (Document document : documents) {
                try {
                    log.info("处理文档: {}", document.metadata().getString("file_name"));
                    processDocument(document);
                } catch (Exception e) {
                    log.error("处理文档失败: {}", document.metadata(), e);
                }
            }
        } catch (Exception e) {
            log.error("文档加载失败", e);
        }
    }

    /**
     * 处理单个文档
     */
    private void processDocument(Document document) {
        // 判断是否为课表
        Boolean isCourseTable = checkIsCourseTable(document);
        log.info("文档类型判断: {}", isCourseTable ? "课程表" : "普通文档");

        if (isCourseTable) {
            processCourseTable(document);
        } else {
            processRegularDocument(document);
        }
    }

    /**
     * 改进的课表处理方法 - 增加重试机制和验证
     */
    private void processCourseTable(Document document) {
        try {
            log.info("开始处理课表文档，文档内容长度: {}", document.text().length());

            // 尝试多次解析，确保成功
            List<Course> courses = null;
            for (int attempt = 1; attempt <= MAX_RETRY_COUNT; attempt++) {
                log.info("第 {} 次尝试解析课表", attempt);

                courses = attemptCourseExtraction(document);

                if (courses != null && !courses.isEmpty() && validateCourses(courses)) {
                    log.info("第 {} 次尝试成功，解析到 {} 门课程", attempt, courses.size());
                    break;
                } else {
                    log.warn("第 {} 次尝试失败，解析结果无效", attempt);
                    if (attempt < MAX_RETRY_COUNT) {
                        log.info("等待重试...");
                        Thread.sleep(1000); // 等待1秒后重试
                    }
                }
            }

            if (courses == null || courses.isEmpty()) {
                log.error("所有尝试均失败，无法解析课表");
                // 作为备选方案，将原文档作为普通文档处理
                processRegularDocument(document);
                return;
            }

            // 打印解析结果用于调试
            log.info("课程解析详情:");
            courses.forEach(course -> {
                log.info("课程: {} | 教师: {} | 时间: {} {} {} | 地点: {} | 周次: {}",
                        course.getCourseName(),
                        course.getTeacher(),
                        course.getWeekday(),
                        course.getTimeSlot(),
                        course.getPeriod(),
                        course.getLocation(),
                        course.getWeeks()
                );
            });

            // 多维度存储课表信息
            storeCourseTableData(courses, document);

        } catch (Exception e) {
            log.error("课表处理失败", e);
            // 降级处理：作为普通文档处理
            processRegularDocument(document);
        }
    }

    /**
     * 单次课程提取尝试
     */
    private List<Course> attemptCourseExtraction(Document document) {
        try {
            String optimizedPrompt = extractionPrompt.replace("{markdown_table}", document.text());
            ChatResponse chatResponse = chatLanguageModel.chat(
                    SystemMessage.systemMessage(optimizedPrompt),
                    UserMessage.userMessage("请解析上述课表内容，确保每个课程的所有字段都完整")
            );

            String rawResponse = chatResponse.aiMessage().text();
            log.debug("LLM原始响应: {}", rawResponse);

            String jsonResult = cleanJsonResponse(rawResponse);
            log.debug("清理后的JSON: {}", jsonResult);

            return parseCoursesFromJson(jsonResult);
        } catch (Exception e) {
            log.error("课程提取失败", e);
            return Collections.emptyList();
        }
    }

    /**
     * 验证课程数据完整性
     */
    private boolean validateCourses(List<Course> courses) {
        if (courses == null || courses.isEmpty()) {
            return false;
        }
        int validCount = 0;
        for (Course course : courses) {
            if (isValidCourse(course)) {
                validCount++;
            } else {
                log.warn("发现无效课程记录: {}", course);
            }
        }
        double validRatio = (double) validCount / courses.size();
        log.info("课程验证结果: {}/{} 有效，有效率: {:.2%}", validCount, courses.size(), validRatio);

        return validRatio >= 0.8; // 至少80%的课程记录有效
    }

    /**
     * 验证单个课程记录
     */
    private boolean isValidCourse(Course course) {
        return course != null
                && hasValue(course.getCourseName())
                && hasValue(course.getWeekday())
                && hasValue(course.getTimeSlot())
                && hasValue(course.getPeriod())
                && hasValue(course.getWeeks());
    }

    private boolean hasValue(String value) {
        return value != null && !value.trim().isEmpty();
    }

    /**
     * 处理普通文档 - 优化后的文档处理逻辑
     */
    private void processRegularDocument(Document document) {
        try {
            log.info("开始处理普通文档");

            DocumentByParagraphSplitter primarySplitter = new DocumentByParagraphSplitter(
                    CHUNK_SIZE, OVERLAP_SIZE, tokenizer
            );
            List<TextSegment> primaryChunks = primarySplitter.split(document);
            log.info("文档分割为 {} 个块", primaryChunks.size());

            for (int i = 0; i < primaryChunks.size(); i++) {
                log.debug("处理文档块 {}/{}", i + 1, primaryChunks.size());
                processTextChunk(primaryChunks.get(i), document);
            }

        } catch (Exception e) {
            log.error("普通文档处理失败", e);
        }
    }

    /**
     * 处理文本块 - 分层存储策略
     */
    private void processTextChunk(TextSegment chunk, Document originalDocument) {
        try {
            String chunkText = chunk.text();

            // 1. 生成摘要用于高层检索
            String summary = generateSummary(chunkText);
            if (!summary.isEmpty()) {
                storeTextSegment(summary, createMetadata(originalDocument, "summary"));
            }

            // 2. 存储原始块用于中等粒度检索
            storeTextSegment(chunkText, createMetadata(originalDocument, "chunk"));

            // 3. 进行细粒度分割用于精确检索
            List<TextSegment> fineChunks = createFineGrainedChunks(chunkText);
            for (TextSegment fineChunk : fineChunks) {
                storeTextSegment(fineChunk.text(), createMetadata(originalDocument, "fine"));
            }

            // 4. 提取关键信息并单独存储
            storeKeyInformation(chunkText, originalDocument);

        } catch (Exception e) {
            log.error("文本块处理失败", e);
        }
    }

    /**
     * 增强的多维度存储课表数据 - 确保完整性
     */
    private void storeCourseTableData(List<Course> courses, Document document) {
        try {
            List<TextSegment> textSegments = new ArrayList<>();

            for (Course course : courses) {
                // 1. 完整课程信息 - JSON格式便于精确匹配
                String courseJson = convertCourseToJson(course);
                textSegments.add(TextSegment.from(courseJson, createCourseMetadata(course, "json")));

                // 2. 完整课程描述 - 自然语言格式
                String fullDescription = buildFullCourseDescription(course);
                textSegments.add(TextSegment.from(fullDescription, createCourseMetadata(course, "full")));

                // 3. 课程基本信息 - 便于课程名检索
                String basicInfo = buildBasicCourseInfo(course);
                textSegments.add(TextSegment.from(basicInfo, createCourseMetadata(course, "basic")));

                // 4. 时间维度信息 - 便于时间查询
                String timeInfo = buildTimeBasedCourseInfo(course);
                textSegments.add(TextSegment.from(timeInfo, createCourseMetadata(course, "time")));

                // 5. 教师维度信息 - 便于教师查询
                String teacherInfo = buildTeacherBasedCourseInfo(course);
                textSegments.add(TextSegment.from(teacherInfo, createCourseMetadata(course, "teacher")));

                // 6. 地点维度信息 - 便于地点查询
                String locationInfo = buildLocationBasedCourseInfo(course);
                textSegments.add(TextSegment.from(locationInfo, createCourseMetadata(course, "location")));

                // 7. 周次维度信息 - 便于周次查询
                List<String> weekInfos = buildWeekBasedCourseInfo(course);
                for (String weekInfo : weekInfos) {
                    textSegments.add(TextSegment.from(weekInfo, createCourseMetadata(course, "week")));
                }

                // 8. 结构化查询信息 - 便于自然语言查询
                String structuredInfo = buildStructuredCourseInfo(course);
                textSegments.add(TextSegment.from(structuredInfo, createCourseMetadata(course, "structured")));
            }

            // 批量生成 embedding
            log.info("生成 {} 个文本片段的向量嵌入", textSegments.size());
            List<Embedding> embeddings = embeddingModel.embedAll(textSegments).content();

            // 批量入库
            embeddingStore.addAll(embeddings, textSegments);

            log.info("成功存储 {} 门课程，共 {} 个文本片段", courses.size(), textSegments.size());

        } catch (Exception e) {
            log.error("课表数据存储失败", e);
        }
    }

    /**
     * 将课程对象转换为JSON字符串
     */
    private String convertCourseToJson(Course course) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsString(course);
        } catch (Exception e) {
            log.error("课程对象转JSON失败", e);
            return course.toString();
        }
    }

    /**
     * 构建基本课程信息
     */
    private String buildBasicCourseInfo(Course course) {
        return String.format("课程：%s，教师：%s，时间：%s %s %s，地点：%s",
                nullToEmpty(course.getCourseName()),
                nullToEmpty(course.getTeacher()),
                nullToEmpty(course.getWeekday()),
                nullToEmpty(course.getTimeSlot()),
                nullToEmpty(course.getPeriod()),
                nullToEmpty(course.getLocation()));
    }

    /**
     * 构建完整的课程描述
     */
    private String buildFullCourseDescription(Course course) {
        StringBuilder sb = new StringBuilder();
        sb.append("课程详情：");
        sb.append("课程名称：").append(course.getCourseName()).append("；");
        sb.append("授课教师：").append(nullToEmpty(course.getTeacher())).append("；");
        sb.append("上课时间：").append(course.getWeekday())
                .append(" ").append(course.getTimeSlot())
                .append(" ").append(course.getPeriod()).append("；");
        sb.append("上课周次：").append(course.getWeeks()).append("；");
        sb.append("上课地点：").append(nullToEmpty(course.getLocation())).append("；");
        sb.append("课程类型：").append(nullToEmpty(course.getCourseType())).append("；");
        sb.append("学分：").append(course.getCredits() != null ? course.getCredits() : "未知").append("；");
        sb.append("考核方式：").append(nullToEmpty(course.getAssessment()));

        return sb.toString();
    }

    /**
     * 构建基于时间的课程信息
     */
    private String buildTimeBasedCourseInfo(Course course) {
        return String.format("时间安排：%s %s %s 课程：%s 教师：%s 地点：%s 周次：%s",
                course.getWeekday(),
                course.getTimeSlot(),
                course.getPeriod(),
                course.getCourseName(),
                nullToEmpty(course.getTeacher()),
                nullToEmpty(course.getLocation()),
                course.getWeeks());
    }

    /**
     * 构建基于教师的课程信息
     */
    private String buildTeacherBasedCourseInfo(Course course) {
        return String.format("教师安排：%s老师教授%s课程，时间是%s %s %s，地点在%s，周次为%s",
                nullToEmpty(course.getTeacher()),
                course.getCourseName(),
                course.getWeekday(),
                course.getTimeSlot(),
                course.getPeriod(),
                nullToEmpty(course.getLocation()),
                course.getWeeks());
    }

    /**
     * 构建基于地点的课程信息
     */
    private String buildLocationBasedCourseInfo(Course course) {
        String location = nullToEmpty(course.getLocation());
        if (location.isEmpty()) {
            location = "未安排地点";
        }
        return String.format("地点安排：%s有%s课程，教师：%s，时间：%s %s %s，周次：%s",
                location,
                course.getCourseName(),
                nullToEmpty(course.getTeacher()),
                course.getWeekday(),
                course.getTimeSlot(),
                course.getPeriod(),
                course.getWeeks());
    }

    /**
     * 构建基于周次的课程信息
     */
    private List<String> buildWeekBasedCourseInfo(Course course) {
        List<String> weekInfos = new ArrayList<>();
        String weeks = course.getWeeks();

        if (weeks != null) {
            // 解析周次范围
            Set<Integer> weekNumbers = parseWeekNumbers(weeks);

            // 为每个具体周次创建信息
            for (Integer weekNum : weekNumbers) {
                String weekInfo = String.format("第%d周：%s %s %s有%s课，教师：%s，地点：%s",
                        weekNum,
                        course.getWeekday(),
                        course.getTimeSlot(),
                        course.getPeriod(),
                        course.getCourseName(),
                        nullToEmpty(course.getTeacher()),
                        nullToEmpty(course.getLocation()));
                weekInfos.add(weekInfo);
            }

            // 添加周次范围信息
            String rangeInfo = String.format("周次范围：%s期间%s %s %s有%s课程",
                    weeks,
                    course.getWeekday(),
                    course.getTimeSlot(),
                    course.getPeriod(),
                    course.getCourseName());
            weekInfos.add(rangeInfo);
        }

        return weekInfos;
    }

    /**
     * 构建结构化课程信息
     */
    private String buildStructuredCourseInfo(Course course) {
        return String.format("%s %s %s有%s课，教师是%s，地点在%s，上课周次为%s",
                course.getWeekday(),
                course.getTimeSlot(),
                course.getPeriod(),
                course.getCourseName(),
                nullToEmpty(course.getTeacher()),
                nullToEmpty(course.getLocation()),
                course.getWeeks());
    }

    /**
     * 解析周次字符串为具体周次集合
     */
    private Set<Integer> parseWeekNumbers(String weeks) {
        Set<Integer> weekNumbers = new HashSet<>();
        if (weeks == null) return weekNumbers;

        try {
            // 处理 "1-16 周" 格式
            Pattern rangePattern = Pattern.compile("(\\d+)-(\\d+)\\s*周");
            Matcher rangeMatcher = rangePattern.matcher(weeks);
            if (rangeMatcher.find()) {
                int start = Integer.parseInt(rangeMatcher.group(1));
                int end = Integer.parseInt(rangeMatcher.group(2));
                for (int i = start; i <= end; i++) {
                    weekNumbers.add(i);
                }
            }

            // 处理 "单独周次" 格式，如 "14 周"
            Pattern singlePattern = Pattern.compile("(\\d+)\\s*周");
            Matcher singleMatcher = singlePattern.matcher(weeks);
            while (singleMatcher.find()) {
                weekNumbers.add(Integer.parseInt(singleMatcher.group(1)));
            }

        } catch (Exception e) {
            log.debug("解析周次失败: {}", weeks);
        }

        return weekNumbers;
    }

    /**
     * 生成文本摘要
     */
    private String generateSummary(String text) {
        try {
            String summaryPrompt = String.format(
                    "请将以下内容概括为不超过%d个token的精炼摘要，保留核心信息和关键词：",
                    SUMMARY_TOKEN_SIZE
            );

            ChatResponse response = chatLanguageModel.chat(
                    SystemMessage.systemMessage(summaryPrompt),
                    UserMessage.userMessage(text)
            );

            return response.aiMessage().text().trim();
        } catch (Exception e) {
            log.error("生成摘要失败", e);
            return "";
        }
    }

    /**
     * 创建细粒度文本块
     */
    private List<TextSegment> createFineGrainedChunks(String text) {
        try {
            DocumentByParagraphSplitter fineSplitter = new DocumentByParagraphSplitter(
                    FINE_CHUNK_SIZE, 50, tokenizer
            );
            Document tempDoc = Document.from(text);
            return fineSplitter.split(tempDoc);
        } catch (Exception e) {
            log.error("细粒度分割失败", e);
            return Collections.emptyList();
        }
    }

    /**
     * 提取并存储关键信息
     */
    private void storeKeyInformation(String text, Document document) {
        try {
            // 提取关键实体和概念
            Set<String> keywords = extractKeywords(text);
            if (!keywords.isEmpty()) {
                String keywordText = "关键词：" + String.join("，", keywords);
                storeTextSegment(keywordText, createMetadata(document, "keywords"));
            }

            // 提取数字和日期信息
            Set<String> numbers = extractNumbers(text);
            if (!numbers.isEmpty()) {
                String numberText = "数字信息：" + String.join("，", numbers);
                storeTextSegment(numberText, createMetadata(document, "numbers"));
            }

        } catch (Exception e) {
            log.error("关键信息提取失败", e);
        }
    }

    /**
     * 存储文本片段
     */
    private void storeTextSegment(String text, Metadata metadata) {
        try {
            TextSegment segment = TextSegment.from(text, metadata);
            Embedding embedding = embeddingModel.embed(text).content();
            embeddingStore.add(embedding, segment);
        } catch (Exception e) {
            log.error("文本片段存储失败: {}", text.substring(0, Math.min(50, text.length())), e);
        }
    }

    /**
     * 改进的JSON清理方法
     */
    private String cleanJsonResponse(String response) {
        if (response == null) return "[]";

        // 移除markdown代码块标记
        response = response.replaceAll("```json\\s*", "").replaceAll("```\\s*$", "");

        // 移除多余的解释文字（保留JSON部分）
        int jsonStart = response.indexOf('[');
        int jsonEnd = response.lastIndexOf(']');

        if (jsonStart >= 0 && jsonEnd > jsonStart) {
            response = response.substring(jsonStart, jsonEnd + 1);
        }

        return response.trim();
    }

    /**
     * 解析课程JSON
     */
    private List<Course> parseCoursesFromJson(String jsonString) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            List<Course> courses = objectMapper.readValue(jsonString, new TypeReference<List<Course>>() {
            });

            // 验证解析结果
            if (courses != null) {
                courses.removeIf(course -> !isValidCourse(course));
            }

            return courses != null ? courses : Collections.emptyList();
        } catch (Exception e) {
            log.error("JSON解析失败: {}", jsonString, e);
            return Collections.emptyList();
        }
    }

    /**
     * 创建文档元数据
     */
    private Metadata createMetadata(Document document, String type) {
        Map<String, Object> metadataMap = new HashMap<>(document.metadata().asMap());
        metadataMap.put("segment_type", type);
        metadataMap.put("timestamp", System.currentTimeMillis());
        return Metadata.from(metadataMap);
    }

    /**
     * 增强的课程元数据创建
     */
    private Metadata createCourseMetadata(Course course, String type) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("document_type", "course_table");
        metadata.put("segment_type", type);
        metadata.put("course_name", course.getCourseName());
        metadata.put("teacher", nullToEmpty(course.getTeacher()));
        metadata.put("weekday", course.getWeekday());
        metadata.put("time_slot", course.getTimeSlot());
        metadata.put("period", course.getPeriod());
        metadata.put("weeks", course.getWeeks());
        metadata.put("location", nullToEmpty(course.getLocation()));
        metadata.put("course_type", nullToEmpty(course.getCourseType()));
        metadata.put("credits", course.getCredits());
        metadata.put("assessment", nullToEmpty(course.getAssessment()));
        metadata.put("timestamp", System.currentTimeMillis());

        // 解析周次范围，便于数值查询
        parseWeeksRange(course.getWeeks(), metadata);

        return Metadata.from(metadata);
    }

    /**
     * 提取关键词
     */
    private Set<String> extractKeywords(String text) {
        Set<String> keywords = new HashSet<>();
        // 简单的关键词提取逻辑，可以替换为更复杂的NLP处理
        String[] commonKeywords = {"课程", "教师", "学生", "考试", "成绩", "时间", "地点", "通知", "作业"};

        for (String keyword : commonKeywords) {
            if (text.contains(keyword)) {
                keywords.add(keyword);
            }
        }

        return keywords;
    }

    /**
     * 提取数字信息
     */
    private Set<String> extractNumbers(String text) {
        Set<String> numbers = new HashSet<>();
        // 提取日期、时间、学号等数字信息
        Pattern pattern = Pattern.compile("\\d{4}-\\d{2}-\\d{2}|\\d{2}:\\d{2}|\\d{7,10}");
        Matcher matcher = pattern.matcher(text);

        while (matcher.find()) {
            numbers.add(matcher.group());
        }

        return numbers;
    }

    /**
     * 优化后的课表评分系统
     */
    private Boolean checkIsCourseTable(Document document) {
        String text = document.text().toLowerCase();
        double score = 0;
        // 核心关键词权重更高
        Map<String, Double> keywords = Map.of(
                "课表", 0.25,
                "教学班", 0.2,
                "星期", 0.15,
                "学分", 0.15,
                "课程", 0.1,
                "考核", 0.1,
                "时间", 0.05,
                "teacher", 0.05,
                "course", 0.05
        );

        for (Map.Entry<String, Double> entry : keywords.entrySet()) {
            if (text.contains(entry.getKey())) {
                score += entry.getValue();
            }
        }

        // 表格结构检测
        if (text.contains("|") && text.contains("-")) {
            score += 0.1;
        }

        boolean isCourseTable = score >= COURSE_TABLE_MIN_SCORE;
        log.info("文档课表评分: {:.2f}, 判定结果: {}", score, isCourseTable ? "是课表" : "不是课表");

        return isCourseTable;
    }

    /**
     * 解析周次范围
     */
    private void parseWeeksRange(String weeks, Map<String, Object> metadata) {
        try {
            if (weeks != null && weeks.matches("\\d+-\\d+\\s*周")) {
                String[] parts = weeks.replaceAll("\\s*周", "").split("-");
                if (parts.length == 2) {
                    metadata.put("week_start", Integer.parseInt(parts[0]));
                    metadata.put("week_end", Integer.parseInt(parts[1]));
                }
            } else if (weeks != null && weeks.matches("\\d+\\s*周")) {
                int weekNum = Integer.parseInt(weeks.replaceAll("\\s*周", ""));
                metadata.put("week_start", weekNum);
                metadata.put("week_end", weekNum);
            }
        } catch (Exception e) {
            log.debug("解析周次范围失败: {}", weeks);
        }
    }

    /**
     * 防止空指针的工具方法
     */
    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}