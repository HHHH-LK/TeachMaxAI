package com.aiproject.smartcampus.commons.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 意图匹配工具类 - 提供更精确的意图识别和匹配能力
 */
@Slf4j
@Component
public class IntentMatcherUtility {

    // 预定义的意图模式
    private static final Map<String, List<Pattern>> INTENT_PATTERNS = new HashMap<>();

    // 同义词映射
    private static final Map<String, Set<String>> SYNONYM_MAP = new HashMap<>();

    // 停用词集合
    private static final Set<String> STOP_WORDS = new HashSet<>();

    // 意图优先级权重
    private static final Map<String, Double> INTENT_WEIGHTS = new HashMap<>();

    static {
        initializeIntentPatterns();
        initializeSynonymMap();
        initializeStopWords();
        initializeIntentWeights();
    }

    /**
     * 初始化意图模式
     */
    private static void initializeIntentPatterns() {
        // RAG 检索模式
        INTENT_PATTERNS.put("RAG", Arrays.asList(
                Pattern.compile(".*?(查找|搜索|检索|寻找|获取).*?(信息|资料|内容|文档|知识).*?"),
                Pattern.compile(".*?在.*?(知识库|文档|资料).*?(中|里).*?(查找|搜索|检索).*?"),
                Pattern.compile(".*?(什么是|介绍|了解|查看).*?"),
                Pattern.compile(".*?(有关|关于|涉及|相关).*?(信息|资料|内容).*?"),
                Pattern.compile(".*?从.*?(获取|提取|查找).*?(信息|数据|内容).*?")
        ));

        // 工具调用模式
        INTENT_PATTERNS.put("TOOL", Arrays.asList(
                Pattern.compile(".*?(调用|使用|执行|运行).*?(工具|功能|函数|方法).*?"),
                Pattern.compile(".*?(计算|转换|处理|格式化).*?(数据|文件|信息).*?"),
                Pattern.compile(".*?(生成|创建|制作|产生).*?(文件|报告|数据).*?"),
                Pattern.compile(".*?帮我.*?(计算|转换|处理|生成).*?"),
                Pattern.compile(".*?(发送|传输|导出|保存).*?(到|至|为).*?")
        ));

        // LLM 对话模式
        INTENT_PATTERNS.put("LLM", Arrays.asList(
                Pattern.compile(".*?(怎么|如何|为什么|为啥).*?"),
                Pattern.compile(".*?(分析|解释|说明|解答|回答).*?"),
                Pattern.compile(".*?(生成|创作|写|编写).*?(文本|文章|报告|总结).*?"),
                Pattern.compile(".*?(比较|对比|区别|差异).*?"),
                Pattern.compile(".*?(建议|推荐|意见|看法).*?"),
                Pattern.compile(".*?帮我.*?(想|思考|分析|解决).*?")
        ));

        // 记忆/上下文模式
        INTENT_PATTERNS.put("MEMORY", Arrays.asList(
                Pattern.compile(".*?(之前|刚才|刚刚|刚才说|刚才提到).*?"),
                Pattern.compile(".*?(继续|接着|然后).*?(之前|刚才|上次).*?"),
                Pattern.compile(".*?(历史|记录|对话历史|聊天记录).*?"),
                Pattern.compile(".*?(基于|根据).*?(之前|历史|记忆|上下文).*?"),
                Pattern.compile(".*?(回忆|想起|记得).*?(之前|刚才|上次).*?")
        ));
    }

    /**
     * 初始化同义词映射
     */
    private static void initializeSynonymMap() {
        SYNONYM_MAP.put("查找", Set.of("搜索", "检索", "寻找", "找到", "获取", "查询"));
        SYNONYM_MAP.put("生成", Set.of("创建", "制作", "产生", "生产", "构建", "创作"));
        SYNONYM_MAP.put("分析", Set.of("解析", "研究", "分解", "剖析", "解读"));
        SYNONYM_MAP.put("处理", Set.of("操作", "执行", "运行", "处置", "加工"));
        SYNONYM_MAP.put("获取", Set.of("取得", "获得", "得到", "拿到", "收集"));
        SYNONYM_MAP.put("转换", Set.of("变换", "转化", "改变", "变更", "转变"));
        SYNONYM_MAP.put("调用", Set.of("使用", "运用", "执行", "启动", "触发"));
    }

    /**
     * 初始化停用词
     */
    private static void initializeStopWords() {
        STOP_WORDS.addAll(Arrays.asList(
                "的", "了", "和", "是", "在", "有", "我", "你", "他", "她", "它", "们",
                "这", "that", "不", "也", "都", "就", "可以", "能够", "应该", "需要",
                "一个", "一些", "几个", "很多", "所有", "全部", "其他", "另外"
        ));
    }

