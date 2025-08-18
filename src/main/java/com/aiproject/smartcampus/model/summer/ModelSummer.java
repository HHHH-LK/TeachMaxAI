package com.aiproject.smartcampus.model.summer;

import com.aiproject.smartcampus.commons.client.ResultCilent;
import com.aiproject.smartcampus.commons.utils.UserLocalThreadUtils;
import com.aiproject.smartcampus.model.store.UnifiedMemoryManager;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.aiproject.smartcampus.model.prompts.SystemPrompts.OPTIMIZED_SUMMER_PROMPT;

@Service
@Slf4j
@RequiredArgsConstructor
public class ModelSummer {

    private final ResultCilent resultCilent;
    private final ChatLanguageModel chatLanguageModel;
    private final UnifiedMemoryManager memoryManager;

    // 性能优化配置
    private static final int MAX_INPUT_LENGTH = 2500; // 外部资料拼接最大输入长度
    private static final int TASK_TIMEOUT_SECONDS = 6;
    private static final int TOTAL_TIMEOUT_SECONDS = 12;
    private static final int MAX_RESULTS_PER_INTENT = 2;
    private static final int MAX_TOTAL_RESULTS = 6;

    // 质量控制配置
    private static final int MIN_RESULT_LENGTH = 20;
    private static final double SIMILARITY_THRESHOLD = 0.8;

    // 记忆检索配置
    private static final UnifiedMemoryManager.MemorySearchOptions SEARCH_OPTS =
            UnifiedMemoryManager.MemorySearchOptions.builder()
                    .topK(3)
                    .minScore(0.2)
                    .neighborWindow(0)
                    .build();

    private static final String DEFAULT_RESPONSE = "根据当前信息，我正在为您整理相关内容，请稍等片刻。";

    /**
     * 兼容旧入口（无用户问题时，不注入历史记忆）
     */
    public String summer(List<String> intents) {
        return summer(intents, null);
    }

