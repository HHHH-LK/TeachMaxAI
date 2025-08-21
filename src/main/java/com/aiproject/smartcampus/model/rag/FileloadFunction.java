package com.aiproject.smartcampus.model.rag;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentParser;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.data.document.parser.apache.pdfbox.ApachePdfBoxDocumentParser;
import dev.langchain4j.data.document.parser.apache.poi.ApachePoiDocumentParser;
import dev.langchain4j.data.document.parser.apache.tika.ApacheTikaDocumentParser;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * 智能文档加载和处理器 - 动态解析器和分割器版本
 * 使用Apache Tika作为主要解析器，支持1000+种文件格式
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
    private final EmbeddingStore<TextSegment> embeddingStore; // 使用泛型，避免Raw type
    private final ChatLanguageModel chatLanguageModel; // 可选使用生成摘要
    private final LocalTokenizerFuncation tokenizer;

    // === 解析器实例（复用） ===
    private final ApacheTikaDocumentParser tikaParser = new ApacheTikaDocumentParser();
    private final TextDocumentParser textParser = new TextDocumentParser();
    private final ApachePdfBoxDocumentParser pdfBoxParser = new ApachePdfBoxDocumentParser();
    private final ApachePoiDocumentParser poiParser = new ApachePoiDocumentParser();

    // === 文件类型枚举 ===
    public enum FileType {
        PDF(".pdf"), WORD_DOCX(".docx"), WORD_DOC(".doc"), TEXT(".txt"), MARKDOWN(".md"), HTML(".html", ".htm"), EXCEL(".xlsx", ".xls"), CSV(".csv"), JSON(".json"), XML(".xml"), RTF(".rtf"), PPT(".pptx", ".ppt"), CODE(".java", ".py", ".js", ".cpp", ".c", ".go", ".rs", ".ts", ".jsx", ".tsx"), UNKNOWN("");

        private final String[] extensions;

        FileType(String... extensions) {
            this.extensions = extensions;
        }

        public static FileType fromFileName(String fileName) {
            String lowerName = fileName.toLowerCase();
            for (FileType type : values()) {
                for (String ext : type.extensions) {
                    if (lowerName.endsWith(ext)) {
                        return type;
                    }
                }
            }
            return UNKNOWN;
        }
    }

    // === 缓存 ===
    private final Map<String, List<TextSegment>> documentCache = new ConcurrentHashMap<>();
    // 缓存创建时间（用于TTL）
    private final Map<String, Long> cacheTimestamps = new ConcurrentHashMap<>();
    // 文件的最后修改时间（用于变更校验）
    private final Map<String, Long> cacheLastModified = new ConcurrentHashMap<>();
    private static final long CACHE_EXPIRY_MS = 24 * 60 * 60 * 1000;

    // ========================================
    // 主入口方法
    // ========================================

    /**
     * 动态处理文档 - 根据文件类型自动选择最佳策略
     */
    public void processDocumentDynamically(File file) {
        if (file == null || !file.exists()) {
            log.error("文件不存在: {}", file);
            return;
        }

        try {
            // 1. 识别文件类型
            FileType fileType = FileType.fromFileName(file.getName());
            log.info("识别文件类型: {} -> {}", file.getName(), fileType);

            // 2. 检查缓存
            String fileKey = generateFileKey(file);
            if (isCacheValid(fileKey, file)) {
                log.info("使用缓存的文档: {}", file.getName());
                return;
            }

            // 3. 选择合适的解析器并解析（按候选解析器顺序尝试，失败回退）
            Document document = parseWithOptimalParser(file, fileType);
            if (document == null || document.text() == null || document.text().trim().isEmpty()) {
                log.error("文档解析失败或内容为空: {}", file.getName());
                return;
            }

            // 4. 记录文档信息
            logDocumentInfo(document, fileType);

            // 5. 使用动态分割策略
            List<TextSegment> segments = splitWithOptimalStrategy(document, fileType);

            // 6. 增强、生成嵌入并存储、缓存
            processSegments(segments, document, fileType, fileKey, file);

            log.info("文档处理完成: {}, 类型: {}, 生成段落: {}", file.getName(), fileType, segments.size());

        } catch (Exception e) {
            log.error("处理文档失败: {}", file.getName(), e);
        }
    }

    // ========================================
    // 动态解析器选择
    // ========================================

    /**
     * 根据文件类型选择最优解析器（候选列表逐一尝试）
     * 策略：
     * 1. 文本类文件优先 TextDocumentParser（更快）
     * 2. PDF 优先 PDFBox，失败回退 Tika
     * 3. Office 优先 POI，失败回退 Tika
     * 4. 其他格式使用 Tika
     */
    private Document parseWithOptimalParser(File file, FileType fileType) {
        List<DocumentParser> candidates = new ArrayList<>();
        switch (fileType) {
            case TEXT:
            case MARKDOWN:
            case CODE:
            case CSV:
            case JSON:
            case XML:
                candidates.add(textParser);
                break;
            case PDF:
                candidates.add(pdfBoxParser);
                candidates.add(tikaParser);
                break;
            case WORD_DOCX:
            case WORD_DOC:
            case EXCEL:
            case PPT:
                candidates.add(poiParser);
                candidates.add(tikaParser);
                break;
            default:
                candidates.add(tikaParser);
        }

        for (DocumentParser parser : candidates) {
            try {
                log.info("尝试使用解析器 {} 处理: {}", parser.getClass().getSimpleName(), file.getName());
                Document document = FileSystemDocumentLoader.loadDocument(file.toPath(), parser);
                if (document != null && document.text() != null && !document.text().trim().isEmpty()) {
                    // 增强元数据
                    Metadata md = document.metadata();
                    if (md == null) {
                        md = new Metadata();
                        document = Document.from(document.text(), md);
                    }
                    md.put("file_type", fileType.toString());
                    md.put("file_name", file.getName());
                    md.put("file_path", file.getAbsolutePath());
                    md.put("file_size", String.valueOf(file.length()));
                    md.put("parse_time", String.valueOf(System.currentTimeMillis()));
                    md.put("parser", parser.getClass().getSimpleName());
                    return document;
                }
            } catch (Exception e) {
                log.warn("解析器 {} 处理失败，将尝试下一个解析器。原因: {}", parser.getClass().getSimpleName(), e.getMessage());
            }
        }

        log.error("所有解析器均失败: {}", file.getName());
        return null;
    }

    // ========================================
    // 动态分割策略
    // ========================================

    /**
     * 根据文件类型选择最优分割策略
     */
    private List<TextSegment> splitWithOptimalStrategy(Document document, FileType fileType) {
        String content = document.text();

        log.info("为文件类型 {} 选择分割策略，内容长度: {}", fileType, content.length());

        // 如果内容很短，直接返回一个段落（克隆元数据，避免共享引用）
        if (content.length() < 200) {
            return Collections.singletonList(TextSegment.from(content, cloneMetadata(document.metadata())));
        }

        switch (fileType) {
            case PDF:
                return splitPDFDocument(document);
            case WORD_DOCX:
            case WORD_DOC:
                return splitWordDocument(document);
            case CODE:
                return splitCodeDocument(document);
            case MARKDOWN:
                return splitMarkdownDocument(document);
            case EXCEL:
            case CSV:
                return splitTableDocument(document);
            case JSON:
            case XML:
                return splitStructuredDocument(document);
            case PPT:
                return splitPresentationDocument(document);
            default:
                return splitGenericDocument(document);
        }
    }

    /**
     * PDF文档分割策略
     */
    private List<TextSegment> splitPDFDocument(Document document) {
        log.info("使用PDF专用分割策略");
        String content = preprocessPdfText(document.text());
        boolean isChinese = detectChineseContent(content);
        ChunkPolicy policy = getPolicy(FileType.PDF, isChinese);
        int overlap = 50;

        String[] paras = content.split("\\n\\n+");
        if (paras.length < 5) paras = content.split("\\n+");

        List<String> paragraphs = new ArrayList<>();
        for (String p : paras) {
            String s = p == null ? "" : p.trim();
            if (!s.isEmpty()) paragraphs.add(s);
        }
        if (paragraphs.size() < 10 && content.length() > 4000) {
            paragraphs = splitIntoSentences(content, isChinese);
        }

        List<String> normalized = normalizeParagraphs(paragraphs, isChinese, policy);
        List<TextSegment> segments = mergeIntoChunks(normalized, policy, overlap, "pdf", "pdf");

        if (segments.size() < 5 && estimateLength(content) > policy.maxTokens * 5) {
            log.warn("PDF分割结果过少（{}段），回退固定长度切分", segments.size());
            return performFixedLengthSplitting(content, policy.targetTokens, overlap);
        }
        return segments;
    }

    /**
     * Word文档分割策略
     */
    private List<TextSegment> splitWordDocument(Document document) {
        log.info("使用Word文档分割策略");

        String content = document.text();
        boolean isChinese = detectChineseContent(content);
        ChunkPolicy policy = getPolicy(FileType.WORD_DOCX, isChinese);
        int overlap = 100;

        String[] paras = content.split("\\n\\n+");
        if (paras.length < 5) paras = content.split("\\n+");

        List<String> raw = new ArrayList<>();
        for (String p : paras) {
            if (p != null && !p.trim().isEmpty()) raw.add(p.trim());
        }

        List<String> normalized = normalizeParagraphs(raw, isChinese, policy);
        return mergeIntoChunks(normalized, policy, overlap, "word", "word");
    }

    /**
     * 代码文档分割策略（按函数/类/花括号聚合，后续再按策略合并）
     */
    private List<TextSegment> splitCodeDocument(Document document) {
        log.info("使用代码文档分割策略");

        String content = document.text();
        boolean isChinese = detectChineseContent(content);
        ChunkPolicy policy = getPolicy(FileType.CODE, isChinese);
        int overlap = 80;

        String[] lines = content.split("\\r?\\n");
        List<String> blocks = new ArrayList<>();

        StringBuilder current = new StringBuilder();
        int braceCount = 0;

        Pattern blockStart = Pattern.compile("^\\s*(public|private|protected|class\\b|interface\\b|enum\\b|def\\b|function\\b|async\\s+function\\b|module\\b)\\b.*");

        for (String line : lines) {
            String trimmed = line.trim();
            boolean isBlockStart = blockStart.matcher(trimmed).find() || trimmed.endsWith("{");

            if (isBlockStart && current.length() > 0 && braceCount == 0) {
                blocks.add(current.toString().trim());
                current.setLength(0);
            }

            current.append(line).append('\n');

            // 更新花括号平衡
            braceCount += countChar(line, '{') - countChar(line, '}');

            // 块闭合且达到较大尺寸时切分
            if (braceCount == 0 && estimateLength(current.toString()) >= policy.targetTokens) {
                blocks.add(current.toString().trim());
                current.setLength(0);
            }
        }
        if (current.length() > 0) {
            blocks.add(current.toString().trim());
        }

        // 对超长代码块做兜底切分（按 token）
        List<String> normalized = new ArrayList<>();
        for (String b : blocks) {
            if (estimateLength(b) <= policy.maxTokens) {
                normalized.add(b);
            } else {
                normalized.addAll(hardSplitByTokens(b, policy.maxTokens, policy.targetTokens));
            }
        }

        return mergeIntoChunks(normalized, policy, overlap, "code", "code");
    }

    /**
     * Markdown文档分割策略（保护 fenced code blocks）
     */
    private List<TextSegment> splitMarkdownDocument(Document document) {
        log.info("使用Markdown文档分割策略");

        String content = document.text();
        boolean isChinese = detectChineseContent(content);
        ChunkPolicy policy = getPolicy(FileType.MARKDOWN, isChinese);
        int overlap = 50;

        // 将 fenced code blocks 作为原子块
        List<String> blocks = new ArrayList<>();
        String[] lines = content.split("\\r?\\n", -1);
        boolean inFence = false;
        String fence = null;
        StringBuilder buf = new StringBuilder();
        for (String line : lines) {
            if (!inFence) {
                if (line.matches("^\\s*(```+|~~~+).*")) {
                    if (buf.length() > 0) {
                        blocks.add(buf.toString());
                        buf.setLength(0);
                    }
                    inFence = true;
                    fence = line.trim().startsWith("```") ? "```" : "~~~";
                    buf.append(line).append('\n');
                } else {
                    buf.append(line).append('\n');
                }
            } else {
                buf.append(line).append('\n');
                if (line.trim().startsWith(fence)) {
                    blocks.add(buf.toString());
                    buf.setLength(0);
                    inFence = false;
                    fence = null;
                }
            }
        }
        if (buf.length() > 0) blocks.add(buf.toString());

        // 非 code block 的内容，按标题与段落再切
        List<String> paragraphs = new ArrayList<>();
        for (String block : blocks) {
            String b = block.trim();
            if (b.startsWith("```") || b.startsWith("~~~")) {
                paragraphs.add(b); // 原子块
            } else {
                String[] sections = b.split("(?m)(?=^#{1,6}\\s+)");
                for (String sec : sections) {
                    String s = sec.trim();
                    if (s.isEmpty()) continue;
                    String[] ps = s.split("\\n\\n+");
                    for (String p : ps) {
                        String t = p.trim();
                        if (!t.isEmpty()) paragraphs.add(t);
                    }
                }
            }
        }

        // 对普通段落做二次切分；code block 保持原子
        List<String> normalized = new ArrayList<>();
        for (String p : paragraphs) {
            if (p.startsWith("```") || p.startsWith("~~~")) {
                normalized.add(p);
            } else {
                normalized.addAll(normalizeParagraphs(Collections.singletonList(p), isChinese, policy));
            }
        }

        return mergeIntoChunks(normalized, policy, overlap, "markdown", "markdown");
    }

    /**
     * 表格文档分割策略（Excel/CSV）——行数切片并可保留表头
     */
    private List<TextSegment> splitTableDocument(Document document) {
        log.info("使用表格文档分割策略");

        String content = document.text();
        int rowsPerChunk = 50;
        String[] lines = content.split("\\r?\\n");

        List<TextSegment> segments = new ArrayList<>();
        if (lines.length == 0) return segments;

        String header = lines[0];
        boolean hasHeader = header.contains(",") || header.contains("\t") || header.contains("|");

        int startRow = hasHeader ? 1 : 0;
        int segmentIndex = 0;

        for (int i = startRow; i < lines.length; i += rowsPerChunk) {
            int endExclusive = Math.min(i + rowsPerChunk, lines.length);
            StringBuilder sb = new StringBuilder();
            if (hasHeader) {
                sb.append(header).append("\n");
            }
            for (int r = i; r < endExclusive; r++) {
                sb.append(lines[r]).append("\n");
            }
            String chunk = sb.toString().trim();
            if (chunk.isEmpty()) continue;

            Metadata md = new Metadata();
            md.put("segment_index", String.valueOf(segmentIndex++));
            md.put("file_type", "table");
            md.put("split_method", "rows_chunk");
            md.put("row_start", String.valueOf(i));
            md.put("row_end", String.valueOf(endExclusive - 1));
            segments.add(TextSegment.from(chunk, md));
        }

        return segments;
    }

    /**
     * 结构化文档分割策略（JSON/XML）
     */
    private List<TextSegment> splitStructuredDocument(Document document) {
        log.info("使用结构化文档分割策略");

        String content = document.text();
        int chunkSize = 600;

        if ((content.contains("{") && content.contains("}")) || (content.contains("[") && content.contains("]"))) {
            return splitByBracesRobust(content, chunkSize);
        } else if (content.contains("<") && content.contains(">")) {
            return splitByTags(content, chunkSize);
        } else {
            return splitGenericDocument(document);
        }
    }

    /**
     * PPT演示文档分割策略
     */
    private List<TextSegment> splitPresentationDocument(Document document) {
        log.info("使用PPT文档分割策略");

        String content = document.text();
        int chunkSize = 400;  // PPT通常每页内容较少

        String[] slides = content.split("(?i)(slide\\s*\\d+|第\\s*\\d+\\s*页|page\\s*\\d+)");
        if (slides.length > 1) {
            List<TextSegment> segments = new ArrayList<>();
            for (String slide : slides) {
                String s = slide.trim();
                if (s.length() > 20) {
                    Metadata metadata = new Metadata();
                    metadata.put("file_type", "presentation");
                    metadata.put("split_method", "slide_hint");
                    segments.add(TextSegment.from(s, metadata));
                }
            }
            return segments;
        } else {
            return splitGenericDocument(document);
        }
    }

    /**
     * 通用文档分割策略
     */
    private List<TextSegment> splitGenericDocument(Document document) {
        log.info("使用通用文档分割策略");

        String content = document.text();
        boolean isChinese = detectChineseContent(content);
        ChunkPolicy policy = getPolicy(FileType.UNKNOWN, isChinese);
        int overlap = 80;

        String[] paras = content.split("\\n\\n+");
        if (paras.length < 5) paras = content.split("\\n+");

        List<String> raw = new ArrayList<>();
        for (String p : paras) {
            if (p != null) {
                String t = p.trim();
                if (!t.isEmpty()) raw.add(t);
            }
        }
        List<String> normalized = normalizeParagraphs(raw, isChinese, policy);
        return mergeIntoChunks(normalized, policy, overlap, "generic", "generic");
    }

    /**
     * 固定长度分割（令牌级滑窗，作为兜底）
     */
    private List<TextSegment> performFixedLengthSplitting(String content, int chunkSizeTokens, int overlapTokens) {
        List<TextSegment> segments = new ArrayList<>();
        if (content == null) return segments;

        int n = content.length();
        int start = 0;
        int index = 0;

        while (start < n) {
            int lo = start + 1;
            int hi = Math.min(n, start + Math.max(512, chunkSizeTokens * 8)); // 字符上界估计，避免走太远
            int best = lo;

            // 二分：找最大 end，使 tokens(text[start:end]) <= chunkSizeTokens
            while (lo <= hi) {
                int mid = (lo + hi) >>> 1;
                String sub = content.substring(start, mid);
                int t = estimateLength(sub);
                if (t <= chunkSizeTokens) {
                    best = mid;
                    lo = mid + 1;
                } else {
                    hi = mid - 1;
                }
            }

            if (best <= start) {
                // 退化保护：至少前进
                best = Math.min(n, start + Math.max(1, chunkSizeTokens * 3));
            }

            String seg = content.substring(start, best).trim();
            if (!seg.isEmpty()) {
                Metadata md = new Metadata();
                md.put("segment_index", String.valueOf(index++));
                md.put("split_method", "fixed_length_tokens");
                md.put("chunk_tokens_est", String.valueOf(estimateLength(seg)));
                segments.add(TextSegment.from(seg, md));
            }

            if (best >= n) break;

            // 计算在当前段内，尾部 overlapTokens 的起点 index，并以此对齐下一段起点
            int k = tailStartIndexByTokens(seg, overlapTokens);
            start = start + k;
            if (start >= n) break;
        }

        return segments;
    }

    // ========================================
    // 辅助方法
    // ========================================

    // 统一的 chunk 策略：minTokens/targetTokens/maxTokens
    private static class ChunkPolicy {
        final int minTokens;
        final int targetTokens;
        final int maxTokens;

        ChunkPolicy(int minTokens, int targetTokens, int maxTokens) {
            this.minTokens = minTokens;
            this.targetTokens = targetTokens;
            this.maxTokens = maxTokens;
        }
    }

    private ChunkPolicy getPolicy(FileType fileType, boolean isChinese) {
        switch (fileType) {
            case PDF:
                return new ChunkPolicy(200, 500, 800);
            case WORD_DOCX:
            case WORD_DOC:
                return new ChunkPolicy(200, 600, 900);
            case CODE:
                return new ChunkPolicy(150, 700, 1000);
            case MARKDOWN:
                return new ChunkPolicy(180, 500, 800);
            case EXCEL:
            case CSV:
                return new ChunkPolicy(120, 300, 600);
            case JSON:
            case XML:
                return new ChunkPolicy(180, 600, 900);
            case PPT:
                return new ChunkPolicy(120, 400, 700);
            default:
                return isChinese ? new ChunkPolicy(180, 420, 700) : new ChunkPolicy(200, 520, 800);
        }
    }

    // 从文本尾部精确取 overlapTokens 个 token 的“尾巴”作为重叠前缀文本
    private String tailByTokens(String text, int overlapTokens) {
        if (overlapTokens <= 0 || text == null || text.isEmpty()) return "";
        int totalTokens = estimateLength(text);
        if (totalTokens <= overlapTokens) return text;

        int right = text.length();
        int low = Math.max(0, right - overlapTokens * 8);
        int high = right;
        int res = low;

        while (low <= high) {
            int mid = (low + high) >>> 1;
            String sub = text.substring(mid);
            int t = estimateLength(sub);
            if (t >= overlapTokens) {
                res = mid;
                low = mid + 1;
            } else {
                high = mid - 1;
            }
        }
        return text.substring(res);
    }

    // 返回段内“尾部 overlapTokens 的起始字符下标”
    private int tailStartIndexByTokens(String text, int overlapTokens) {
        if (text == null) return 0;
        if (overlapTokens <= 0) return text.length();
        int total = estimateLength(text);
        if (total <= overlapTokens) return text.length(); // 无法实现足够重叠，退化为零重叠（从段末开始）

        int right = text.length();
        int low = 0, high = right, res = right;
        while (low <= high) {
            int mid = (low + high) >>> 1;
            String sub = text.substring(mid);
            int t = estimateLength(sub);
            if (t >= overlapTokens) {
                res = mid;
                low = mid + 1;
            } else {
                high = mid - 1;
            }
        }
        return res;
    }

    // 简单的中英文句子分割
    private List<String> splitIntoSentences(String text, boolean chinese) {
        List<String> list = new ArrayList<>();
        if (text == null || text.trim().isEmpty()) return list;
        String[] arr = chinese ? text.split("(?<=[。！？；…])") : text.split("(?<=[.!?])\\s+");
        for (String s : arr) {
            String t = s.trim();
            if (!t.isEmpty()) list.add(t);
        }
        return list;
    }

    // 兜底：把一个超长文本按 token 上限切成若干不超过 maxTokens 的块（尽量靠近 targetTokens）
    private List<String> hardSplitByTokens(String text, int maxTokens, int targetTokens) {
        List<String> out = new ArrayList<>();
        if (text == null || text.isEmpty()) return out;
        int n = text.length();
        int i = 0;
        while (i < n) {
            int upperChar = Math.min(n, i + Math.max(256, targetTokens * 6));
            int lo = i + 1, hi = upperChar, ans = i + Math.min(upperChar - i, 512);
            // 二分找最大 end，使 tokens(text[i:end]) <= maxTokens
            while (lo <= hi) {
                int mid = (lo + hi) >>> 1;
                String sub = text.substring(i, mid);
                int t = estimateLength(sub);
                if (t <= maxTokens) {
                    ans = mid;
                    lo = mid + 1;
                } else {
                    hi = mid - 1;
                }
            }
            if (ans <= i) {
                ans = Math.min(n, i + Math.max(1, maxTokens * 3));
            }
            String piece = text.substring(i, ans).trim();
            if (!piece.isEmpty()) out.add(piece);
            i = ans;
        }
        return out;
    }

    // 对“长段”进行二次切分：先句子级，再兜底 token 切
    private List<String> normalizeLongParagraph(String para, boolean chinese, int maxTokens, int targetTokens) {
        List<String> result = new ArrayList<>();
        if (para == null || para.trim().isEmpty()) return result;
        para = para.trim();

        if (estimateLength(para) <= maxTokens) {
            result.add(para);
            return result;
        }

        List<String> sentences = splitIntoSentences(para, chinese);
        if (sentences.isEmpty()) {
            result.addAll(hardSplitByTokens(para, maxTokens, targetTokens));
            return result;
        }

        StringBuilder buf = new StringBuilder();
        int tokens = 0;
        for (String s : sentences) {
            int t = estimateLength(s);
            if (t > maxTokens) {
                if (buf.length() > 0) {
                    result.add(buf.toString().trim());
                    buf.setLength(0);
                    tokens = 0;
                }
                result.addAll(hardSplitByTokens(s, maxTokens, targetTokens));
                continue;
            }
            if (tokens > 0 && tokens + t > maxTokens) {
                result.add(buf.toString().trim());
                buf.setLength(0);
                tokens = 0;
            }
            if (buf.length() > 0) buf.append(' ');
            buf.append(s);
            tokens += t;
        }
        if (buf.length() > 0) {
            result.add(buf.toString().trim());
        }
        return result;
    }

    // 批量对段落做“超长二次切分”
    private List<String> normalizeParagraphs(List<String> paragraphs, boolean chinese, ChunkPolicy policy) {
        List<String> out = new ArrayList<>();
        for (String p : paragraphs) {
            String para = p == null ? "" : p.trim();
            if (para.isEmpty()) continue;
            if (estimateLength(para) <= policy.maxTokens) {
                out.add(para);
            } else {
                out.addAll(normalizeLongParagraph(para, chinese, policy.maxTokens, policy.targetTokens));
            }
        }
        return out;
    }

    /**
     * 将段落合并为 chunk（输入段落需已 normalize）
     * - 目标大小：policy.targetTokens，最大不超过 policy.maxTokens
     * - 重叠：按 token，从上一个 chunk 尾部取 overlapTokens 的文本尾
     */
    private List<TextSegment> mergeIntoChunks(List<String> paragraphs, ChunkPolicy policy, int overlapTokens, String splitMethod, String fileTypeForMeta) {
        List<TextSegment> segments = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int currentTokens = 0;
        int idx = 0;

        for (String para : paragraphs) {
            int t = estimateLength(para);

            if ((currentTokens >= policy.minTokens && currentTokens + t > policy.targetTokens) || (currentTokens > 0 && currentTokens + t > policy.maxTokens)) {

                String segmentText = current.toString().trim();
                if (!segmentText.isEmpty()) {
                    Metadata md = new Metadata();
                    md.put("segment_index", String.valueOf(idx++));
                    md.put("split_method", splitMethod);
                    if (fileTypeForMeta != null) md.put("file_type", fileTypeForMeta);
                    md.put("chunk_tokens", String.valueOf(currentTokens));
                    segments.add(TextSegment.from(segmentText, md));
                }

                String tail = tailByTokens(segmentText, overlapTokens);
                current = new StringBuilder(tail);
                currentTokens = estimateLength(tail);
            }

            if (current.length() > 0) current.append('\n');
            current.append(para);
            currentTokens += t;
        }

        String last = current.toString().trim();
        if (!last.isEmpty()) {
            Metadata md = new Metadata();
            md.put("segment_index", String.valueOf(idx));
            md.put("split_method", splitMethod);
            if (fileTypeForMeta != null) md.put("file_type", fileTypeForMeta);
            md.put("chunk_tokens", String.valueOf(currentTokens));
            segments.add(TextSegment.from(last, md));
        }
        return segments;
    }

    /**
     * 更稳的 JSON 括号分割（简化版，支持字符串与 []）
     */
    private List<TextSegment> splitByBracesRobust(String content, int chunkSizeTokens) {
        List<TextSegment> segments = new ArrayList<>();
        if (content == null || content.trim().isEmpty()) return segments;

        StringBuilder buf = new StringBuilder();
        boolean inString = false;
        char quote = 0;
        boolean escape = false;
        int braces = 0;   // {}
        int brackets = 0; // []
        int idx = 0;

        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);
            buf.append(c);

            if (inString) {
                if (escape) {
                    escape = false;
                } else if (c == '\\') {
                    escape = true;
                } else if (c == quote) {
                    inString = false;
                }
            } else {
                if (c == '"' || c == '\'') {
                    inString = true;
                    quote = c;
                } else if (c == '{') {
                    braces++;
                } else if (c == '}') {
                    braces--;
                } else if (c == '[') {
                    brackets++;
                } else if (c == ']') {
                    brackets--;
                }
            }

            int depth = Math.max(0, braces + brackets);
            boolean candidateBoundary = !inString && (depth <= 1) && (c == '}' || c == ']' || c == ',' || c == '\n');

            if (candidateBoundary) {
                String part = buf.toString().trim();
                if (!part.isEmpty() && estimateLength(part) >= chunkSizeTokens) {
                    Metadata md = new Metadata();
                    md.put("file_type", "json");
                    md.put("split_method", "json_braces_robust");
                    md.put("segment_index", String.valueOf(idx++));
                    segments.add(TextSegment.from(part, md));
                    buf.setLength(0);
                }
            }
        }
        String remain = buf.toString().trim();
        if (!remain.isEmpty()) {
            Metadata md = new Metadata();
            md.put("file_type", "json");
            md.put("split_method", "json_braces_robust");
            md.put("segment_index", String.valueOf(idx));
            segments.add(TextSegment.from(remain, md));
        }
        return segments;
    }

    /**
     * 按XML标签分割（简化实现：回退到通用分割）
     */
    private List<TextSegment> splitByTags(String content, int chunkSize) {
        return splitGenericDocument(Document.from(content));
    }

    /**
     * 处理分割后的段落：摘要、增强、向量化、缓存
     */
    private void processSegments(List<TextSegment> segments, Document document, FileType fileType, String fileKey, File file) {
        try {
            // 生成文档摘要（可选：调用LLM简述）
            String summary = generateDocumentSummary(segments);

            // 增强段落
            List<TextSegment> enhancedSegments = enhanceSegments(segments, summary, fileType, document.metadata());

            // 生成和存储嵌入
            generateAndStoreEmbeddings(enhancedSegments);

            // 缓存结果（使用 file.path 作为 key，并记录 TTL 和 lastModified）
            documentCache.put(fileKey, enhancedSegments);
            cacheTimestamps.put(fileKey, System.currentTimeMillis());
            cacheLastModified.put(fileKey, file.lastModified());

        } catch (Exception e) {
            log.error("处理段落失败", e);
        }
    }

    /**
     * 增强段落元数据（复制元数据，避免共享引用）
     */
    private List<TextSegment> enhanceSegments(List<TextSegment> segments, String summary, FileType fileType, Metadata docMetadata) {
        List<TextSegment> enhanced = new ArrayList<>();

        for (int i = 0; i < segments.size(); i++) {
            TextSegment segment = segments.get(i);

            Metadata metadata = cloneMetadata(segment.metadata());
            if (metadata == null) metadata = new Metadata();

            // 文档级元数据（只读信息复制一份）
            if (docMetadata != null) {
                Metadata finalMetadata = metadata;
                docMetadata.toMap().forEach((k, v) -> finalMetadata.put(k, String.valueOf(v)));
            }

            // 添加文档级信息
            metadata.put("document_summary", summary);
            metadata.put("file_type", fileType.toString());
            metadata.put("position", String.valueOf(i));
            metadata.put("total_segments", String.valueOf(segments.size()));
            metadata.put("position_ratio", String.format(Locale.ROOT, "%.4f", (segments.size() == 0 ? 0.0 : (double) i / segments.size())));

            // 添加上下文
            if (i > 0) {
                String prevText = segments.get(i - 1).text();
                metadata.put("prev_context", prevText.substring(0, Math.min(100, prevText.length())));
            }
            if (i < segments.size() - 1) {
                String nextText = segments.get(i + 1).text();
                metadata.put("next_context", nextText.substring(0, Math.min(100, nextText.length())));
            }

            enhanced.add(TextSegment.from(segment.text(), metadata));
        }

        return enhanced;
    }

    /**
     * 生成并存储嵌入
     */
    private void generateAndStoreEmbeddings(List<TextSegment> segments) {
        int batchSize = 10;

        for (int i = 0; i < segments.size(); i += batchSize) {
            int end = Math.min(i + batchSize, segments.size());
            List<TextSegment> batch = segments.subList(i, end);

            try {
                List<Embedding> embeddings = embeddingModel.embedAll(batch).content();

                for (int j = 0; j < batch.size(); j++) {
                    embeddingStore.add(embeddings.get(j), batch.get(j));
                }

                log.debug("成功存储 {} 个嵌入", batch.size());
            } catch (Exception e) {
                log.error("生成嵌入失败", e);
            }
        }
    }

    /**
     * 生成文档摘要（若注入了ChatLanguageModel，则简要概述，否则给出统计信息）
     */
    private String generateDocumentSummary(List<TextSegment> segments) {
        if (segments == null || segments.isEmpty()) {
            return "无内容";
        }

        // 如果存在LLM，则用前若干片段做简要总结（避免超长提示）
        if (chatLanguageModel != null) {
            try {
                StringBuilder sb = new StringBuilder();
                int limitChars = 3000;
                for (TextSegment s : segments) {
                    if (sb.length() > limitChars) break;
                    sb.append(s.text()).append("\n");
                }
                String prompt = "请用不超过100字总结以下文档的主要内容：\n" + sb;
                String llmSummary = chatLanguageModel.generate(prompt);
                if (llmSummary != null && !llmSummary.trim().isEmpty()) {
                    return llmSummary.trim();
                }
            } catch (Exception e) {
                log.warn("LLM 摘要失败，使用统计信息作为摘要: {}", e.getMessage());
            }
        }

        // 回退：统计信息
        return String.format(Locale.ROOT, "文档包含%d个段落，总长度约%d字符", segments.size(), segments.stream().mapToInt(s -> s.text().length()).sum());
    }

    /**
     * 记录文档信息（打印预览，避免泄露全文）
     */
    private void logDocumentInfo(Document document, FileType fileType) {
        String content = document.text();
        int previewLen = Math.min(800, content.length());
        String preview = content.substring(0, previewLen).replaceAll("\\s+", " ");
        log.info("=== 文档分析 ===");
        log.info("文件类型: {}", fileType);
        log.info("内容长度: {} 字符", content.length());
        log.info("内容预览(前{}字): {}", previewLen, preview);
        log.info("行数: {}", content.split("\\r?\\n").length);
        log.info("段落数(\\n\\n): {}", content.split("\\n\\n").length);
    }

    // ========================================
    // 工具方法
    // ========================================

    private boolean detectChineseContent(String text) {
        if (text == null || text.isEmpty()) return false;
        String sample = text.length() > 1000 ? text.substring(0, 1000) : text;
        long chineseChars = sample.chars().filter(ch -> ch >= 0x4e00 && ch <= 0x9fa5).count();
        return chineseChars > sample.length() * 0.1;
    }

    private int countChar(String str, char c) {
        int count = 0;
        for (char ch : str.toCharArray()) {
            if (ch == c) count++;
        }
        return count;
    }

    private int estimateLength(String text) {
        if (text == null || text.isEmpty()) return 0;
        try {
            return tokenizer.estimateTokenCountInText(text);
        } catch (Exception e) {
            // 退化估算：1 token ≈ 3字符
            return Math.max(1, text.length() / 3);
        }
    }

    private String generateFileKey(File file) {
        // 避免 key 膨胀：使用 path 作为 key，通过 isCacheValid 校验 lastModified
        return file.getAbsolutePath();
    }

    private boolean isCacheValid(String fileKey, File file) {
        if (!documentCache.containsKey(fileKey)) {
            return false;
        }

        Long createdTime = cacheTimestamps.get(fileKey);
        Long cachedLastModified = cacheLastModified.get(fileKey);
        if (createdTime == null || cachedLastModified == null) {
            return false;
        }

        // TTL 过期
        if (System.currentTimeMillis() - createdTime > CACHE_EXPIRY_MS) {
            documentCache.remove(fileKey);
            cacheTimestamps.remove(fileKey);
            cacheLastModified.remove(fileKey);
            return false;
        }

        // 文件有更新
        if (file.lastModified() > cachedLastModified) {
            documentCache.remove(fileKey);
            cacheTimestamps.remove(fileKey);
            cacheLastModified.remove(fileKey);
            return false;
        }

        return true;
    }

    private Metadata cloneMetadata(Metadata source) {
        Metadata target = new Metadata();
        if (source != null) {
            Map<String, Object> map = source.toMap();
            for (Map.Entry<String, Object> e : map.entrySet()) {
                target.put(e.getKey(), String.valueOf(e.getValue()));
            }
        }
        return target;
    }

    // PDF 文本基础清洗：合并软换行、去常见页码/页眉页脚
    private String preprocessPdfText(String raw) {
        if (raw == null) return "";
        String text = raw.replace("\r\n", "\n").replace('\r', '\n');

        // 断词连字：英文字母"-\n"拼接
        text = text.replaceAll("([A-Za-z]{2,})-\\n([A-Za-z]{2,})", "$1$2");

        // 暂存段落分隔，再合并软换行
        String marker = "\u000B";
        text = text.replaceAll("\\n\\s*\\n", marker);
        // 合并单个换行（段内软换行）为空格
        text = text.replaceAll("(?<=\\S)\\n(?=\\S)", " ");
        text = text.replace(marker, "\n\n");

        // 去常见页码行（Page x / y、纯数字行、中文“第 x 页”）
        text = java.util.regex.Pattern.compile("(?m)^\\s*(Page\\s*)?\\d+(\\s*/\\s*\\d+)?\\s*$").matcher(text).replaceAll("");
        text = java.util.regex.Pattern.compile("(?m)^\\s*第\\s*\\d+\\s*页\\s*$").matcher(text).replaceAll("");

        return text.trim();
    }

    /**
     * 批量处理目录下的所有文件（非递归）
     */
    public void processDirectory(String directoryPath) {
        File directory = new File(directoryPath);
        if (!directory.exists() || !directory.isDirectory()) {
            log.error("目录不存在: {}", directoryPath);
            return;
        }

        File[] files = directory.listFiles();
        if (files == null || files.length == 0) {
            log.warn("目录为空: {}", directoryPath);
            return;
        }

        log.info("开始处理目录: {}, 包含 {} 个文件", directoryPath, files.length);

        for (File file : files) {
            if (file.isFile()) {
                processDocumentDynamically(file);
            }
        }

        log.info("目录处理完成: {}", directoryPath);
    }

    /**
     * 清理缓存
     */
    public void clearCache() {
        documentCache.clear();
        cacheTimestamps.clear();
        cacheLastModified.clear();
        log.info("文档缓存已清理");
    }

    /**
     * 获取文档统计信息
     */
    public Map<String, Object> getDocumentStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("cached_documents", documentCache.size());
        stats.put("total_segments", documentCache.values().stream().mapToInt(List::size).sum());
        stats.put("supported_formats", Arrays.toString(FileType.values()));
        return stats;
    }
}