    /**
     * 初始化意图权重
     */
    private static void initializeIntentWeights() {
        INTENT_WEIGHTS.put("RAG", 0.9);      // 检索任务通常很明确
        INTENT_WEIGHTS.put("TOOL", 0.8);     // 工具调用通常有明确动词
        INTENT_WEIGHTS.put("MEMORY", 1.0);   // 记忆相关通常有明确时间词
        INTENT_WEIGHTS.put("LLM", 0.7);      // 对话生成相对模糊
    }

    /**
     * 智能意图匹配 - 主要入口方法
     */
    public IntentMatchResult matchIntent(String userInput) {
        if (userInput == null || userInput.trim().isEmpty()) {
            return new IntentMatchResult("UNKNOWN", 0.0, "输入为空");
        }

        String normalizedInput = normalizeText(userInput);
        log.debug("标准化输入: {}", normalizedInput);

        // 1. 模式匹配
        Map<String, Double> patternScores = calculatePatternScores(normalizedInput);

        // 2. 关键词匹配
        Map<String, Double> keywordScores = calculateKeywordScores(normalizedInput);

        // 3. 语义匹配
        Map<String, Double> semanticScores = calculateSemanticScores(normalizedInput);

        // 4. 综合评分
        Map<String, Double> finalScores = combineScores(patternScores, keywordScores, semanticScores);

        // 5. 选择最佳匹配
        return selectBestMatch(finalScores, normalizedInput);
    }

    /**
     * 计算模式匹配分数
     */
    private Map<String, Double> calculatePatternScores(String input) {
        Map<String, Double> scores = new HashMap<>();

        for (Map.Entry<String, List<Pattern>> entry : INTENT_PATTERNS.entrySet()) {
            String intentType = entry.getKey();
            List<Pattern> patterns = entry.getValue();

            double maxScore = 0.0;
            for (Pattern pattern : patterns) {
                if (pattern.matcher(input).matches()) {
                    maxScore = Math.max(maxScore, 0.8);
                } else if (pattern.matcher(input).find()) {
                    maxScore = Math.max(maxScore, 0.6);
                }
            }

            if (maxScore > 0) {
                scores.put(intentType, maxScore);
            }
        }

        return scores;
    }

    /**
     * 计算关键词匹配分数
     */
    private Map<String, Double> calculateKeywordScores(String input) {
        Map<String, Double> scores = new HashMap<>();

        // RAG 关键词
        if (containsKeywords(input, Arrays.asList("检索", "搜索", "查找", "知识库", "文档", "资料"))) {
            scores.put("RAG", 0.7);
        }

        // TOOL 关键词
        if (containsKeywords(input, Arrays.asList("调用", "工具", "执行", "计算", "转换", "处理"))) {
            scores.put("TOOL", 0.7);
        }

        // LLM 关键词
        if (containsKeywords(input, Arrays.asList("生成", "分析", "解释", "创作", "回答", "对话"))) {
            scores.put("LLM", 0.6);
        }

        // MEMORY 关键词
        if (containsKeywords(input, Arrays.asList("之前", "历史", "记忆", "继续", "上次", "刚才"))) {
            scores.put("MEMORY", 0.8);
        }

        return scores;
    }

    /**
     * 计算语义匹配分数
     */
    private Map<String, Double> calculateSemanticScores(String input) {
        Map<String, Double> scores = new HashMap<>();

        // 基于同义词的语义匹配
        for (Map.Entry<String, Set<String>> synonymEntry : SYNONYM_MAP.entrySet()) {
            String baseWord = synonymEntry.getKey();
            Set<String> synonyms = synonymEntry.getValue();

            if (synonyms.stream().anyMatch(input::contains)) {
                // 根据同义词匹配增加相应意图的分数
                addSemanticScore(scores, baseWord, 0.4);
            }
        }

        return scores;
    }

    /**
     * 添加语义分数
     */
    private void addSemanticScore(Map<String, Double> scores, String keyword, double score) {
        switch (keyword) {
            case "查找":
                scores.put("RAG", scores.getOrDefault("RAG", 0.0) + score);
                break;
            case "生成":
                scores.put("LLM", scores.getOrDefault("LLM", 0.0) + score);
                break;
            case "处理":
            case "转换":
            case "调用":
                scores.put("TOOL", scores.getOrDefault("TOOL", 0.0) + score);
                break;
            case "获取":
                scores.put("RAG", scores.getOrDefault("RAG", 0.0) + score * 0.8);
                scores.put("TOOL", scores.getOrDefault("TOOL", 0.0) + score * 0.6);
                break;
        }
    }

