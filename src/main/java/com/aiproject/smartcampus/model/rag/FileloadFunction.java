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
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

/**
 * 智能文档加载和处理器 - 优化版
 * 优化重点：提高RAG检索准确率
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

    // === 线程池 ===
    private final ExecutorService executorService = Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors()
    );

    // === 常量配置 ===
    private static final String DEFAULT_DOCUMENT_PATH = "documents";
    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of(
            ".txt", ".md", ".pdf", ".docx", ".doc", ".rtf", ".html", ".htm", ".json", ".xml", ".csv"
    );

    // === 优化的分割参数 ===
    private static final int DEFAULT_CHUNK_SIZE = 500;
    private static final int DEFAULT_OVERLAP_SIZE = 100;
    private static final int MIN_CHUNK_SIZE = 100;
    private static final int MAX_CHUNK_SIZE = 1500;

    // === 文档结构识别正则表达式 ===
    private static final Pattern TITLE_PATTERN = Pattern.compile(
            "^#{1,6}\\s+(.+)$|^(.+)\\n[=-]{3,}$", Pattern.MULTILINE);
    private static final Pattern LIST_PATTERN = Pattern.compile(
            "^[\\s]*[-*+•]\\s+(.+)$|^[\\s]*\\d+[.)］】]\\s+(.+)$", Pattern.MULTILINE);
    private static final Pattern CODE_PATTERN = Pattern.compile(
            "```[\\s\\S]*?```|`[^`]+`");
    private static final Pattern TABLE_PATTERN = Pattern.compile(
            "\\|.*\\|.*\\n\\|[-:| ]+\\|");
    private static final Pattern CHINESE_SENTENCE_PATTERN = Pattern.compile(
            "[。！？；]");
    private static final Pattern ENGLISH_SENTENCE_PATTERN = Pattern.compile(
            "[.!?]+\\s+");

    // === 缓存 ===
    private final Map<String, List<TextSegment>> documentCache = new ConcurrentHashMap<>();
    private final Map<String, Long> cacheTimestamps = new ConcurrentHashMap<>();
    private static final long CACHE_EXPIRY_MS = 24 * 60 * 60 * 1000;

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
     * 获取文档统计信息
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
        cacheTimestamps.clear();
        log.info("文档缓存已清理");
    }

    // ========================================
    // 核心处理方法 - 优化版
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
            if (isCacheValid(fileKey, file)) {
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
     * 处理单个文档的核心逻辑 - 优化版
     */
    private void processDocument(Document document) {
        try {
            String fileName = getDocumentFileName(document);
            log.debug("开始处理文档: {}, 内容长度: {}", fileName, document.text().length());

            // 1. 检测文档语言
            boolean isChinese = detectChineseContent(document.text());

            // 2. 智能识别文档结构
            DocumentStructure structure = analyzeDocumentStructureEnhanced(document);

            // 3. 优化的文档分割
            List<TextSegment> segments = performOptimizedSplitting(document, structure, isChinese);

            if (segments.isEmpty()) {
                log.warn("文档分割后无有效段落: {}", fileName);
                return;
            }

            // 4. 生成文档级摘要
            String documentSummary = generateDocumentSummary(segments);

            // 5. 增强文本段落
            List<TextSegment> enhancedSegments = enhanceTextSegmentsOptimized(
                    segments, structure, documentSummary, isChinese);

            // 6. 生成和存储向量嵌入
            generateAndStoreEmbeddings(enhancedSegments);

            // 7. 缓存处理结果
            String fileKey = generateDocumentKey(document);
            documentCache.put(fileKey, enhancedSegments);
            cacheTimestamps.put(fileKey, System.currentTimeMillis());

            log.info("文档处理完成: {}, 生成段落: {}", fileName, enhancedSegments.size());

        } catch (Exception e) {
            String fileName = getDocumentFileName(document);
            log.error("处理文档时发生错误: {}", fileName, e);
        }
    }

    // ========================================
    // 语言检测
    // ========================================

    /**
     * 检测是否包含中文内容
     */
    private boolean detectChineseContent(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }

        // 取样本检测
        String sample = text.length() > 1000 ? text.substring(0, 1000) : text;
        long chineseChars = sample.chars().filter(ch -> ch >= 0x4e00 && ch <= 0x9fa5).count();

        return chineseChars > sample.length() * 0.1; // 超过10%认为是中文文档
    }

    // ========================================
    // 文档结构分析 - 增强版
    // ========================================

    /**
     * 增强的文档结构分析
     */
    private DocumentStructure analyzeDocumentStructureEnhanced(Document document) {
        DocumentStructure structure = new DocumentStructure();

        if (document == null || document.text() == null || document.text().trim().isEmpty()) {
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

            // 识别关键段落（首尾段落等）
            identifyKeyParagraphs(content, structure);

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
        Matcher matcher = TITLE_PATTERN.matcher(content);
        while (matcher.find()) {
            String title = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
            if (title != null && !title.trim().isEmpty()) {
                structure.addTitle(title.trim(), matcher.start());
            }
        }
    }

    private void extractLists(String content, DocumentStructure structure) {
        Matcher matcher = LIST_PATTERN.matcher(content);
        while (matcher.find()) {
            String listItem = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
            if (listItem != null && !listItem.trim().isEmpty()) {
                structure.addListItem(listItem.trim(), matcher.start());
            }
        }
    }

    private void extractCodeBlocks(String content, DocumentStructure structure) {
        Matcher matcher = CODE_PATTERN.matcher(content);
        while (matcher.find()) {
            structure.addCodeBlock(matcher.group(), matcher.start());
        }
    }

    private void extractTables(String content, DocumentStructure structure) {
        Matcher matcher = TABLE_PATTERN.matcher(content);
        while (matcher.find()) {
            structure.addTable(matcher.group(), matcher.start());
        }
    }

    /**
     * 识别关键段落
     */
    private void identifyKeyParagraphs(String content, DocumentStructure structure) {
        String[] paragraphs = content.split("\\n\\n+");

        // 标记首段
        if (paragraphs.length > 0 && paragraphs[0].length() > 50) {
            structure.addKeyParagraph(paragraphs[0], 0, "introduction");
        }

        // 标记末段
        if (paragraphs.length > 1) {
            String lastPara = paragraphs[paragraphs.length - 1];
            if (lastPara.length() > 50) {
                structure.addKeyParagraph(lastPara, content.lastIndexOf(lastPara), "conclusion");
            }
        }

        // 标记包含关键词的段落
        String[] keywords = {"总结", "结论", "概述", "摘要", "summary", "conclusion", "abstract"};
        for (String para : paragraphs) {
            String lower = para.toLowerCase();
            for (String keyword : keywords) {
                if (lower.contains(keyword)) {
                    structure.addKeyParagraph(para, content.indexOf(para), "key_content");
                    break;
                }
            }
        }
    }

    // ========================================
    // 优化的文本分割
    // ========================================

    /**
     * 执行优化的文档分割
     */
    private List<TextSegment> performOptimizedSplitting(Document document, DocumentStructure structure,
                                                        boolean isChinese) {
        List<TextSegment> segments = new ArrayList<>();
        String content = document.text();

        // 确定最优块大小
        int chunkSize = determineOptimalChunkSize(content, structure, isChinese);
        int overlapSize = Math.min(chunkSize / 5, 150);

        // 1. 先按结构分割成大段
        List<String> sections = splitByStructure(content, structure);

        // 2. 对每个大段进行语义分割
        for (int sectionIdx = 0; sectionIdx < sections.size(); sectionIdx++) {
            String section = sections.get(sectionIdx);
            List<TextSegment> sectionSegments = splitSectionSemantically(
                    section, chunkSize, overlapSize, isChinese, sectionIdx);
            segments.addAll(sectionSegments);
        }

        // 3. 优化段落边界
        segments = optimizeSegmentBoundaries(segments);

        return segments;
    }

    /**
     * 确定最优块大小
     */
    private int determineOptimalChunkSize(String content, DocumentStructure structure, boolean isChinese) {
        int baseSize = DEFAULT_CHUNK_SIZE;

        // 根据文档类型调整
        if (!structure.getCodeBlocks().isEmpty()) {
            baseSize = 800; // 代码文档需要更大的块
        } else if (!structure.getTables().isEmpty()) {
            baseSize = 1000; // 包含表格的文档
        }

        // 根据语言调整
        if (isChinese) {
            baseSize = (int) (baseSize * 0.8); // 中文文档稍小
        }

        // 根据文档长度调整
        if (content.length() > 50000) {
            baseSize = (int) (baseSize * 1.2);
        } else if (content.length() < 5000) {
            baseSize = (int) (baseSize * 0.8);
        }

        return Math.min(Math.max(baseSize, MIN_CHUNK_SIZE), MAX_CHUNK_SIZE);
    }

    /**
     * 基于结构分割文档
     */
    private List<String> splitByStructure(String content, DocumentStructure structure) {
        List<String> sections = new ArrayList<>();

        if (structure.getTitles().isEmpty()) {
            // 没有标题结构，按段落分割
            String[] paragraphs = content.split("\\n\\n+");
            StringBuilder currentSection = new StringBuilder();

            for (String para : paragraphs) {
                if (currentSection.length() + para.length() > MAX_CHUNK_SIZE * 2) {
                    if (currentSection.length() > 0) {
                        sections.add(currentSection.toString());
                        currentSection = new StringBuilder();
                    }
                }
                currentSection.append(para).append("\n\n");
            }

            if (currentSection.length() > 0) {
                sections.add(currentSection.toString());
            }
        } else {
            // 按标题分割
            List<StructureElement> titles = new ArrayList<>(structure.getTitles());
            titles.sort(Comparator.comparingInt(StructureElement::getPosition));

            int lastPos = 0;
            for (int i = 0; i < titles.size(); i++) {
                int endPos = (i < titles.size() - 1) ?
                        titles.get(i + 1).getPosition() : content.length();

                if (endPos > lastPos) {
                    sections.add(content.substring(lastPos, endPos));
                    lastPos = endPos;
                }
            }
        }

        return sections;
    }

    /**
     * 语义分割段落
     */
    private List<TextSegment> splitSectionSemantically(String section, int chunkSize,
                                                       int overlapSize, boolean isChinese, int sectionIdx) {
        List<TextSegment> segments = new ArrayList<>();

        // 分句
        List<String> sentences = splitIntoSentences(section, isChinese);

        if (sentences.isEmpty()) {
            return segments;
        }

        // 滑动窗口创建段落
        StringBuilder currentChunk = new StringBuilder();
        List<String> currentSentences = new ArrayList<>();
        int currentLength = 0;

        for (String sentence : sentences) {
            int sentenceLength = estimateLength(sentence);

            if (currentLength + sentenceLength > chunkSize && !currentSentences.isEmpty()) {
                // 创建段落
                Metadata metadata = new Metadata();
                metadata.put("section_index", String.valueOf(sectionIdx));
                metadata.put("chunk_size", String.valueOf(currentChunk.length()));
                segments.add(TextSegment.from(currentChunk.toString().trim(), metadata));

                // 处理重叠
                currentChunk = new StringBuilder();
                currentSentences = new ArrayList<>();
                currentLength = 0;

                // 从后向前添加句子作为重叠
                int overlapLength = 0;
                for (int i = segments.size() - 1; i >= 0 && overlapLength < overlapSize; i--) {
                    String lastSentence = sentences.get(Math.max(0, sentences.indexOf(sentence) - 1));
                    if (lastSentence != null && overlapLength + lastSentence.length() <= overlapSize) {
                        currentChunk.append(lastSentence).append(" ");
                        currentSentences.add(lastSentence);
                        overlapLength += lastSentence.length();
                    }
                }
                currentLength = overlapLength;
            }

            currentChunk.append(sentence).append(" ");
            currentSentences.add(sentence);
            currentLength += sentenceLength;
        }

        // 添加最后一个段落
        if (!currentSentences.isEmpty()) {
            Metadata metadata = new Metadata();
            metadata.put("section_index", String.valueOf(sectionIdx));
            metadata.put("chunk_size", String.valueOf(currentChunk.length()));
            segments.add(TextSegment.from(currentChunk.toString().trim(), metadata));
        }

        return segments;
    }

    /**
     * 分句
     */
    private List<String> splitIntoSentences(String text, boolean isChinese) {
        List<String> sentences = new ArrayList<>();

        if (text == null || text.trim().isEmpty()) {
            return sentences;
        }

        Pattern pattern = isChinese ? CHINESE_SENTENCE_PATTERN : ENGLISH_SENTENCE_PATTERN;
        Matcher matcher = pattern.matcher(text);

        int lastEnd = 0;
        while (matcher.find()) {
            String sentence = text.substring(lastEnd, matcher.end()).trim();
            if (sentence.length() > 10) {
                sentences.add(sentence);
            }
            lastEnd = matcher.end();
        }

        // 添加最后一部分
        if (lastEnd < text.length()) {
            String lastSentence = text.substring(lastEnd).trim();
            if (lastSentence.length() > 10) {
                sentences.add(lastSentence);
            }
        }

        // 如果没有找到句子，直接返回整个文本
        if (sentences.isEmpty() && text.length() > 10) {
            sentences.add(text);
        }

        return sentences;
    }

    /**
     * 优化段落边界
     */
    private List<TextSegment> optimizeSegmentBoundaries(List<TextSegment> segments) {
        List<TextSegment> optimized = new ArrayList<>();

        for (int i = 0; i < segments.size(); i++) {
            TextSegment current = segments.get(i);
            String text = current.text().trim();

            // 检查段落是否太短
            if (text.length() < MIN_CHUNK_SIZE && i < segments.size() - 1) {
                // 尝试与下一个段落合并
                TextSegment next = segments.get(i + 1);
                String merged = text + "\n" + next.text();
                if (merged.length() <= MAX_CHUNK_SIZE) {
                    // 创建新的元数据，复制当前段落的元数据
                    Metadata mergedMetadata = new Metadata();
                    // 复制当前段落的元数据
                    Map<String, Object> currentMap = current.metadata().toMap();
                    for (Map.Entry<String, Object> entry : currentMap.entrySet()) {
                        mergedMetadata.put(entry.getKey(), entry.getValue().toString());
                    }
                    optimized.add(TextSegment.from(merged, mergedMetadata));
                    i++; // 跳过下一个段落
                    continue;
                }
            }

            optimized.add(current);
        }

        return optimized;
    }

    // ========================================
    // 文本增强
    // ========================================

    /**
     * 增强文本段落 - 优化版
     */
    private List<TextSegment> enhanceTextSegmentsOptimized(List<TextSegment> segments,
                                                           DocumentStructure structure,
                                                           String documentSummary,
                                                           boolean isChinese) {
        List<TextSegment> enhanced = new ArrayList<>();

        for (int i = 0; i < segments.size(); i++) {
            TextSegment segment = segments.get(i);
            Metadata metadata = segment.metadata();
            String text = segment.text();

            // 添加文档级信息
            metadata.put("document_summary", documentSummary);
            metadata.put("is_chinese", String.valueOf(isChinese));

            // 添加位置信息
            metadata.put("position", String.valueOf(i));
            metadata.put("total_segments", String.valueOf(segments.size()));
            metadata.put("position_ratio", String.format("%.2f", (double) i / segments.size()));

            // 添加结构信息
            addStructuralInfo(metadata, text, structure);

            // 添加内容特征
            addContentFeatures(metadata, text);

            // 添加上下文
            if (i > 0) {
                String prevText = segments.get(i - 1).text();
                metadata.put("prev_context", prevText.substring(0, Math.min(100, prevText.length())));
            }
            if (i < segments.size() - 1) {
                String nextText = segments.get(i + 1).text();
                metadata.put("next_context", nextText.substring(0, Math.min(100, nextText.length())));
            }

            // 提取并添加关键词
            List<String> keywords = extractKeywords(text);
            if (!keywords.isEmpty()) {
                metadata.put("keywords", String.join(", ", keywords));
            }

            // 计算质量分数
            double qualityScore = calculateQualityScore(text, i, segments.size());
            metadata.put("quality_score", String.format("%.3f", qualityScore));

            enhanced.add(TextSegment.from(text, metadata));
        }

        return enhanced;
    }

    /**
     * 添加结构信息
     */
    private void addStructuralInfo(Metadata metadata, String text, DocumentStructure structure) {
        // 查找最近的标题
        String nearestTitle = findNearestTitle(text, structure);
        if (nearestTitle != null) {
            metadata.put("section_title", nearestTitle);
        }

        // 检查内容类型
        if (CODE_PATTERN.matcher(text).find()) {
            metadata.put("contains_code", "true");
        }
        if (TABLE_PATTERN.matcher(text).find()) {
            metadata.put("contains_table", "true");
        }
        if (LIST_PATTERN.matcher(text).find()) {
            metadata.put("contains_list", "true");
        }

        // 检查是否是关键段落
        for (KeyParagraph kp : structure.getKeyParagraphs()) {
            if (text.contains(kp.getContent().substring(0, Math.min(50, kp.getContent().length())))) {
                metadata.put("is_key_paragraph", "true");
                metadata.put("paragraph_type", kp.getType());
                break;
            }
        }
    }

    /**
     * 添加内容特征
     */
    private void addContentFeatures(Metadata metadata, String text) {
        // 字数统计
        metadata.put("word_count", String.valueOf(text.split("\\s+").length));
        metadata.put("char_count", String.valueOf(text.length()));

        // 检查是否包含问题
        if (text.contains("?") || text.contains("？")) {
            metadata.put("contains_question", "true");
        }

        // 检查是否包含数字
        if (text.matches(".*\\d+.*")) {
            metadata.put("contains_numbers", "true");
        }

        // 检查是否是定义
        if (text.contains("是指") || text.contains("定义为") || text.contains("is defined as") ||
                text.contains("refers to")) {
            metadata.put("is_definition", "true");
        }
    }

    /**
     * 查找最近的标题
     */
    private String findNearestTitle(String text, DocumentStructure structure) {
        if (structure.getTitles().isEmpty()) {
            return null;
        }

        // 简单实现：返回第一个标题
        // 实际应该根据位置查找最近的
        return structure.getTitles().getFirst().getContent();
    }

    /**
     * 提取关键词
     */
    private List<String> extractKeywords(String text) {
        List<String> keywords = new ArrayList<>();

        try {
            // 简单的关键词提取：基于词频
            String[] words = text.toLowerCase()
                    .replaceAll("[^\\w\\s\\u4e00-\\u9fa5]", " ")
                    .split("\\s+");

            Map<String, Integer> wordFreq = new HashMap<>();
            for (String word : words) {
                if (word.length() > 2 && !isStopWord(word)) {
                    wordFreq.put(word, wordFreq.getOrDefault(word, 0) + 1);
                }
            }

            // 取频率最高的5个词
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

    /**
     * 判断是否是停用词
     */
    private boolean isStopWord(String word) {
        Set<String> stopWords = Set.of(
                "的", "了", "在", "是", "我", "有", "和", "就", "不", "人", "都", "一", "一个",
                "the", "and", "or", "but", "in", "on", "at", "to", "for", "of", "with", "by",
                "is", "are", "was", "were", "be", "been", "being", "have", "has", "had"
        );
        return stopWords.contains(word.toLowerCase());
    }

    /**
     * 计算质量分数
     */
    private double calculateQualityScore(String text, int position, int totalSegments) {
        double score = 0.5; // 基础分

        // 长度评分
        int length = text.length();
        if (length >= 200 && length <= 800) {
            score += 0.2;
        } else if (length >= 100 && length <= 1000) {
            score += 0.1;
        }

        // 位置评分（首尾段落更重要）
        if (position == 0 || position == totalSegments - 1) {
            score += 0.15;
        } else if (position < 3 || position > totalSegments - 4) {
            score += 0.1;
        }

        // 内容丰富度评分
        if (text.contains("？") || text.contains("?")) score += 0.05;
        if (text.matches(".*\\d+.*")) score += 0.05;
        if (text.split("\\s+").length > 50) score += 0.05;

        return Math.min(score, 1.0);
    }

    // ========================================
    // 向量嵌入生成和存储
    // ========================================

    /**
     * 生成和存储嵌入
     */
    private void generateAndStoreEmbeddings(List<TextSegment> segments) {
        // 批量处理
        int batchSize = 10;

        for (int i = 0; i < segments.size(); i += batchSize) {
            int end = Math.min(i + batchSize, segments.size());
            List<TextSegment> batch = segments.subList(i, end);

            try {
                // 准备增强的文本段落
                List<TextSegment> enhancedBatch = new ArrayList<>();
                for (TextSegment segment : batch) {
                    String enhancedText = prepareEmbeddingText(segment);
                    // 创建新的TextSegment，包含增强的文本但保留原始元数据
                    enhancedBatch.add(TextSegment.from(enhancedText, segment.metadata()));
                }

                // 批量生成嵌入
                List<Embedding> embeddings = embeddingModel.embedAll(enhancedBatch).content();

                // 存储（使用原始段落，而不是增强版本）
                for (int j = 0; j < batch.size(); j++) {
                    try {
                        embeddingStore.add(embeddings.get(j), batch.get(j));
                        log.debug("成功存储段落 {} 的嵌入", i + j);
                    } catch (Exception e) {
                        log.error("存储嵌入失败", e);
                    }
                }

            } catch (Exception e) {
                log.error("批量生成嵌入失败", e);
                // 降级到单个处理
                for (TextSegment segment : batch) {
                    try {
                        // 创建增强文本的TextSegment
                        String enhancedText = prepareEmbeddingText(segment);
                        TextSegment enhancedSegment = TextSegment.from(enhancedText, segment.metadata());

                        Embedding embedding = embeddingModel.embed(enhancedSegment).content();
                        embeddingStore.add(embedding, segment);
                    } catch (Exception ex) {
                        log.error("单个嵌入生成失败", ex);
                    }
                }
            }
        }
    }

    /**
     * 准备嵌入文本
     */
    private String prepareEmbeddingText(TextSegment segment) {
        StringBuilder text = new StringBuilder();

        // 添加标题上下文
        Map<String, Object> metadataMap = segment.metadata().toMap();
        Object sectionTitle = metadataMap.get("section_title");
        if (sectionTitle != null && !sectionTitle.toString().isEmpty()) {
            text.append("[标题: ").append(sectionTitle).append("] ");
        }

        // 主要内容
        text.append(segment.text());

        // 添加关键词
        Object keywords = metadataMap.get("keywords");
        if (keywords != null && !keywords.toString().isEmpty()) {
            text.append(" [关键词: ").append(keywords).append("]");
        }

        return text.toString();
    }

    // ========================================
    // 摘要生成
    // ========================================

    /**
     * 生成文档摘要
     */
    private String generateDocumentSummary(List<TextSegment> segments) {
        try {
            // 选择关键段落
            List<String> keyTexts = new ArrayList<>();

            // 添加第一个段落
            if (!segments.isEmpty()) {
                keyTexts.add(segments.get(0).text());
            }

            // 添加最后一个段落
            if (segments.size() > 1) {
                keyTexts.add(segments.get(segments.size() - 1).text());
            }

            // 添加中间的关键段落
            for (TextSegment segment : segments) {
                Map<String, Object> metadataMap = segment.metadata().toMap();
                Object isKey = metadataMap.get("is_key_paragraph");
                if (isKey != null && "true".equals(isKey.toString()) && keyTexts.size() < 5) {
                    keyTexts.add(segment.text());
                }
            }

            if (keyTexts.isEmpty()) {
                return "文档摘要生成失败";
            }

            // 合并文本
            String combinedText = String.join("\n\n", keyTexts);
            if (combinedText.length() > 2000) {
                combinedText = combinedText.substring(0, 2000);
            }

            // 使用LLM生成摘要
            String prompt = "请为以下文档内容生成一个100-200字的摘要：\n\n" + combinedText;

            ChatResponse response = chatLanguageModel.chat(
                    SystemMessage.from("你是一个专业的文档摘要生成助手。"),
                    UserMessage.from(prompt)
            );

            return response.aiMessage().text();

        } catch (Exception e) {
            log.error("生成文档摘要失败", e);
            return "文档包含" + segments.size() + "个段落";
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

    private boolean isCacheValid(String fileKey, File file) {
        if (!documentCache.containsKey(fileKey)) {
            return false;
        }

        Long cacheTime = cacheTimestamps.get(fileKey);
        if (cacheTime == null) {
            return false;
        }

        // 检查缓存是否过期
        if (System.currentTimeMillis() - cacheTime > CACHE_EXPIRY_MS) {
            documentCache.remove(fileKey);
            cacheTimestamps.remove(fileKey);
            return false;
        }

        // 检查文件是否被修改
        if (file.lastModified() > cacheTime) {
            documentCache.remove(fileKey);
            cacheTimestamps.remove(fileKey);
            return false;
        }

        return true;
    }

    private int estimateLength(String text) {
        if (text == null || text.isEmpty()) return 0;

        try {
            return tokenizer.estimateTokenCountInText(text);
        } catch (Exception e) {
            // 简单估算
            return text.length() / 3;
        }
    }

    /**
     * 清理资源
     */
    public void cleanup() {
        try {
            executorService.shutdown();
            documentCache.clear();
            cacheTimestamps.clear();
            log.info("资源清理完成");
        } catch (Exception e) {
            log.error("资源清理失败", e);
        }
    }

    // ========================================
    // 内部类
    // ========================================

    /**
     * 文档结构类
     */
    private static class DocumentStructure {
        private final List<StructureElement> titles = new ArrayList<>();
        private final List<StructureElement> listItems = new ArrayList<>();
        private final List<StructureElement> codeBlocks = new ArrayList<>();
        private final List<StructureElement> tables = new ArrayList<>();
        private final List<KeyParagraph> keyParagraphs = new ArrayList<>();

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

        public void addKeyParagraph(String content, int position, String type) {
            keyParagraphs.add(new KeyParagraph(content, position, type));
        }

        public List<StructureElement> getTitles() {
            return titles;
        }

        public List<StructureElement> getListItems() {
            return listItems;
        }

        public List<StructureElement> getCodeBlocks() {
            return codeBlocks;
        }

        public List<StructureElement> getTables() {
            return tables;
        }

        public List<KeyParagraph> getKeyParagraphs() {
            return keyParagraphs;
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

        public String getContent() {
            return content;
        }

        public int getPosition() {
            return position;
        }
    }

    /**
     * 关键段落类
     */
    private static class KeyParagraph {
        private final String content;
        private final int position;
        private final String type;

        public KeyParagraph(String content, int position, String type) {
            this.content = content;
            this.position = position;
            this.type = type;
        }

        public String getContent() {
            return content;
        }

        public int getPosition() {
            return position;
        }

        public String getType() {
            return type;
        }
    }
}