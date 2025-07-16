package com.aiproject.smartcampus.model.summer;

import com.aiproject.smartcampus.commons.client.ResultCilent;
import com.aiproject.smartcampus.commons.utils.UserLocalThreadUtils;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import static com.aiproject.smartcampus.model.prompts.SystemPrompts.OPTIMIZED_SUMMER_PROMPT;

/**
 * @program: SmartCampus
 * @description: 优化的模型结果处理器 - 提升响应速度
 * @author: lk
 * @create: 2025-05-28 00:33
 **/

import java.util.concurrent.ConcurrentHashMap;
import java.util.Arrays;

@Service
@Slf4j
@RequiredArgsConstructor
public class ModelSummer {

    private final ResultCilent resultCilent;
    private final ChatLanguageModel chatLanguageModel;

    // 性能优化配置
    private static final int MAX_INPUT_LENGTH = 2500; // 减少输入长度，提升速度
    private static final int TASK_TIMEOUT_SECONDS = 6; // 缩短超时时间
    private static final int TOTAL_TIMEOUT_SECONDS = 12; // 总超时时间
    private static final int MAX_RESULTS_PER_INTENT = 2; // 减少每个意图的结果数量
    private static final int MAX_TOTAL_RESULTS = 6; // 限制总结果数量

    // 质量控制配置
    private static final int MIN_RESULT_LENGTH = 20; // 最小结果长度
    private static final double SIMILARITY_THRESHOLD = 0.8; // 相似度阈值

    private static final String DEFAULT_RESPONSE = "根据当前信息，我正在为您整理相关内容，请稍等片刻。";

    /**
     * 高效高质量的主处理方法
     */
    public String summer(List<String> intents) {
        long startTime = System.currentTimeMillis();

        try {
            // 1. 快速验证和预处理
            if (intents == null || intents.isEmpty()) {
                return DEFAULT_RESPONSE;
            }

            // 2. 智能意图优先级排序
            List<String> prioritizedIntents = prioritizeIntents(intents);
            log.debug("优先级排序后的意图: {}", prioritizedIntents);

            // 3. 并行获取高质量结果
            Map<String, List<String>> intentResults = getHighQualityResults(prioritizedIntents);

            // 4. 智能结果筛选和去重
            List<String> qualityResults = selectQualityResults(intentResults);

            if (qualityResults.isEmpty()) {
                return DEFAULT_RESPONSE;
            }

            // 5. 高效汇总生成
            String result = generateHighQualitySummary(qualityResults);

            long duration = System.currentTimeMillis() - startTime;
            log.info("EnhancedModelSummer处理完成，耗时: {}ms，结果质量评分: {}",
                    duration, evaluateQuality(result));

            return result;

        } catch (Exception e) {
            log.error("ModelSummer处理异常", e);
            return "系统正在为您准备回答，请稍后重试。";
        }
    }

    /**
     * 智能意图优先级排序
     */
    private List<String> prioritizeIntents(List<String> intents) {
        // 定义意图重要性权重
        Map<String, Integer> intentWeights = Map.of(
                "search", 10,      // 搜索类最高优先级
                "question", 9,     // 问答类次之
                "analysis", 8,     // 分析类
                "recommendation", 7, // 推荐类
                "calculation", 6,  // 计算类
                "education", 5,    // 教育类
                "general", 1       // 通用类最低
        );

        return intents.stream()
                .distinct()
                .sorted((a, b) -> intentWeights.getOrDefault(b, 0) - intentWeights.getOrDefault(a, 0))
                .limit(3) // 最多处理3个意图
                .collect(Collectors.toList());
    }