    /**
     * 新入口：带用户问题，结合“相关历史片段”+“外部资料”做高质量总结（抗幻觉）
     */
    public String summer(List<String> intents, String userQuery) {
        long startTime = System.currentTimeMillis();

        try {
            // 1) 快速验证和预处理
            if (intents == null || intents.isEmpty()) {
                return DEFAULT_RESPONSE;
            }

            // 2) 智能意图优先级排序
            List<String> prioritizedIntents = prioritizeIntents(intents);
            log.debug("优先级排序后的意图: {}", prioritizedIntents);

            // 3) 并行获取高质量结果（外部资料）
            Map<String, List<String>> intentResults = getHighQualityResults(prioritizedIntents);

            // 4) 智能结果筛选和去重
            List<String> qualityResults = selectQualityResults(intentResults);
            if (qualityResults.isEmpty()) {
                return DEFAULT_RESPONSE;
            }

            // 5) 相关记忆检索（仅与当前问题相关）
            String userId = getCurrentUserId();
            String memoryContext = "";
            if (userQuery != null && !userQuery.trim().isEmpty()) {
                memoryContext = memoryManager.searchRelevantMemoryAsContext(userId, userQuery, SEARCH_OPTS);
            }

            // 6) 高效汇总生成（严格基于“相关历史片段 + 外部资料”）
            String result = generateHighQualitySummary(userQuery, qualityResults, memoryContext);

            long duration = System.currentTimeMillis() - startTime;
            log.info("ModelSummer处理完成，耗时: {}ms，结果质量评分: {}", duration, evaluateQuality(result));

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
        Map<String, Integer> intentWeights = Map.of(
                "search", 10,
                "question", 9,
                "analysis", 8,
                "recommendation", 7,
                "calculation", 6,
                "education", 5,
                "general", 1
        );

        return intents.stream()
                .distinct()
                .sorted((a, b) -> intentWeights.getOrDefault(b, 0) - intentWeights.getOrDefault(a, 0))
                .limit(3)
                .collect(Collectors.toList());
    }

    /**
     * 获取高质量结果
     */
    private Map<String, List<String>> getHighQualityResults(List<String> intents) {
        Map<String, List<String>> results = new ConcurrentHashMap<>();

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
                .toList();

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

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .get(TASK_TIMEOUT_SECONDS - 1, TimeUnit.SECONDS);

            return futures.stream()
                    .map(f -> {
                        try { return f.get(); } catch (Exception e) { return null; }
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
        if (result == null || result.trim().isEmpty()) return false;

        String trimmed = result.trim();
        if (trimmed.length() < MIN_RESULT_LENGTH) return false;

        // 简易质量启发
        if (trimmed.matches(".*[。！？].*")) return true;
        if (trimmed.length() > 50 && !trimmed.equals(trimmed.toUpperCase())) return true;

        return false;
    }

    /**
     * 智能结果选择和去重
     */
    private List<String> selectQualityResults(Map<String, List<String>> intentResults) {
        List<String> allResults = new ArrayList<>();

        String[] priority = {"search", "question", "analysis", "recommendation"};
        for (String intent : priority) {
            List<String> results = intentResults.get(intent);
            if (results != null) allResults.addAll(results);
        }

        intentResults.entrySet().stream()
                .filter(entry -> !Arrays.asList(priority).contains(entry.getKey()))
                .forEach(entry -> allResults.addAll(entry.getValue()));

        return deduplicateAndRank(allResults).stream()
                .limit(MAX_TOTAL_RESULTS)
                .collect(Collectors.toList());
    }

    /**
     * 智能去重与排序
     */
    private List<String> deduplicateAndRank(List<String> results) {
        if (results == null || results.isEmpty()) return new ArrayList<>();

        List<String> deduplicated = new ArrayList<>();
        for (String r : results) {
            if (r == null || r.trim().isEmpty()) continue;

            boolean dup = false;
            try {
                for (String ex : deduplicated) {
                    if (calculateSimilarity(r, ex) > SIMILARITY_THRESHOLD) { dup = true; break; }
                }
            } catch (Exception e) {
                log.debug("相似度计算异常，使用直接包含判断");
                dup = deduplicated.contains(r);
            }
            if (!dup) deduplicated.add(r);
        }

        try {
            return deduplicated.stream()
                    .sorted((a, b) -> {
                        try {
                            int qa = calculateQualityScore(a), qb = calculateQualityScore(b);
                            return Integer.compare(qb, qa);
                        } catch (Exception e) {
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
     * 计算文本相似度（Jaccard）
     */
    private double calculateSimilarity(String t1, String t2) {
        if (t1 == null || t2 == null) return 0.0;
        if (t1.equals(t2)) return 1.0;

        Set<String> s1 = new HashSet<>(Arrays.asList(t1.toLowerCase().split("\\s+")));
        Set<String> s2 = new HashSet<>(Arrays.asList(t2.toLowerCase().split("\\s+")));
        s1.removeIf(String::isEmpty);
        s2.removeIf(String::isEmpty);

        if (s1.isEmpty() && s2.isEmpty()) return 1.0;
        if (s1.isEmpty() || s2.isEmpty()) return 0.0;

        Set<String> inter = new HashSet<>(s1); inter.retainAll(s2);
        Set<String> union = new HashSet<>(s1); union.addAll(s2);

        return union.isEmpty() ? 0.0 : (double) inter.size() / union.size();
    }

    /**
     * 计算结果质量分数
     */
    private int calculateQualityScore(String result) {
        int score = 0;
        int len = result.length();
        if (len >= 50 && len <= 300) score += 10; else if (len > 300) score += 5;
        if (result.contains("。") || result.contains("！") || result.contains("？")) score += 15;
        long sentences = result.chars().filter(ch -> ch == '。' || ch == '！' || ch == '？').count();
        if (sentences >= 2) score += 10;
        if (result.matches(".*根据.*|.*显示.*|.*表明.*")) score += 5;
        return score;
    }

    /**
     * 生成高质量汇总（抗幻觉）
     */
    private String generateHighQualitySummary(String userQuery, List<String> results, String memoryContext) {
        String evidenceBlock = buildEvidenceBlock(results);

        // 智能截断（仅对外部资料块）
        if (evidenceBlock.length() > MAX_INPUT_LENGTH) {
            evidenceBlock = intelligentTruncate(evidenceBlock);
        }

        // 构建用户消息载荷：用户问题 + 相关历史片段 + 外部资料
        String userPayload = buildUserPayload(userQuery, memoryContext, evidenceBlock);

        long modelStart = System.currentTimeMillis();
        try {
            ChatResponse response = chatLanguageModel.chat(
                    SystemMessage.from(OPTIMIZED_SUMMER_PROMPT),
                    UserMessage.from(userPayload)
            );
            log.debug("模型调用耗时: {}ms", System.currentTimeMillis() - modelStart);

            String raw = response.aiMessage().text();
            return enhanceResponse(raw);

        } catch (Exception e) {
            log.error("模型调用失败", e);
            return DEFAULT_RESPONSE;
        }
    }

    private String buildEvidenceBlock(List<String> results) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < results.size(); i++) {
            String item = results.get(i);
            // 单条资料再控长，避免超长段落
            if (item.length() > 600) {
                item = item.substring(0, 600) + "...";
            }
            sb.append("- 资料").append(i + 1).append("：").append(item.trim()).append("\n");
        }
        return sb.toString().trim();
    }

    private String buildUserPayload(String userQuery, String memoryContext, String evidenceBlock) {
        String uq = (userQuery == null || userQuery.trim().isEmpty()) ? "通用咨询" : userQuery.trim();
        String mc = (memoryContext == null || memoryContext.trim().isEmpty()) ? "无" : memoryContext.trim();

        return new StringBuilder()
                .append("【当前用户问题】\n").append(uq).append("\n\n")
                .append("【相关历史对话片段】\n").append(mc).append("\n\n")
                .append("【外部资料（供总结）】\n").append(evidenceBlock).toString();
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
                if (i < sentences.length - 1) result.append("。");
                currentLength += sentence.length() + 1;
            } else break;
        }
        return result.toString();
    }

    /**
     * 响应增强处理
     */
    private String enhanceResponse(String response) {
        if (response == null || response.trim().isEmpty()) return DEFAULT_RESPONSE;
        String enhanced = response.trim();
        enhanced = optimizeFormat(enhanced);
        enhanced = convertMathFormulas(enhanced);

        if (evaluateQuality(enhanced) < 60) {
            log.warn("生成的回答质量较低，使用默认回答");
            return DEFAULT_RESPONSE;
        }
        return enhanced;
    }

    /**
     * 格式优化：清理轻量Markdown、将 ¥ 转换为空行、智能换行
     */
    private String optimizeFormat(String text) {
        // 将模型段落分隔符 ¥ 转为空行
        text = text.replace("¥", "\n\n");

        // 轻量Markdown清理
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
        if (response == null || response.trim().isEmpty()) return 0;

        int score = 0;
        String text = response.trim();
        int length = text.length();
        if (length >= 200 && length <= 600) score += 25;
        else if (length >= 100) score += 15;

        if (text.contains("首先") || text.contains("其次")) score += 15;
        if (text.contains("根据") || text.contains("显示")) score += 10;

        long sentenceCount = text.chars().filter(ch -> ch == '。' || ch == '！' || ch == '？').count();
        if (sentenceCount >= 3) score += 25; else if (sentenceCount >= 2) score += 15;

        if (!text.contains("**") && !text.contains("*") && !text.contains("#")) score += 25;

        return score;
    }

    /**
     * 获取当前用户ID
     */
    private String getCurrentUserId() {
        try {
            if (UserLocalThreadUtils.getUserInfo() != null &&
                    UserLocalThreadUtils.getUserInfo().getUserId() != null) {
                String userId = UserLocalThreadUtils.getUserInfo().getUserId().toString();
                log.debug("从UserLocalThreadUtils获取用户ID: {}", userId);
                return userId;
            }
        } catch (Exception e) {
            log.debug("从UserLocalThreadUtils获取用户ID失败: {}", e.getMessage());
        }
        String defaultUserId = "default_user_1";
        log.debug("使用默认用户ID: {}", defaultUserId);
        return defaultUserId;
    }
}