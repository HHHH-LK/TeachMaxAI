package com.aiproject.smartcampus.model.handler.impl;

import com.aiproject.smartcampus.commons.client.ResultCilent;
import com.aiproject.smartcampus.commons.delayedtask.IntentBatchTask;
import com.aiproject.smartcampus.commons.delayedtask.IntentDelayedQueueClien;
import com.aiproject.smartcampus.commons.utils.CreateDiagram;
import com.aiproject.smartcampus.model.handler.BaseEnhancedHandler;
import com.aiproject.smartcampus.model.prompts.UserPrompts;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.query.Query;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.PreDestroy;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class SeptIntentRagHandler extends BaseEnhancedHandler {

    private final CreateDiagram createDiagram;
    private final ChatLanguageModel chatLanguageModel;
    private final IntentDelayedQueueClien intentDelayedQueueClient;
    private final ResultCilent resultClient;
    private final ContentRetriever contentRetriever;
    private final EmbeddingModel embeddingModel;

    private final ExecutorService executorService = Executors.newFixedThreadPool(
            Math.max(4, Runtime.getRuntime().availableProcessors() / 2),
            r -> {
                Thread t = new Thread(r);
                t.setName("rag-retrieval-" + t.getId());
                t.setDaemon(true);
                return t;
            });

    // 基本参数（建议外部化）
    private static final int MAX_RETRY_TIMES = 30;
    private static final int SLEEP_TIME_MS = 200;
    private static final int TASK_TIMEOUT_SECONDS = 5;

    // 检索与融合参数
    private static final int TOP_K_CANDIDATES = 50;     // 每个 query 的候选池
    private static final int TOP_K_PER_STRATEGY = 10;   // 每个策略取前k
    private static final int TOP_K_RESULTS = 10;        // 融合后保留前k
    private static final int TOP_N_FINAL = 5;           // 最终对用户展示的数量
    private static final double MIN_RELEVANCE_SCORE = 0.35;
    private static final double HYBRID_SEM_WEIGHT = 0.65;
    private static final double HYBRID_KEY_WEIGHT = 0.35;
    private static final int RETRIEVAL_TIMEOUT_SECONDS = 10;
    private static final int RRF_K = 60;                // RRF 稳定常数

    // Embedding 缓存（简单 LRU）
    private final Map<String, double[]> embeddingCache = Collections.synchronizedMap(
            new LinkedHashMap<String, double[]>(1024, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, double[]> eldest) {
                    return size() > 2048;
                }
            }
    );

    @Getter
    public enum RetrievalStrategy {
        SEMANTIC("语义检索", 0.4),
        KEYWORD("关键词检索", 0.3),
        HYBRID("混合检索", 0.3);

        private final String name;
        private final double weight;

        RetrievalStrategy(String name, double weight) {
            this.name = name;
            this.weight = weight;
        }
    }

    @Data
    public static class ScoredContent {
        private Content content;
        private double score;
        private RetrievalStrategy strategy;
        private Map<String, Double> dimensionScores = new HashMap<>();

        public ScoredContent(Content content, double score, RetrievalStrategy strategy) {
            this.content = content;
            this.score = score;
            this.strategy = strategy;
        }
    }

    @Override
    public String executeBusinessLogic(String intent, List<CompletableFuture<String>> result) {
        if (!StringUtils.hasText(intent)) {
            throw new IllegalArgumentException("意图参数不能为空");
        }

        log.info("增强型RAG处理器开始执行业务逻辑，意图: {}", intent);
        int inDegree = createDiagram.getInDegree(intent);
        if (inDegree == 0) {
            return executeDirectTask(intent);
        } else {
            return executeWithDependencies(intent, result);
        }
    }

    private String executeDirectTask(String intent) {
        try {
            List<ScoredContent> scoredContents = performMultiDimensionalRetrieval(intent);
            if (scoredContents.isEmpty()) {
                log.warn("任务[{}]未找到相关内容", intent);
                return "未找到相关内容";
            }
            List<ScoredContent> fused = fuseAndRerankRRF(scoredContents);
            String out = buildFinalResult(fused, intent);
            log.info("执行[{}]成功，结果长度: {}", intent, out.length());
            return out;
        } catch (Exception e) {
            log.error("执行直接任务[{}]失败", intent, e);
            throw new RuntimeException("直接任务执行失败: " + intent, e);
        }
    }

    private String executeWithDependencies(String intent, List<CompletableFuture<String>> dependencies) {
        List<String> parentTasks = createDiagram.getParetents(intent);
        int expectedDependencyCount = parentTasks.size();

        if (dependencies != null && dependencies.size() == expectedDependencyCount &&
                areAllTasksCompleted(dependencies)) {
            return executeTaskWithCompletedDependencies(intent, dependencies);
        } else {
            return handlePendingDependencies(intent, dependencies, expectedDependencyCount);
        }
    }

    private String executeTaskWithCompletedDependencies(String intent,
                                                        List<CompletableFuture<String>> dependencies) {
        try {
            List<String> dependencyResults = collectDependencyResults(intent, dependencies);
            String combinedResults = buildDependencyResultString(dependencyResults);
            String enhancedQuery = generateEnhancedQuery(intent, combinedResults);

            log.info("生成增强查询成功，将执行: {}", enhancedQuery);

            List<ScoredContent> scoredContents = performMultiDimensionalRetrieval(enhancedQuery);
            if (scoredContents.isEmpty()) {
                log.warn("任务[{}]未找到相关内容", intent);
                return "未找到相关内容";
            }
            List<ScoredContent> fused = fuseAndRerankRRF(scoredContents);
            String finalResult = buildFinalResult(fused, intent);
            log.info("执行[{}]成功，结果长度: {}", intent, finalResult.length());
            return finalResult;

        } catch (Exception e) {
            log.error("执行依赖任务[{}]失败", intent, e);
            throw new RuntimeException("依赖任务执行失败: " + intent, e);
        }
    }

    private List<String> collectDependencyResults(String intent,
                                                  List<CompletableFuture<String>> dependencies)
            throws InterruptedException, ExecutionException, TimeoutException {
        List<String> dependencyResults = new ArrayList<>();
        for (int i = 0; i < dependencies.size(); i++) {
            try {
                String result = dependencies.get(i).get(TASK_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                dependencyResults.add(result != null ? result : "");
            } catch (TimeoutException te) {
                log.warn("前置任务{}等待超时，任务[{}]将重试", i, intent);
                throw te;
            } catch (ExecutionException | InterruptedException e) {
                log.error("获取前置任务{}结果异常，任务[{}]", i, intent, e);
                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
                throw e;
            }
        }
        return dependencyResults;
    }

    private String buildDependencyResultString(List<String> dependencyResults) {
        StringBuilder resultBuilder = new StringBuilder();
        resultBuilder.append("以下是前置任务的执行结果（系统内部处理结果）：\n\n");
        for (int i = 0; i < dependencyResults.size(); i++) {
            resultBuilder.append("步骤").append(i + 1).append("执行结果：\n")
                    .append(dependencyResults.get(i)).append("\n\n");
        }
        resultBuilder.append("请基于以上前置步骤的执行结果完成当前任务。");
        return resultBuilder.toString();
    }

    private String generateEnhancedQuery(String intent, String dependencyResults) {
        ChatResponse chatResponse = chatLanguageModel.chat(
                UserMessage.from(UserPrompts.chainUserPrompts(intent, dependencyResults))
        );
        return chatResponse.aiMessage().text();
    }

    private String handlePendingDependencies(String intent, List<CompletableFuture<String>> dependencies,
                                             int expectedCount) {
        log.info("任务[{}]缺失前置依赖，需要延迟执行", intent);

        List<CompletableFuture<String>> completedDependencies =
                waitForDependenciesToComplete(intent, expectedCount);

        if (completedDependencies.size() == expectedCount && areAllTasksCompleted(completedDependencies)) {
            addToDelayedQueue(intent, completedDependencies);
            return null;
        } else {
            log.warn("任务[{}]的依赖结果未准备完成，标记为依赖失败", intent);
            throw new RuntimeException("任务依赖缺失: " + intent);
        }
    }

    private List<CompletableFuture<String>> waitForDependenciesToComplete(String intent, int expectedCount) {
        int retryCount = 0;
        List<CompletableFuture<String>> results = new ArrayList<>();

        while (retryCount < MAX_RETRY_TIMES) {
            try {
                results.clear();
                List<String> parentTasks = createDiagram.getParetents(intent);
                for (String parentTask : parentTasks) {
                    List<CompletableFuture<String>> parentResults = resultClient.getResult(parentTask);
                    if (parentResults != null) {
                        results.addAll(parentResults);
                    }
                }

                if (results.size() == expectedCount && areAllTasksCompleted(results)) {
                    log.info("任务[{}]的依赖结果已准备完成", intent);
                    break;
                }

                Thread.sleep(SLEEP_TIME_MS);
                retryCount++;
                if (retryCount % 2 == 0) {
                    log.info("任务[{}]等待依赖结果中，重试次数: {}/{}", intent, retryCount, MAX_RETRY_TIMES);
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("等待依赖结果被中断: {}", intent, e);
                break;
            } catch (Exception e) {
                log.error("获取依赖结果时发生异常: {}", intent, e);
                retryCount++;
            }
        }
        return results;
    }

    private void addToDelayedQueue(String intent, List<CompletableFuture<String>> dependencies) {
        if (!intentDelayedQueueClient.containsTask(intent)) {
            log.info("任务[{}]的依赖结果已准备完成，加入延时队列", intent);
            intentDelayedQueueClient.addTask(new IntentBatchTask(intent, dependencies, 500));
        } else {
            log.info("任务[{}]已存在延时队列中", intent);
        }
    }

    private boolean areAllTasksCompleted(List<CompletableFuture<String>> futures) {
        if (futures == null || futures.isEmpty()) return false;
        for (CompletableFuture<String> f : futures) {
            if (f == null || !f.isDone() || f.isCancelled() || f.isCompletedExceptionally()) {
                return false;
            }
        }
        return true;
    }

    /**
     * 多维度检索：每个扩展 query 只进行一次召回，在内存中做多策略打分
     * 修复类型问题
     */
    private List<ScoredContent> performMultiDimensionalRetrieval(String intent) {
        List<String> expandedQueries = expandQuery(intent);

        // 修复：明确指定泛型类型为 List<ScoredContent>
        List<CompletableFuture<List<ScoredContent>>> futures = expandedQueries.stream()
                .map(q -> CompletableFuture.supplyAsync(() -> {
                            List<Content> candidates = safeRetrieve(q, TOP_K_CANDIDATES);
                            if (candidates.isEmpty()) return Collections.<ScoredContent>emptyList();

                            // 关键词集（对中文更友好）
                            Set<String> keywords = extractKeywords(q);

                            // 语义打分
                            List<ScoredContent> semantic = candidates.stream()
                                    .map(c -> {
                                        double s = calculateSemanticScore(q, c);
                                        ScoredContent sc = new ScoredContent(c, s, RetrievalStrategy.SEMANTIC);
                                        sc.getDimensionScores().put("semantic", s);
                                        return sc;
                                    })
                                    .sorted(Comparator.comparingDouble(ScoredContent::getScore).reversed())
                                    .limit(TOP_K_PER_STRATEGY)
                                    .collect(Collectors.toList());

                            // 关键词打分
                            List<ScoredContent> keywordList = candidates.stream()
                                    .map(c -> {
                                        double s = calculateKeywordScore(keywords, c);
                                        ScoredContent sc = new ScoredContent(c, s, RetrievalStrategy.KEYWORD);
                                        sc.getDimensionScores().put("keyword", s);
                                        return sc;
                                    })
                                    .sorted(Comparator.comparingDouble(ScoredContent::getScore).reversed())
                                    .limit(TOP_K_PER_STRATEGY)
                                    .collect(Collectors.toList());

                            // 混合打分
                            List<ScoredContent> hybrid = candidates.stream()
                                    .map(c -> {
                                        double ss = calculateSemanticScore(q, c);
                                        double ks = calculateKeywordScore(keywords, c);
                                        double hs = HYBRID_SEM_WEIGHT * ss + HYBRID_KEY_WEIGHT * ks;
                                        ScoredContent sc = new ScoredContent(c, hs, RetrievalStrategy.HYBRID);
                                        sc.getDimensionScores().put("semantic", ss);
                                        sc.getDimensionScores().put("keyword", ks);
                                        sc.getDimensionScores().put("hybrid", hs);
                                        return sc;
                                    })
                                    .sorted(Comparator.comparingDouble(ScoredContent::getScore).reversed())
                                    .limit(TOP_K_PER_STRATEGY)
                                    .collect(Collectors.toList());

                            List<ScoredContent> ret = new ArrayList<>(semantic.size() + keywordList.size() + hybrid.size());
                            ret.addAll(semantic);
                            ret.addAll(keywordList);
                            ret.addAll(hybrid);

                            return ret.stream()
                                    .filter(sc -> sc.getScore() >= MIN_RELEVANCE_SCORE)
                                    .collect(Collectors.toList());
                        }, executorService)
                        .<List<ScoredContent>>orTimeout(RETRIEVAL_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                        .exceptionally(ex -> {
                            log.warn("检索/打分失败，query='{}'", q, ex);
                            return Collections.emptyList();
                        }))
                .toList();

        return futures.stream()
                .map(CompletableFuture::join)
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }

    private List<Content> safeRetrieve(String query, int limit) {
        try {
            List<Content> list = contentRetriever.retrieve(Query.from(query));
            if (list == null) return Collections.emptyList();
            return list.stream().limit(limit).collect(Collectors.toList());
        } catch (Exception e) {
            log.error("ContentRetriever.retrieve 异常, query={}", query, e);
            return Collections.emptyList();
        }
    }

    /**
     * RRF 融合与重排序，按内容唯一键聚合
     */
    private List<ScoredContent> fuseAndRerankRRF(List<ScoredContent> scoredContents) {
        if (scoredContents == null || scoredContents.isEmpty()) return Collections.emptyList();

        // 分策略排序并生成 rank
        Map<RetrievalStrategy, List<ScoredContent>> byStrategy = scoredContents.stream()
                .collect(Collectors.groupingBy(ScoredContent::getStrategy));

        // 用内容 key 做聚合
        Map<String, Double> rrfScore = new HashMap<>();
        Map<String, ScoredContent> representative = new HashMap<>();
        Map<String, Map<String, Double>> dimAgg = new HashMap<>();

        for (Map.Entry<RetrievalStrategy, List<ScoredContent>> entry : byStrategy.entrySet()) {
            RetrievalStrategy st = entry.getKey();
            // 对于同一内容，只记第一次出现的 rank（更靠前的版本）
            List<ScoredContent> sorted = entry.getValue().stream()
                    .sorted(Comparator.comparingDouble(ScoredContent::getScore).reversed())
                    .toList();

            Set<String> seen = new HashSet<>();
            int rank = 1;
            for (ScoredContent sc : sorted) {
                String key = contentKey(sc.getContent());
                if (seen.contains(key)) continue;
                seen.add(key);

                double add = 1.0 / (RRF_K + rank);
                rrfScore.merge(key, add, Double::sum);

                // 代表条目：保留最高分的作为代表
                representative.merge(key, sc, (oldV, newV) ->
                        (newV.getScore() > oldV.getScore()) ? newV : oldV);

                // 维度分数组合：取最大值
                dimAgg.computeIfAbsent(key, k -> new HashMap<>());
                for (Map.Entry<String, Double> d : sc.getDimensionScores().entrySet()) {
                    dimAgg.get(key).merge(d.getKey(), d.getValue(), Math::max);
                }
                rank++;
            }
        }

        // 组装融合结果
        List<ScoredContent> fused = new ArrayList<>(representative.size());
        for (Map.Entry<String, ScoredContent> e : representative.entrySet()) {
            ScoredContent rep = e.getValue();
            rep.setScore(rrfScore.getOrDefault(e.getKey(), 0.0));
            rep.setStrategy(RetrievalStrategy.HYBRID); // 标记为融合
            rep.setDimensionScores(dimAgg.getOrDefault(e.getKey(), Collections.emptyMap()));
            fused.add(rep);
        }

        fused.sort(Comparator.comparingDouble(ScoredContent::getScore).reversed());
        return fused.stream().limit(TOP_K_RESULTS).collect(Collectors.toList());
    }

    /**
     * 查询扩展（去重/过滤）
     */
    private List<String> expandQuery(String originalQuery) {
        LinkedHashSet<String> expanded = new LinkedHashSet<>();
        expanded.add(originalQuery);

        try {
            String prompt = "请为以下查询生成2-3个相关的变体查询，用于改进检索效果：\n" +
                    "原始查询：" + originalQuery + "\n" +
                    "要求：\n" +
                    "1) 语言与原查询一致\n" +
                    "2) 不要编号，只换行列出\n" +
                    "3) 每个变体不超过20个字/词";
            ChatResponse response = chatLanguageModel.chat(UserMessage.from(prompt));
            String[] variants = response.aiMessage().text().split("\\r?\\n");
            for (String v : variants) {
                String vv = v.trim().replaceAll("^[\\d\\-.)】\\s]+", "");
                if (StringUtils.hasText(vv) && !vv.equalsIgnoreCase(originalQuery)) {
                    expanded.add(vv);
                    if (expanded.size() >= 4) break;
                }
            }
        } catch (Exception e) {
            log.warn("查询扩展失败，使用原始查询: {}", e.getMessage());
        }
        return new ArrayList<>(expanded);
    }

    /**
     * 计算语义分（Embedding + 余弦），回退 Jaccard
     */
    private double calculateSemanticScore(String query, Content content) {
        String text = extractTextFromContent(content);
        if (!StringUtils.hasText(text)) return 0.0;

        try {
            double[] qv = embedVector(query);
            double[] cv = embedVector(text);
            if (qv.length > 0 && cv.length > 0) {
                return cosine(qv, cv);
            }
        } catch (Exception e) {
            log.warn("Embedding 计算失败，使用回退: {}", e.getMessage());
        }
        return jaccardOverlap(query, text);
    }

    private double[] embedVector(String text) {
        return embeddingCache.computeIfAbsent(text, t -> {
            try {
                var resp = embeddingModel.embed(TextSegment.from(t));
                var emb = resp.content();
                float[] fv = emb.vector();
                double[] arr = new double[fv.length];
                for (int i = 0; i < fv.length; i++) arr[i] = fv[i];
                return arr;
            } catch (Exception e) {
                log.warn("embedVector 异常: {}", e.getMessage());
                return new double[0];
            }
        });
    }

    private double cosine(double[] a, double[] b) {
        if (a.length == 0 || b.length == 0 || a.length != b.length) return 0.0;
        double dot = 0, na = 0, nb = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            na += a[i] * a[i];
            nb += b[i] * b[i];
        }
        return (na == 0 || nb == 0) ? 0.0 : dot / (Math.sqrt(na) * Math.sqrt(nb));
    }

    private double jaccardOverlap(String q, String t) {
        Set<String> s1 = tokenizeForOverlap(q);
        Set<String> s2 = tokenizeForOverlap(t);
        if (s1.isEmpty() || s2.isEmpty()) return 0.0;
        Set<String> inter = new HashSet<>(s1);
        inter.retainAll(s2);
        Set<String> union = new HashSet<>(s1);
        union.addAll(s2);
        return inter.size() * 1.0 / union.size();
    }

    /**
     * 关键词匹配分（中文友好）
     */
    private double calculateKeywordScore(Set<String> keywords, Content content) {
        String text = extractTextFromContent(content).toLowerCase();
        if (keywords.isEmpty() || !StringUtils.hasText(text)) return 0.0;
        long match = keywords.stream().filter(k -> text.contains(k.toLowerCase())).count();
        return match * 1.0 / keywords.size();
    }

    private Set<String> extractKeywords(String text) {
        if (!StringUtils.hasText(text)) return Collections.emptySet();
        String normalized = text.replaceAll("\\s+", " ").trim();

        boolean hasCJK = normalized.codePoints().anyMatch(cp ->
                (cp >= 0x4E00 && cp <= 0x9FFF) || (cp >= 0x3400 && cp <= 0x4DBF));

        if (hasCJK) {
            List<String> grams = new ArrayList<>();
            String s = normalized.replaceAll("\\p{Punct}", "");
            for (int i = 0; i < s.length() - 1; i++) {
                grams.add(s.substring(i, i + 2));
            }
            return grams.stream()
                    .collect(Collectors.groupingBy(g -> g, Collectors.counting()))
                    .entrySet().stream()
                    .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                    .limit(20)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
        } else {
            String[] tokens = normalized.toLowerCase().split("[^a-z0-9_]+");
            Set<String> stop = defaultStopWords();
            return Arrays.stream(tokens)
                    .filter(t -> t.length() >= 3 && !stop.contains(t))
                    .limit(30)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
        }
    }

    private Set<String> defaultStopWords() {
        return new HashSet<>(Arrays.asList(
                "the", "and", "for", "with", "that", "this", "from", "have", "you", "are", "not", "but",
                "was", "were", "has", "had", "any", "can", "all", "will", "into", "your", "what", "when",
                "how", "why", "where", "which", "then", "also", "such", "than", "its", "a", "an", "of", "to", "in", "on", "as"
        ));
    }

    private Set<String> tokenizeForOverlap(String text) {
        if (!StringUtils.hasText(text)) return Collections.emptySet();
        boolean hasCJK = text.codePoints().anyMatch(cp ->
                (cp >= 0x4E00 && cp <= 0x9FFF) || (cp >= 0x3400 && cp <= 0x4DBF));
        if (hasCJK) {
            Set<String> grams = new HashSet<>();
            String s = text.replaceAll("\\s+", "");
            for (int i = 0; i < s.length() - 1; i++) grams.add(s.substring(i, i + 2));
            return grams;
        } else {
            return Arrays.stream(text.toLowerCase().split("[^a-z0-9_]+"))
                    .filter(w -> w.length() >= 3)
                    .collect(Collectors.toSet());
        }
    }

    /**
     * 构建最终结果
     */
    private String buildFinalResult(List<ScoredContent> fusedResults, String intent) {
        StringBuilder sb = new StringBuilder();
        sb.append("查询意图: ").append(intent).append("\n");
        sb.append("检索到 ").append(fusedResults.size()).append(" 个相关结果\n\n");

        for (int i = 0; i < Math.min(TOP_N_FINAL, fusedResults.size()); i++) {
            ScoredContent sc = fusedResults.get(i);
            String text = extractDisplayText(sc.getContent());
            String snippet = text.length() > 400 ? text.substring(0, 400) + "..." : text;

            String sectionTitle = getMeta(sc.getContent(), "section_title");
            if (!StringUtils.hasText(sectionTitle)) {
                sectionTitle = getMeta(sc.getContent(), "标题"); // 若使用自然语言前缀入库
            }

            sb.append("【结果 ").append(i + 1).append("】\n");
            if (StringUtils.hasText(sectionTitle)) {
                sb.append("章节: ").append(sectionTitle).append("\n");
            }
            sb.append("相关度: ").append(String.format("%.4f", sc.getScore())).append("\n");
            sb.append("内容: ").append(snippet).append("\n\n");
        }
        return sb.toString();
    }

    // =============== 工具方法 ===============

    private String extractTextFromContent(Content content) {
        if (content == null) return "";
        try {
            if (content.textSegment() != null) {
                return content.textSegment().text();
            }
        } catch (Exception ignore) {
        }
        try {
            return content.toString();
        } catch (Exception e) {
            log.warn("无法从Content对象提取文本", e);
            return "";
        }
    }

    // 如果你采用"增强文本入库 + 原文放在 metadata.raw_text"，优先显示 raw_text
    private String extractDisplayText(Content content) {
        String raw = getMeta(content, "raw_text");
        if (StringUtils.hasText(raw)) return raw;
        String t = extractTextFromContent(content);
        return sanitize(t);
    }

    private String getMeta(Content c, String key) {
        try {
            if (c.textSegment() != null && c.textSegment().metadata() != null) {
                Object v = c.textSegment().metadata().toMap().get(key);
                return v == null ? null : String.valueOf(v);
            }
        } catch (Exception ignore) {
        }
        return null;
    }

    // 可选：移除可能的人工标签痕迹
    private static final Pattern BLOCK_MATH = Pattern.compile(
            "```math\\s*\\R(.*?)\n```",                // 匹配 ```math ... ``` 块
            Pattern.DOTALL);

    private static final Pattern LINE_TITLE_OR_KEYWORD = Pattern.compile(
            "^\\s*(标题|关键词)\\s*:.*\\R?",             // 匹配"标题:"或"关键词:"开头的整行
            Pattern.MULTILINE);

    /**
     * 删除 ```math ... ``` 块中的 "标题: xxx" 和 "关键词: xxx" 行
     */
    private String sanitize(String text) {
        if (text == null) {
            return "";
        }

        Matcher m = BLOCK_MATH.matcher(text);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String blockBody = m.group(1);                       // 取出 ```math 里的内容
            String cleaned = LINE_TITLE_OR_KEYWORD               // 删除指定行
                    .matcher(blockBody)
                    .replaceAll("");
            m.appendReplacement(sb,
                    Matcher.quoteReplacement("```math\n" + cleaned + "\n```"));
        }
        m.appendTail(sb);
        return sb.toString().trim();
    }

    /**
     * 修复：使用文本内容的哈希值作为唯一标识，而不是使用不存在的id()方法
     */
    private String contentKey(Content content) {
        // 获取文本内容
        String text = extractTextFromContent(content);

        // 如果有元数据中的唯一标识，优先使用
        String docId = getMeta(content, "document_id");
        if (StringUtils.hasText(docId)) {
            String position = getMeta(content, "position");
            if (StringUtils.hasText(position)) {
                return docId + "_" + position;
            }
            return docId;
        }

        // 否则使用文本的哈希值
        return sha256(text);
    }

    private String sha256(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] d = md.digest(s.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(d.length * 2);
            for (byte b : d) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return Integer.toHexString(s.hashCode());
        }
    }

    @PreDestroy
    public void destroy() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    @Override
    protected boolean shouldUpdateGraphOnFailure() {
        return true;
    }
}