    /**
     * 综合评分
     */
    private Map<String, Double> combineScores(Map<String, Double> patternScores,
                                              Map<String, Double> keywordScores,
                                              Map<String, Double> semanticScores) {
        Map<String, Double> finalScores = new HashMap<>();
        Set<String> allIntents = new HashSet<>();
        allIntents.addAll(patternScores.keySet());
        allIntents.addAll(keywordScores.keySet());
        allIntents.addAll(semanticScores.keySet());

        for (String intent : allIntents) {
            double patternScore = patternScores.getOrDefault(intent, 0.0);
            double keywordScore = keywordScores.getOrDefault(intent, 0.0);
            double semanticScore = semanticScores.getOrDefault(intent, 0.0);

            // 加权平均：模式匹配 50%，关键词匹配 30%，语义匹配 20%
            double combinedScore = patternScore * 0.5 + keywordScore * 0.3 + semanticScore * 0.2;

            // 应用意图权重
            double intentWeight = INTENT_WEIGHTS.getOrDefault(intent, 1.0);
            finalScores.put(intent, combinedScore * intentWeight);
        }

        return finalScores;
    }

    /**
     * 选择最佳匹配
     */
    private IntentMatchResult selectBestMatch(Map<String, Double> scores, String input) {
        if (scores.isEmpty()) {
            return new IntentMatchResult("LLM", 0.3, "默认对话处理");
        }

        // 找到最高分数的意图
        Map.Entry<String, Double> bestMatch = scores.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .orElse(null);

        if (bestMatch == null || bestMatch.getValue() < 0.3) {
            return new IntentMatchResult("LLM", 0.3, "分数过低，使用默认处理");
        }

        // 检查是否有接近的竞争者
        List<Map.Entry<String, Double>> sortedScores = scores.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .collect(Collectors.toList());

        String matchReason = buildMatchReason(sortedScores, input);

        return new IntentMatchResult(bestMatch.getKey(), bestMatch.getValue(), matchReason);
    }

    /**
     * 构建匹配原因说明
     */
    private String buildMatchReason(List<Map.Entry<String, Double>> sortedScores, String input) {
        StringBuilder reason = new StringBuilder();

        if (sortedScores.size() > 1) {
            double topScore = sortedScores.get(0).getValue();
            double secondScore = sortedScores.get(1).getValue();

            if (topScore - secondScore < 0.1) {
                reason.append("竞争激烈，");
            }
        }

        String topIntent = sortedScores.get(0).getKey();
        switch (topIntent) {
            case "RAG":
                reason.append("检测到检索/查找意图");
                break;
            case "TOOL":
                reason.append("检测到工具调用意图");
                break;
            case "LLM":
                reason.append("检测到对话生成意图");
                break;
            case "MEMORY":
                reason.append("检测到记忆/上下文意图");
                break;
            default:
                reason.append("基于综合分析");
        }

        return reason.toString();
    }

    /**
     * 检查是否包含关键词
     */
    private boolean containsKeywords(String input, List<String> keywords) {
        return keywords.stream().anyMatch(input::contains);
    }

    /**
     * 文本标准化
     */
    private String normalizeText(String text) {
        return text.toLowerCase()
                .replaceAll("[，。！？、；：\"''()【】{}\\[\\]\\-_=+]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    /**
     * 意图匹配结果类
     */
    public static class IntentMatchResult {
        private final String intentType;
        private final double confidence;
        private final String reason;

        public IntentMatchResult(String intentType, double confidence, String reason) {
            this.intentType = intentType;
            this.confidence = confidence;
            this.reason = reason;
        }

        public String getIntentType() {
            return intentType;
        }

        public double getConfidence() {
            return confidence;
        }

        public String getReason() {
            return reason;
        }

        @Override
        public String toString() {
            return String.format("IntentMatchResult{type='%s', confidence=%.2f, reason='%s'}",
                    intentType, confidence, reason);
        }
    }

    /**
     * 批量意图匹配 - 用于测试和验证
     */
    public Map<String, IntentMatchResult> batchMatchIntents(List<String> inputs) {
        Map<String, IntentMatchResult> results = new HashMap<>();
        for (String input : inputs) {
            results.put(input, matchIntent(input));
        }
        return results;
    }

    /**
     * 获取意图匹配统计信息
     */
    public String getMatchingStats() {
        return String.format(
                "意图匹配工具统计:\n" +
                        "- 支持的意图类型: %d\n" +
                        "- 预定义模式数量: %d\n" +
                        "- 同义词组数量: %d\n" +
                        "- 停用词数量: %d",
                INTENT_PATTERNS.size(),
                INTENT_PATTERNS.values().stream().mapToInt(List::size).sum(),
                SYNONYM_MAP.size(),
                STOP_WORDS.size()
        );
    }
}