    /**
     * 获取高质量结果
     */
    private Map<String, List<String>> getHighQualityResults(List<String> intents) {
        Map<String, List<String>> results = new ConcurrentHashMap<>();

        // 使用并行流提升效率
        List<CompletableFuture<Void>> futures = intents.stream()
                .map(intent -> CompletableFuture
                        .runAsync(() -> {
                            try {
                                List<String> intentResults = processIntentWithQuality(intent);
                                if (!intentResults.isEmpty()) {
                                    results.put(intent, intentResults);
                                }
                            } catch (Exception e) {
                                log.warn("意图 {} 处理失败: {}", intent, e.getMessage());
                            }
                        })
                        .orTimeout(TASK_TIMEOUT_SECONDS, TimeUnit.SECONDS))
                .collect(Collectors.toList());

        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .get(TOTAL_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("部分意图处理超时，使用已完成结果");
        }

        return results;
    }

    /**
     * 高质量意图处理
     */
    private List<String> processIntentWithQuality(String intent) {
        try {
            List<CompletableFuture<String>> futures = resultCilent.getResult(intent);
            if (futures == null || futures.isEmpty()) {
                return new ArrayList<>();
            }

            // 等待结果完成
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .get(TASK_TIMEOUT_SECONDS - 1, TimeUnit.SECONDS);

            // 收集并质量筛选
            return futures.stream()
                    .map(future -> {
                        try {
                            return future.get();
                        } catch (Exception e) {
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .filter(this::isQualityResult)
                    .limit(MAX_RESULTS_PER_INTENT)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.warn("意图 {} 处理异常", intent);
            return new ArrayList<>();
        }
    }

    /**
     * 质量结果判断
     */
    private boolean isQualityResult(String result) {
        if (result == null || result.trim().isEmpty()) {
            return false;
        }

        String trimmed = result.trim();

        // 长度检查
        if (trimmed.length() < MIN_RESULT_LENGTH) {
            return false;
        }

        // 内容质量检查
        if (trimmed.matches(".*[。！？].*")) {
            return true;
        }

        if (trimmed.length() > 50 && !trimmed.equals(trimmed.toUpperCase())) {
            return true;
        }

        return false;
    }

    /**
     * 智能结果选择和去重
     */
    private List<String> selectQualityResults(Map<String, List<String>> intentResults) {
        List<String> allResults = new ArrayList<>();

        // 按优先级收集结果
        String[] priority = {"search", "question", "analysis", "recommendation"};
        for (String intent : priority) {
            List<String> results = intentResults.get(intent);
            if (results != null) {
                allResults.addAll(results);
            }
        }

        // 添加其他意图结果
        intentResults.entrySet().stream()
                .filter(entry -> !Arrays.asList(priority).contains(entry.getKey()))
                .forEach(entry -> allResults.addAll(entry.getValue()));

        // 智能去重和筛选
        return deduplicateAndRank(allResults)
                .stream()
                .limit(MAX_TOTAL_RESULTS)
                .collect(Collectors.toList());
    }

    /**
     * 智能去重和排序（增强异常处理）
     */
    private List<String> deduplicateAndRank(List<String> results) {
        if (results == null || results.isEmpty()) {
            return new ArrayList<>();
        }

        List<String> deduplicated = new ArrayList<>();

        for (String result : results) {
            if (result == null || result.trim().isEmpty()) {
                continue;
            }

            boolean isDuplicate = false;

            try {
                // 简单的相似度检查
                for (String existing : deduplicated) {
                    if (calculateSimilarity(result, existing) > SIMILARITY_THRESHOLD) {
                        isDuplicate = true;
                        break;
                    }
                }
            } catch (Exception e) {
                log.debug("相似度计算异常，跳过去重检查: {}", e.getMessage());
                // 如果相似度计算失败，进行简单的字符串比较
                isDuplicate = deduplicated.contains(result);
            }

            if (!isDuplicate) {
                deduplicated.add(result);
            }
        }

        // 按长度和质量排序，增加异常处理
        try {
            return deduplicated.stream()
                    .sorted((a, b) -> {
                        try {
                            int qualityA = calculateQualityScore(a);
                            int qualityB = calculateQualityScore(b);
                            return Integer.compare(qualityB, qualityA);
                        } catch (Exception e) {
                            log.debug("质量评分异常，使用长度排序: {}", e.getMessage());
                            return Integer.compare(b.length(), a.length());
                        }
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("排序异常，返回原始去重结果: {}", e.getMessage());
            return deduplicated;
        }
    }

    /**
     * 计算文本相似度（修复版）
     */
    private double calculateSimilarity(String text1, String text2) {
        if (text1 == null || text2 == null) return 0.0;
        if (text1.equals(text2)) return 1.0;

        // 修复：使用HashSet避免重复元素异常
        Set<String> words1 = new HashSet<>(Arrays.asList(text1.toLowerCase().split("\\s+")));
        Set<String> words2 = new HashSet<>(Arrays.asList(text2.toLowerCase().split("\\s+")));

        // 移除空字符串
        words1.removeIf(String::isEmpty);
        words2.removeIf(String::isEmpty);

        if (words1.isEmpty() && words2.isEmpty()) return 1.0;
        if (words1.isEmpty() || words2.isEmpty()) return 0.0;

        Set<String> intersection = new HashSet<>(words1);
        intersection.retainAll(words2);

        Set<String> union = new HashSet<>(words1);
        union.addAll(words2);

        return union.isEmpty() ? 0.0 : (double) intersection.size() / union.size();
    }

    /**
     * 计算结果质量分数
     */
    private int calculateQualityScore(String result) {
        int score = 0;

        // 长度分数（适中长度得分高）
        int length = result.length();
        if (length >= 50 && length <= 300) {
            score += 10;
        } else if (length > 300) {
            score += 5;
        }

        // 完整性分数
        if (result.contains("。") || result.contains("！") || result.contains("？")) {
            score += 15;
        }

        // 信息密度分数
        long sentenceCount = result.chars().filter(ch -> ch == '。' || ch == '！' || ch == '？').count();
        if (sentenceCount >= 2) {
            score += 10;
        }

        // 专业性分数
        if (result.matches(".*根据.*|.*显示.*|.*表明.*")) {
            score += 5;
        }

        return score;
    }

    /**
     * 生成高质量汇总
     */
    private String generateHighQualitySummary(List<String> results) {
        String summaryInput = String.join("\n", results);

        // 智能截断
        if (summaryInput.length() > MAX_INPUT_LENGTH) {
            summaryInput = intelligentTruncate(summaryInput);
        }

        String userMemory = UserLocalThreadUtils.getUserMemory();
        String enhancedPrompt = buildEnhancedPrompt(userMemory, results.size());

        long modelStartTime = System.currentTimeMillis();

        try {
            ChatResponse response = chatLanguageModel.chat(
                    SystemMessage.from(OPTIMIZED_SUMMER_PROMPT + enhancedPrompt),
                    UserMessage.from(summaryInput)
            );

            log.debug("模型调用耗时: {}ms", System.currentTimeMillis() - modelStartTime);

            String rawResponse = response.aiMessage().text();
            return enhanceResponse(rawResponse);

        } catch (Exception e) {
            log.error("模型调用失败", e);
            return DEFAULT_RESPONSE;
        }
    }

    /**
     * 构建增强提示词
     */
    private String buildEnhancedPrompt(String userMemory, int resultCount) {
        return String.format("""
                
                ## 当前任务上下文：
                - 用户需求：%s
                - 可用信息量：%d条高质量结果
                - 期望输出：300-500字的专业回答
                
                ## 质量标准：
                1. 信息准确性：严格基于提供的资料
                2. 逻辑清晰性：重要信息优先，层次分明
                3. 实用价值：提供可操作的具体建议
                4. 语言亲和：专业而不失温暖
                
                ## 效率要求：
                - 开头直接回答核心问题
                - 中间3个要点支撑
                - 结尾实用建议
                
                请生成高质量的专业回答。
                """,
                userMemory != null ? userMemory : "通用咨询",
                resultCount);
    }

    /**
     * 智能截断
     */
    private String intelligentTruncate(String input) {
        String[] sentences = input.split("([。！？])");
        StringBuilder result = new StringBuilder();
        int currentLength = 0;

        for (int i = 0; i < sentences.length && currentLength < MAX_INPUT_LENGTH; i++) {
            String sentence = sentences[i];
            if (currentLength + sentence.length() <= MAX_INPUT_LENGTH) {
                result.append(sentence);
                if (i < sentences.length - 1) {
                    result.append("。");
                }
                currentLength += sentence.length() + 1;
            } else {
                break;
            }
        }

        return result.toString();
    }

    /**
     * 响应增强处理
     */
    private String enhanceResponse(String response) {
        if (response == null || response.trim().isEmpty()) {
            return DEFAULT_RESPONSE;
        }

        String enhanced = response.trim();

        // 格式优化
        enhanced = optimizeFormat(enhanced);

        // 数学公式转换
        enhanced = convertMathFormulas(enhanced);

        // 质量验证
        if (evaluateQuality(enhanced) < 60) {
            log.warn("生成的回答质量较低，使用默认回答");
            return DEFAULT_RESPONSE;
        }

        return enhanced;
    }

    /**
     * 格式优化
     */
    private String optimizeFormat(String text) {
        // 强制Markdown清理
        text = text.replaceAll("\\*\\*([^*]+)\\*\\*", "$1");
        text = text.replaceAll("\\*([^*]+)\\*", "$1");
        text = text.replaceAll("^#{1,6}\\s+", "");
        text = text.replaceAll("\n#{1,6}\\s+", "\n");
        text = text.replaceAll("^[-*+]\\s+", "");
        text = text.replaceAll("\n[-*+]\\s+", "\n");

        // 智能换行
        text = text.replaceAll("([。！？])([\\u4e00-\\u9fff])", "$1\n\n$2");
        text = text.replaceAll("([。！？])\\s*(首先|其次|另外|最后|总之|但是|然而|因此|所以)", "$1\n\n$2");

        // 清理多余空行
        text = text.replaceAll("\n{3,}", "\n\n");

        return text.trim();
    }

    /**
     * 数学公式转换
     */
    private String convertMathFormulas(String text) {
        text = text.replaceAll("\\$?\\\\lim_\\{([^}]+)\\\\to\\s*([^}]+)\\}\\$?", "lim($1→$2)");
        text = text.replaceAll("\\$?\\\\frac\\{([^}]+)\\}\\{([^}]+)\\}\\$?", "($1)/($2)");
        text = text.replaceAll("\\$?([a-zA-Z])\\^2\\$?", "$1²");
        text = text.replaceAll("\\$?([a-zA-Z])\\^3\\$?", "$1³");
        text = text.replaceAll("\\$?\\\\sqrt\\{([^}]+)\\}\\$?", "√($1)");
        text = text.replaceAll("\\$+", "");

        return text;
    }

    /**
     * 评估回答质量
     */
    private int evaluateQuality(String response) {
        if (response == null || response.trim().isEmpty()) {
            return 0;
        }

        int score = 0;
        String text = response.trim();

        // 长度合理性 (25分)
        int length = text.length();
        if (length >= 200 && length <= 600) {
            score += 25;
        } else if (length >= 100) {
            score += 15;
        }

        // 结构完整性 (25分)
        if (text.contains("首先") || text.contains("其次")) {
            score += 15;
        }
        if (text.contains("根据") || text.contains("显示")) {
            score += 10;
        }

        // 内容丰富性 (25分)
        long sentenceCount = text.chars().filter(ch -> ch == '。' || ch == '！' || ch == '？').count();
        if (sentenceCount >= 3) {
            score += 25;
        } else if (sentenceCount >= 2) {
            score += 15;
        }

        // 格式规范性 (25分)
        if (!text.contains("**") && !text.contains("*") && !text.contains("#")) {
            score += 25;
        }

        return score;
    }
}