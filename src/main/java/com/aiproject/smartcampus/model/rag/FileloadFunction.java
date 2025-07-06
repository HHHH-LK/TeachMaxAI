package com.aiproject.smartcampus.model.rag;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.parser.apache.tika.ApacheTikaDocumentParser;
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

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

/**
 * 智能文档加载和处理器
 * 功能：
 * 1. 支持多种文档格式的批量加载
 * 2. 智能文档结构识别和分析
 * 3. 多层次文本分割和优化
 * 4. 向量嵌入生成和存储
 * 5. 文档缓存和性能优化
 *
 * @author lk
 * @since 2025-05-17
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FileloadFunction {

    // === 依赖注入 ===
    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore embeddingStore;
    private final ChatLanguageModel chatLanguageModel;
    private final LocalTokenizerFuncation tokenizer;

    // === 常量配置 ===
    private static final String DEFAULT_DOCUMENT_PATH = "documents";
    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of(
            ".txt", ".md", ".pdf", ".docx", ".doc", ".rtf", ".html", ".htm"
    );

    // === 文档结构识别正则表达式 ===
    private static final Pattern TITLE_PATTERN = Pattern.compile(
            "^#{1,6}\\s+(.+)$|^(.+)\\n[=-]{3,}$", Pattern.MULTILINE);
    private static final Pattern LIST_PATTERN = Pattern.compile(
            "^[\\s]*[-*+]\\s+(.+)$|^[\\s]*\\d+\\.\\s+(.+)$", Pattern.MULTILINE);
    private static final Pattern CODE_PATTERN = Pattern.compile("```[\\s\\S]*?```|`[^`]+`");
    private static final Pattern TABLE_PATTERN = Pattern.compile("\\|.*\\|");

    // === 缓存 ===
    private final Map<String, List<TextSegment>> documentCache = new ConcurrentHashMap<>();

    // ========================================
    // 公共API方法
    // ========================================

    /**
     * 主要文档加载方法 - 默认加载documents目录下所有文件
     */
    public void documentsloade() {
        documentsloade(null);
    }

    /**
     * 文档加载方法 - 支持指定文件或默认目录
     * @param file 指定文件，为null时加载默认目录
     */
    public void documentsloade(File file) {
        if (file == null || !file.exists()) {
            log.info("开始加载默认文档目录下的所有文件...");
            loadDefaultDirectory();
        } else {
            log.info("开始处理用户指定文件: {}", file.getAbsolutePath());
            loadSingleFile(file);
        }
    }

    /**
     * 批量加载目录下的所有文件
     * @param directoryPath 目录路径
     */
    public void loadAllFilesInDirectory(String directoryPath) {
        try {
            log.info("开始加载目录 {} 下的所有文档", directoryPath);

            List<Document> documents = FileSystemDocumentLoader.loadDocuments(
                    directoryPath, new ApacheTikaDocumentParser()
            );

            processDocumentBatch(documents, directoryPath);

        } catch (Exception e) {
            log.error("批量加载目录文件失败: {}", directoryPath, e);
        }
    }

    /**
     * 批量加载多个指定文件
     * @param filePaths 文件路径列表
     */
    public void loadMultipleFiles(List<String> filePaths) {
        if (filePaths == null || filePaths.isEmpty()) {
            log.warn("文件路径列表为空，加载默认目录");
            documentsloade();
            return;
        }

        log.info("开始批量处理 {} 个文件", filePaths.size());

        int successCount = 0;
        int errorCount = 0;

        for (String filePath : filePaths) {
            try {
                File file = new File(filePath);
                if (file.exists() && file.isFile()) {
                    documentsloade(file);
                    successCount++;
                } else {
                    log.warn("文件不存在或不是文件: {}", filePath);
                    errorCount++;
                }
            } catch (Exception e) {
                log.error("处理文件失败: {}", filePath, e);
                errorCount++;
            }
        }

        log.info("批量文件处理完成 - 成功: {}, 失败: {}", successCount, errorCount);
    }

    /**
     * 生成文档摘要报告
     * @param file 文档文件
     * @return 文档摘要报告
     */
    public DocumentSummaryReport generateDocumentReport(File file) {
        try {
            String fileKey = generateFileKey(file);
            List<TextSegment> segments = documentCache.get(fileKey);

            if (segments == null || segments.isEmpty()) {
                return new DocumentSummaryReport("文档未找到或未处理", "", new ArrayList<>(), new HashMap<>());
            }

            String documentSummary = getMetadataValue(segments.get(0), "document_summary");
            List<String> allKeywords = extractAllKeywords(segments);
            Map<String, Long> topicDistribution = calculateTopicDistribution(segments);

            return new DocumentSummaryReport(
                    file.getName(),
                    documentSummary != null ? documentSummary : "无摘要",
                    allKeywords,
                    topicDistribution
            );

        } catch (Exception e) {
            log.error("生成文档报告失败: {}", file.getName(), e);
            return new DocumentSummaryReport("报告生成失败", "", new ArrayList<>(), new HashMap<>());
        }
    }

    /**
     * 获取文档统计信息
     * @return 统计信息映射
     */
    public Map<String, Object> getDocumentStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("cached_documents", documentCache.size());
        stats.put("total_segments", documentCache.values().stream().mapToInt(List::size).sum());
        stats.put("supported_formats", String.join(", ", SUPPORTED_EXTENSIONS));
        stats.put("default_path", DEFAULT_DOCUMENT_PATH);
        return stats;
    }

    /**
     * 清理缓存
     */
    public void clearCache() {
        documentCache.clear();
        log.info("文档缓存已清理");
    }

    // ========================================
    // 核心处理方法
    // ========================================

    /**
     * 加载默认目录
     */
    private void loadDefaultDirectory() {
        try {
            log.info("开始加载文档...");

            List<Document> documents = loadDocumentsFromDefaultPath();
            if (documents.isEmpty()) {
                log.error("所有路径都未找到文档，请检查documents目录是否存在且包含支持的文件");
                return;
            }

            log.info("共加载 {} 个文档", documents.size());
            processDocumentBatch(documents, DEFAULT_DOCUMENT_PATH);

        } catch (Exception e) {
            log.error("批量加载文档失败", e);
        }
    }

    /**
     * 从默认路径加载文档
     */
    private List<Document> loadDocumentsFromDefaultPath() {
        List<Document> documents = new ArrayList<>();

        // 尝试主路径
        try {
            documents = FileSystemDocumentLoader.loadDocuments(
                    DEFAULT_DOCUMENT_PATH, new ApacheTikaDocumentParser()
            );
            if (!documents.isEmpty()) {
                return documents;
            }
        } catch (Exception e) {
            log.debug("主路径 {} 加载失败: {}", DEFAULT_DOCUMENT_PATH, e.getMessage());
        }

        // 尝试备用路径
        List<String> alternatePaths = Arrays.asList(
                "src/main/resources/documents",
                "./documents",
                System.getProperty("user.dir") + "/documents"
        );

        for (String path : alternatePaths) {
            try {
                documents = FileSystemDocumentLoader.loadDocuments(path, new ApacheTikaDocumentParser());
                if (!documents.isEmpty()) {
                    log.info("在备用路径 {} 中找到 {} 个文档", path, documents.size());
                    break;
                }
            } catch (Exception e) {
                log.debug("备用路径 {} 加载失败: {}", path, e.getMessage());
            }
        }

        return documents;
    }

    /**
     * 批量处理文档
     */
    private void processDocumentBatch(List<Document> documents, String sourcePath) {
        if (documents.isEmpty()) {
            log.warn("目录 {} 中未找到任何文档", sourcePath);
            return;
        }

        int successCount = 0;
        int errorCount = 0;

        for (Document document : documents) {
            try {
                String fileName = getDocumentFileName(document);
                log.info("正在处理文档: {}", fileName);

                processDocument(document);
                successCount++;

            } catch (Exception e) {
                String fileName = getDocumentFileName(document);
                log.error("处理文档失败: {}", fileName, e);
                errorCount++;
            }
        }

        log.info("目录 {} 处理完成 - 成功: {}, 失败: {}", sourcePath, successCount, errorCount);
    }

    /**
     * 加载单个文件
     */
    private void loadSingleFile(File file) {
        try {
            // 检查文件缓存
            String fileKey = generateFileKey(file);
            if (documentCache.containsKey(fileKey)) {
                log.info("文档已存在缓存中，跳过重复处理: {}", file.getName());
                return;
            }

            // 加载文档
            Document document = loadDocument(file);
            if (document == null) {
                log.error("文档加载失败: {}", file.getName());
                return;
            }

            // 处理文档
            processDocument(document);

            log.info("文档处理完成: {}", file.getName());

        } catch (Exception e) {
            log.error("处理用户文件时发生错误: {}", file.getName(), e);
        }
    }

    /**
     * 处理单个文档的核心逻辑
     */
    private void processDocument(Document document) {
        try {
            String fileName = getDocumentFileName(document);
            log.debug("开始处理文档: {}, 内容长度: {}", fileName, document.text().length());

            // 1. 智能识别文档结构
            DocumentStructure structure = analyzeDocumentStructure(document);

            // 2. 多层次分割文档
            List<TextSegment> segments = performMultiLevelSplitting(document, structure);

            if (segments.isEmpty()) {
                log.warn("文档分割后无有效段落: {}", fileName);
                return;
            }

            // 3. 生成文档级摘要
            String documentSummary = generateDocumentSummary(segments);

            // 4. 增强文本段落
            List<TextSegment> enhancedSegments = enhanceTextSegments(segments, structure, documentSummary);

            // 5. 生成向量嵌入
            List<Embedding> embeddings = generateEmbeddings(enhancedSegments);

            // 6. 存储到向量数据库
            storeEmbeddings(enhancedSegments, embeddings);

            // 7. 缓存处理结果
            String fileKey = generateDocumentKey(document);
            documentCache.put(fileKey, enhancedSegments);

            log.info("文档处理完成: {}, 生成段落: {}", fileName, enhancedSegments.size());

        } catch (Exception e) {
            String fileName = getDocumentFileName(document);
            log.error("处理文档时发生错误: {}", fileName, e);
        }
    }

    // ========================================
    // 文档结构分析
    // ========================================

    /**
     * 分析文档结构
     */
    private DocumentStructure analyzeDocumentStructure(Document document) {
        DocumentStructure structure = new DocumentStructure();

        if (document == null || document.text() == null || document.text().trim().isEmpty()) {
            log.warn("文档为空或无内容，返回空的文档结构");
            return structure;
        }

        String content = document.text();

        try {
            // 识别标题
            extractTitles(content, structure);

            // 识别列表
            extractLists(content, structure);

            // 识别代码块
            extractCodeBlocks(content, structure);

            // 识别表格
            extractTables(content, structure);

            log.debug("文档结构分析完成 - 标题:{}, 列表:{}, 代码块:{}, 表格:{}",
                    structure.getTitles().size(),
                    structure.getListItems().size(),
                    structure.getCodeBlocks().size(),
                    structure.getTables().size());

        } catch (Exception e) {
            log.error("文档结构分析失败", e);
        }

        return structure;
    }

    private void extractTitles(String content, DocumentStructure structure) {
        Matcher titleMatcher = TITLE_PATTERN.matcher(content);
        while (titleMatcher.find()) {
            String title = titleMatcher.group(1) != null ? titleMatcher.group(1) : titleMatcher.group(2);
            if (title != null && !title.trim().isEmpty()) {
                structure.addTitle(title.trim(), titleMatcher.start());
            }
        }
    }

    private void extractLists(String content, DocumentStructure structure) {
        Matcher listMatcher = LIST_PATTERN.matcher(content);
        while (listMatcher.find()) {
            String listItem = listMatcher.group(1) != null ? listMatcher.group(1) : listMatcher.group(2);
            if (listItem != null && !listItem.trim().isEmpty()) {
                structure.addListItem(listItem.trim(), listMatcher.start());
            }
        }
    }

    private void extractCodeBlocks(String content, DocumentStructure structure) {
        Matcher codeMatcher = CODE_PATTERN.matcher(content);
        while (codeMatcher.find()) {
            structure.addCodeBlock(codeMatcher.group(), codeMatcher.start());
        }
    }

    private void extractTables(String content, DocumentStructure structure) {
        Matcher tableMatcher = TABLE_PATTERN.matcher(content);
        while (tableMatcher.find()) {
            structure.addTable(tableMatcher.group(), tableMatcher.start());
        }
    }

    // ========================================
    // 文本分割和优化
    // ========================================

    /**
     * 多层次文本分割
     */
    private List<TextSegment> performMultiLevelSplitting(Document document, DocumentStructure structure) {
        List<TextSegment> segments = new ArrayList<>();
        String content = document.text();

        // 1. 基于文档结构的主要分割
        List<TextSegment> structuralSegments = splitByStructure(content, structure);

        // 2. 对每个结构段落进行二次分割
        for (TextSegment segment : structuralSegments) {
            List<TextSegment> refinedSegments = performSemanticSplitting(segment, structure);
            segments.addAll(refinedSegments);
        }

        // 3. 质量检查和优化
        segments = optimizeSegments(segments);

        return segments;
    }

    /**
     * 基于结构的分割
     */
    private List<TextSegment> splitByStructure(String content, DocumentStructure structure) {
        List<TextSegment> segments = new ArrayList<>();

        if (content == null || content.trim().isEmpty()) {
            log.warn("内容为空，无法进行结构化分割");
            return segments;
        }

        if (structure == null || structure.isEmpty()) {
            log.warn("文档结构为空，使用简单分割策略");
            return performSimpleSplit(content);
        }

        List<Integer> splitPoints = collectSplitPoints(structure, content.length());
        return createSegmentsFromSplitPoints(content, splitPoints);
    }

    private List<TextSegment> performSimpleSplit(String content) {
        List<TextSegment> segments = new ArrayList<>();
        String[] paragraphs = content.split("\n\n+");

        for (String paragraph : paragraphs) {
            if (paragraph.trim().length() > 50) {
                Metadata metadata = new Metadata();
                metadata.put("split_method", "simple_paragraph");
                segments.add(TextSegment.from(paragraph.trim(), metadata));
            }
        }

        return segments;
    }

    private List<Integer> collectSplitPoints(DocumentStructure structure, int contentLength) {
        List<Integer> splitPoints = new ArrayList<>();

        structure.getTitles().forEach(title -> splitPoints.add(title.getPosition()));
        structure.getCodeBlocks().forEach(code -> splitPoints.add(code.getPosition()));
        structure.getTables().forEach(table -> splitPoints.add(table.getPosition()));

        splitPoints.sort(Integer::compareTo);
        splitPoints.add(0, 0); // 添加开始点
        splitPoints.add(contentLength); // 添加结束点

        return splitPoints;
    }

    private List<TextSegment> createSegmentsFromSplitPoints(String content, List<Integer> splitPoints) {
        List<TextSegment> segments = new ArrayList<>();

        for (int i = 0; i < splitPoints.size() - 1; i++) {
            int start = splitPoints.get(i);
            int end = splitPoints.get(i + 1);

            if (end > start && end - start > 50) {
                String segmentText = content.substring(start, Math.min(end, content.length())).trim();
                if (!segmentText.isEmpty()) {
                    Metadata metadata = new Metadata();
                    metadata.put("split_method", "structural");
                    metadata.put("position", String.valueOf(start));
                    segments.add(TextSegment.from(segmentText, metadata));
                }
            }
        }

        return segments;
    }

    /**
     * 语义分割
     */
    private List<TextSegment> performSemanticSplitting(TextSegment segment, DocumentStructure structure) {
        List<TextSegment> segments = new ArrayList<>();
        String text = segment.text();

        int maxChunkSize = determineOptimalChunkSize(text, structure);
        int overlapSize = Math.min(maxChunkSize / 10, 100);

        if (text.length() <= maxChunkSize) {
            segments.add(segment);
            return segments;
        }

        List<String> sentences = splitIntoSentences(text);
        return createSemanticChunks(sentences, maxChunkSize, overlapSize, segment.metadata());
    }

    private int determineOptimalChunkSize(String text, DocumentStructure structure) {
        if (containsCode(text)) return 800;
        if (containsTables(text)) return 1000;
        if (containsLists(text)) return 600;
        if (isTechnicalContent(text)) return 700;
        return 500;
    }

    private List<String> splitIntoSentences(String text) {
        List<String> sentences = new ArrayList<>();
        String[] rawSentences = text.split("[.!?。！？]");

        for (String sentence : rawSentences) {
            sentence = sentence.trim();
            if (sentence.length() > 10) {
                sentences.add(sentence);
            }
        }

        return sentences;
    }

    private List<TextSegment> createSemanticChunks(List<String> sentences, int maxChunkSize,
                                                   int overlapSize, Metadata originalMetadata) {
        List<TextSegment> segments = new ArrayList<>();
        StringBuilder currentChunk = new StringBuilder();

        for (String sentence : sentences) {
            if (currentChunk.length() + sentence.length() > maxChunkSize && currentChunk.length() > 0) {
                // 创建段落
                Metadata metadata = copyMetadata(originalMetadata);
                metadata.put("chunk_method", "semantic");
                metadata.put("chunk_size", String.valueOf(currentChunk.length()));
                segments.add(TextSegment.from(currentChunk.toString().trim(), metadata));

                // 开始新段落，保持重叠
                String overlap = getLastSentences(currentChunk.toString(), overlapSize);
                currentChunk = new StringBuilder(overlap);
            }

            currentChunk.append(sentence).append(" ");
        }

        // 添加最后一个段落
        if (currentChunk.length() > 0) {
            Metadata metadata = copyMetadata(originalMetadata);
            metadata.put("chunk_method", "semantic");
            metadata.put("chunk_size", String.valueOf(currentChunk.length()));
            segments.add(TextSegment.from(currentChunk.toString().trim(), metadata));
        }

        return segments;
    }

    /**
     * 优化段落质量
     */
    private List<TextSegment> optimizeSegments(List<TextSegment> segments) {
        return segments.stream()
                .filter(segment -> isHighQualitySegment(segment.text()))
                .map(this::addQualityScore)
                .collect(Collectors.toList());
    }

    private TextSegment addQualityScore(TextSegment segment) {
        Metadata metadata = segment.metadata();
        metadata.put("quality_score", String.valueOf(calculateQualityScore(segment.text())));
        return TextSegment.from(segment.text(), metadata);
    }

    // ========================================
    // 文本增强和摘要生成
    // ========================================

    /**
     * 增强文本段落
     */
    private List<TextSegment> enhanceTextSegments(List<TextSegment> segments,
                                                  DocumentStructure structure,
                                                  String documentSummary) {
        return segments.stream()
                .map(segment -> enhanceSingleSegment(segment, structure, documentSummary))
                .collect(Collectors.toList());
    }

    /**
     * 增强单个文本段落
     */
    private TextSegment enhanceSingleSegment(TextSegment segment, DocumentStructure structure, String documentSummary) {
        Metadata metadata = segment.metadata();
        String text = segment.text();

        // 添加基本信息
        addBasicMetadata(metadata, text);

        // 添加结构化信息
        addStructuralMetadata(metadata, text, structure);

        // 生成段落摘要
        addSegmentSummary(metadata, text);

        // 添加文档级摘要引用
        metadata.put("document_summary", documentSummary);

        // 提取关键词
        addKeywords(metadata, text);

        // 添加主题分类
        addTopicClassification(metadata, text);

        // 添加分词信息
        addTokenizationInfo(metadata, text);

        return TextSegment.from(text, metadata);
    }

    private void addBasicMetadata(Metadata metadata, String text) {
        metadata.put("word_count", String.valueOf(text.split("\\s+").length));
        metadata.put("char_count", String.valueOf(text.length()));
    }

    private void addStructuralMetadata(Metadata metadata, String text, DocumentStructure structure) {
        if (containsStructuralElement(text, structure)) {
            metadata.put("has_structure", "true");
            metadata.put("structure_type", identifyStructureType(text, structure));
        }
    }

    private void addSegmentSummary(Metadata metadata, String text) {
        String segmentSummary = generateSegmentSummary(text);
        if (!segmentSummary.isEmpty()) {
            metadata.put("summary", segmentSummary);
        }
    }

    private void addKeywords(Metadata metadata, String text) {
        List<String> keywords = extractKeywords(text);
        if (!keywords.isEmpty()) {
            metadata.put("keywords", String.join(", ", keywords));
        }
    }

    private void addTopicClassification(Metadata metadata, String text) {
        String topic = classifyTopic(text);
        metadata.put("topic", topic);
    }

    private void addTokenizationInfo(Metadata metadata, String text) {
        try {
            List<String> tokens = performTokenization(text);
            metadata.put("token_count", String.valueOf(tokens.size()));

            int estimatedTokens = tokenizer.estimateTokenCountInText(text);
            metadata.put("estimated_tokens", String.valueOf(estimatedTokens));

        } catch (Exception e) {
            log.debug("分词处理失败: {}", text.substring(0, Math.min(50, text.length())));
        }
    }

    // ========================================
    // 向量嵌入处理
    // ========================================

    /**
     * 生成向量嵌入
     */
    private List<Embedding> generateEmbeddings(List<TextSegment> segments) {
        List<Embedding> embeddings = new ArrayList<>();

        for (TextSegment segment : segments) {
            try {
                Embedding embedding = embeddingModel.embed(segment).content();
                embeddings.add(embedding);
            } catch (Exception e) {
                log.error("生成嵌入向量失败: {}",
                        segment.text().substring(0, Math.min(50, segment.text().length())), e);
                embeddings.add(null);
            }
        }

        return embeddings;
    }

    /**
     * 存储向量嵌入
     */
        private void storeEmbeddings(List<TextSegment> segments, List<Embedding> embeddings) {
            for (int i = 0; i < segments.size(); i++) {
                if (embeddings.get(i) != null) {
                    try {
                        embeddingStore.add(embeddings.get(i), segments.get(i));
                        log.info("嵌入成功{},{}",embeddings.get(i), segments.get(i));

                    } catch (Exception e) {
                        log.error("存储嵌入向量失败: {}",
                                segments.get(i).text().substring(0, Math.min(50, segments.get(i).text().length())), e);
                    }
                }
            }
        }

    // ========================================
    // 工具方法
    // ========================================

    private Document loadDocument(File file) {
        try {
            return FileSystemDocumentLoader.loadDocument(
                    file.toPath(), new ApacheTikaDocumentParser()
            );
        } catch (Exception e) {
            log.error("加载文档失败: {}", file.getName(), e);
            return null;
        }
    }

    private String getDocumentFileName(Document document) {
        try {
            Map<String, Object> metadataMap = document.metadata().toMap();
            String[] fileNameKeys = {"file_name", "fileName", "name", "source", "path"};

            for (String key : fileNameKeys) {
                Object value = metadataMap.get(key);
                if (value != null) {
                    return value.toString();
                }
            }

            return "document_" + System.currentTimeMillis();

        } catch (Exception e) {
            log.debug("获取文档文件名失败", e);
            return "unknown_document";
        }
    }

    private String generateFileKey(File file) {
        return file.getAbsolutePath() + "_" + file.lastModified();
    }

    private String generateDocumentKey(Document document) {
        try {
            String fileName = getDocumentFileName(document);
            String contentHash = String.valueOf(document.text().hashCode());
            return fileName + "_" + contentHash;
        } catch (Exception e) {
            return "doc_" + System.currentTimeMillis();
        }
    }

    private Metadata copyMetadata(Metadata original) {
        Metadata copy = new Metadata();
        if (original != null) {
            Map<String, Object> originalMap = original.toMap();
            originalMap.forEach((key, value) -> {
                if (value != null) {
                    copy.put(key, value.toString());
                }
            });
        }
        return copy;
    }

    private String getMetadataValue(TextSegment segment, String key) {
        try {
            Metadata metadata = segment.metadata();
            Map<String, Object> metadataMap = metadata.toMap();
            Object value = metadataMap.get(key);
            return value != null ? value.toString() : null;
        } catch (Exception e) {
            log.debug("获取元数据失败: key={}", key);
            return null;
        }
    }

    private List<String> extractAllKeywords(List<TextSegment> segments) {
        return segments.stream()
                .map(segment -> getMetadataValue(segment, "keywords"))
                .filter(Objects::nonNull)
                .filter(keywords -> !keywords.isEmpty())
                .flatMap(keywords -> Arrays.stream(keywords.split(", ")))
                .distinct()
                .collect(Collectors.toList());
    }

    private Map<String, Long> calculateTopicDistribution(List<TextSegment> segments) {
        return segments.stream()
                .map(segment -> getMetadataValue(segment, "topic"))
                .filter(Objects::nonNull)
                .filter(topic -> !topic.isEmpty())
                .collect(Collectors.groupingBy(
                        topic -> topic,
                        Collectors.counting()
                ));
    }

    // ========================================
    // 内容分析方法
    // ========================================

    private boolean containsCode(String text) {
        return text.contains("```") || text.contains("function") || text.contains("class ") ||
                text.contains("def ") || text.contains("import ") || text.contains("package ");
    }

    private boolean containsTables(String text) {
        return text.contains("|") && text.contains("---");
    }

    private boolean containsLists(String text) {
        return text.matches(".*^\\s*[-*+]\\s+.*") || text.matches(".*^\\s*\\d+\\.\\s+.*");
    }

    private boolean isTechnicalContent(String text) {
        String[] techKeywords = {"algorithm", "function", "parameter", "variable", "method",
                "class", "object", "API", "database", "server", "client"};
        String lowerText = text.toLowerCase();
        return Arrays.stream(techKeywords).anyMatch(lowerText::contains);
    }

    private String getLastSentences(String text, int maxLength) {
        if (text.length() <= maxLength) return text;

        int pos = text.length() - maxLength;
        while (pos < text.length() && !Character.isWhitespace(text.charAt(pos))) {
            pos++;
        }

        return text.substring(pos).trim();
    }

    private boolean isHighQualitySegment(String text) {
        if (text.length() < 20) return false;
        if (text.length() > 2000) return false;

        String[] words = text.split("\\s+");
        if (words.length < 5) return false;

        if (text.replaceAll("[\\p{P}\\p{S}\\s\\d]", "").length() < text.length() * 0.3) {
            return false;
        }

        return true;
    }

    private double calculateQualityScore(String text) {
        if (text == null || text.trim().isEmpty()) {
            return 0.0;
        }

        double score = 0.0;

        try {
            // 基于长度的评分
            int length = text.length();
            if (length >= 100 && length <= 800) score += 0.3;

            // 基于词汇多样性的评分
            String[] words = text.toLowerCase().split("\\s+");
            Set<String> uniqueWords = new HashSet<>(Arrays.asList(words));
            double diversity = words.length > 0 ? (double) uniqueWords.size() / words.length : 0;
            score += diversity * 0.4;

            // 基于结构化内容的评分
            if (containsStructuralElement(text, null)) score += 0.2;

            // 基于技术内容的评分
            if (isTechnicalContent(text)) score += 0.1;

        } catch (Exception e) {
            log.debug("计算质量评分时发生错误", e);
            return 0.5;
        }

        return Math.min(1.0, score);
    }

    private boolean containsStructuralElement(String text, DocumentStructure structure) {
        if (structure == null || text == null || text.trim().isEmpty()) {
            return false;
        }

        try {
            return structure.getTitles().stream().anyMatch(title -> text.contains(title.getContent())) ||
                    structure.getListItems().stream().anyMatch(item -> text.contains(item.getContent()));
        } catch (Exception e) {
            log.debug("检查结构化元素时发生错误", e);
            return false;
        }
    }

    private String identifyStructureType(String text, DocumentStructure structure) {
        if (structure == null || text == null || text.trim().isEmpty()) {
            return "paragraph";
        }

        try {
            if (structure.getTitles().stream().anyMatch(title -> text.contains(title.getContent()))) {
                return "title";
            } else if (structure.getListItems().stream().anyMatch(item -> text.contains(item.getContent()))) {
                return "list";
            } else if (structure.getCodeBlocks().stream().anyMatch(code -> text.contains(code.getContent()))) {
                return "code";
            } else if (structure.getTables().stream().anyMatch(table -> text.contains(table.getContent()))) {
                return "table";
            }
        } catch (Exception e) {
            log.debug("识别结构类型时发生错误", e);
        }

        return "paragraph";
    }

    // ========================================
    // 摘要和关键词提取
    // ========================================

    private String generateDocumentSummary(List<TextSegment> segments) {
        try {
            List<TextSegment> keySegments = selectKeySegments(segments, 3);

            if (keySegments.isEmpty()) {
                return "无法生成文档摘要";
            }

            String combinedText = keySegments.stream()
                    .map(TextSegment::text)
                    .collect(Collectors.joining("\n\n"));

            if (combinedText.length() > 2000) {
                combinedText = combinedText.substring(0, 2000) + "...";
            }

            String prompt = "请为以下文档内容生成一个简洁的摘要（100-200字）：\n\n" + combinedText;

            ChatResponse response = chatLanguageModel.chat(
                    SystemMessage.from("你是一个专业的文档摘要生成助手，请生成准确、简洁的摘要。"),
                    UserMessage.from(prompt)
            );

            return cleanAndValidateSummary(response.aiMessage().text());

        } catch (Exception e) {
            log.error("生成文档摘要失败", e);
            return generateFallbackSummary(segments);
        }
    }

    private String generateSegmentSummary(String text) {
        try {
            if (text.length() < 200) {
                return "";
            }

            String[] sentences = text.split("[.!?。！？]");
            if (sentences.length >= 2) {
                String summary = sentences[0].trim();
                if (sentences.length > 1 && summary.length() < 100) {
                    summary += "。" + sentences[1].trim();
                }
                return summary.length() > 150 ? summary.substring(0, 150) + "..." : summary;
            }

            if (text.length() > 800) {
                return generateLLMSummary(text, 50);
            }

            return "";

        } catch (Exception e) {
            log.debug("生成段落摘要失败: {}", text.substring(0, Math.min(50, text.length())));
            return "";
        }
    }

    private String generateLLMSummary(String text, int maxLength) {
        try {
            String prompt = String.format("请为以下文本生成一个%d字以内的摘要：\n\n%s",
                    maxLength, text.length() > 1000 ? text.substring(0, 1000) + "..." : text);

            ChatResponse response = chatLanguageModel.chat(
                    SystemMessage.from("你是一个专业的文本摘要助手，请生成准确、简洁的摘要。"),
                    UserMessage.from(prompt)
            );

            return cleanAndValidateSummary(response.aiMessage().text());

        } catch (Exception e) {
            log.debug("LLM摘要生成失败", e);
            return "";
        }
    }

    private List<TextSegment> selectKeySegments(List<TextSegment> segments, int count) {
        return segments.stream()
                .sorted((a, b) -> {
                    double scoreA = calculateSegmentImportance(a);
                    double scoreB = calculateSegmentImportance(b);
                    return Double.compare(scoreB, scoreA);
                })
                .limit(count)
                .collect(Collectors.toList());
    }

    private double calculateSegmentImportance(TextSegment segment) {
        String text = segment.text();
        double score = 0.0;

        score += Math.min(text.length() / 500.0, 1.0) * 0.3;

        if (text.contains("#") || text.contains("##")) score += 0.4;
        if (text.matches(".*^\\s*[-*+]\\s+.*") || text.matches(".*^\\s*\\d+\\.\\s+.*")) score += 0.2;

        String[] importantWords = {"重要", "关键", "核心", "主要", "总结", "结论", "概述", "摘要"};
        long keywordCount = Arrays.stream(importantWords)
                .mapToLong(word -> text.toLowerCase().split(word).length - 1)
                .sum();
        score += Math.min(keywordCount / 10.0, 0.3);

        return score;
    }

    private String cleanAndValidateSummary(String summary) {
        if (summary == null || summary.trim().isEmpty()) {
            return "无摘要";
        }

        summary = summary.trim()
                .replaceAll("\\n+", " ")
                .replaceAll("\\s+", " ");

        if (summary.length() > 300) {
            summary = summary.substring(0, 300) + "...";
        }

        return summary;
    }

    private String generateFallbackSummary(List<TextSegment> segments) {
        int totalSegments = segments.size();
        int totalWords = segments.stream()
                .mapToInt(segment -> segment.text().split("\\s+").length)
                .sum();

        return String.format("文档包含%d个段落，共计约%d个词。", totalSegments, totalWords);
    }

    private List<String> extractKeywords(String text) {
        List<String> keywords = new ArrayList<>();

        try {
            String[] words = text.toLowerCase()
                    .replaceAll("[^\\w\\s]", " ")
                    .split("\\s+");

            Map<String, Integer> wordFreq = new HashMap<>();
            for (String word : words) {
                if (word.length() > 3 && !isStopWord(word)) {
                    wordFreq.put(word, wordFreq.getOrDefault(word, 0) + 1);
                }
            }

            keywords = wordFreq.entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                    .limit(5)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.debug("关键词提取失败", e);
        }

        return keywords;
    }

    private boolean isStopWord(String word) {
        Set<String> stopWords = Set.of(
                "的", "了", "在", "是", "我", "有", "和", "就", "不", "人", "都", "一", "一个", "上", "也", "很", "到", "说", "要", "去", "你", "会", "着", "没有", "看", "好", "自己", "这",
                "the", "and", "or", "but", "in", "on", "at", "to", "for", "of", "with", "by", "from", "this", "that", "these", "those", "a", "an", "is", "are", "was", "were", "be", "been", "being", "have", "has", "had", "do", "does", "did", "will", "would", "could", "should"
        );
        return stopWords.contains(word.toLowerCase());
    }

    private String classifyTopic(String text) {
        String lowerText = text.toLowerCase();

        if (lowerText.contains("技术") || lowerText.contains("开发") || lowerText.contains("code") || lowerText.contains("programming")) {
            return "技术";
        } else if (lowerText.contains("管理") || lowerText.contains("business") || lowerText.contains("策略")) {
            return "管理";
        } else if (lowerText.contains("教育") || lowerText.contains("学习") || lowerText.contains("education")) {
            return "教育";
        } else if (lowerText.contains("医疗") || lowerText.contains("健康") || lowerText.contains("medical")) {
            return "医疗";
        } else if (lowerText.contains("法律") || lowerText.contains("法规") || lowerText.contains("legal")) {
            return "法律";
        } else {
            return "通用";
        }
    }

    // ========================================
    // 分词处理
    // ========================================

    private List<String> performTokenization(String text) {
        try {
            int tokenCount = tokenizer.estimateTokenCountInText(text);
            log.debug("文本token估算数量: {}", tokenCount);

            return performDefaultTokenization(text);

        } catch (Exception e) {
            log.debug("分词处理失败，返回空列表: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    private List<String> performDefaultTokenization(String text) {
        if (text == null || text.trim().isEmpty()) {
            return new ArrayList<>();
        }

        List<String> tokens = new ArrayList<>();

        String cleanText = text.toLowerCase()
                .replaceAll("[\\p{P}\\p{S}&&[^\\u4e00-\\u9fa5]]+", " ")
                .replaceAll("\\s+", " ")
                .trim();

        String[] words = cleanText.split("\\s+");

        for (String word : words) {
            if (word.length() > 1) {
                if (containsChinese(word)) {
                    tokens.addAll(tokenizeChinese(word));
                } else {
                    tokens.add(word);
                }
            }
        }

        return tokens.stream()
                .filter(token -> token.length() > 0)
                .distinct()
                .collect(Collectors.toList());
    }

    private boolean containsChinese(String text) {
        return text.matches(".*[\\u4e00-\\u9fa5]+.*");
    }

    private List<String> tokenizeChinese(String chineseText) {
        List<String> tokens = new ArrayList<>();
        char[] chars = chineseText.toCharArray();

        // 单字分词
        for (char c : chars) {
            if (Character.toString(c).matches("[\\u4e00-\\u9fa5]")) {
                tokens.add(Character.toString(c));
            }
        }

        // 双字组合（bigram）
        for (int i = 0; i < chars.length - 1; i++) {
            if (Character.toString(chars[i]).matches("[\\u4e00-\\u9fa5]") &&
                    Character.toString(chars[i + 1]).matches("[\\u4e00-\\u9fa5]")) {
                tokens.add(String.valueOf(chars[i]) + String.valueOf(chars[i + 1]));
            }
        }

        return tokens;
    }

    // ========================================
    // 内部数据结构
    // ========================================

    /**
     * 文档结构类
     */
    private static class DocumentStructure {
        private final List<StructureElement> titles = new ArrayList<>();
        private final List<StructureElement> listItems = new ArrayList<>();
        private final List<StructureElement> codeBlocks = new ArrayList<>();
        private final List<StructureElement> tables = new ArrayList<>();

        public void addTitle(String content, int position) {
            titles.add(new StructureElement(content, position));
        }

        public void addListItem(String content, int position) {
            listItems.add(new StructureElement(content, position));
        }

        public void addCodeBlock(String content, int position) {
            codeBlocks.add(new StructureElement(content, position));
        }

        public void addTable(String content, int position) {
            tables.add(new StructureElement(content, position));
        }

        public List<StructureElement> getTitles() { return titles; }
        public List<StructureElement> getListItems() { return listItems; }
        public List<StructureElement> getCodeBlocks() { return codeBlocks; }
        public List<StructureElement> getTables() { return tables; }

        public boolean isEmpty() {
            return titles.isEmpty() && listItems.isEmpty() && codeBlocks.isEmpty() && tables.isEmpty();
        }
    }

    /**
     * 结构元素类
     */
    private static class StructureElement {
        private final String content;
        private final int position;

        public StructureElement(String content, int position) {
            this.content = content;
            this.position = position;
        }

        public String getContent() { return content; }
        public int getPosition() { return position; }
    }

    /**
     * 文档摘要报告类
     */
    public static class DocumentSummaryReport {
        private final String fileName;
        private final String summary;
        private final List<String> keywords;
        private final Map<String, Long> topicDistribution;

        public DocumentSummaryReport(String fileName, String summary, List<String> keywords, Map<String, Long> topicDistribution) {
            this.fileName = fileName;
            this.summary = summary;
            this.keywords = keywords;
            this.topicDistribution = topicDistribution;
        }

        public String getFileName() { return fileName; }
        public String getSummary() { return summary; }
        public List<String> getKeywords() { return keywords; }
        public Map<String, Long> getTopicDistribution() { return topicDistribution; }

        @Override
        public String toString() {
            return String.format(
                    "文档: %s\n摘要: %s\n关键词: %s\n主题分布: %s",
                    fileName, summary, String.join(", ", keywords), topicDistribution
            );
        }
    }
}