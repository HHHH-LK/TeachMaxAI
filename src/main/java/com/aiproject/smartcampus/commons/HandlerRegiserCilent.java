package com.aiproject.smartcampus.commons;

import com.aiproject.smartcampus.model.intent.handler.Handler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Order(4)
@Slf4j
@Component
public class HandlerRegiserCilent {

    // 精确匹配映射
    private static final Map<String, Handler> exactMatchMap = new HashMap<>();
    // 处理器元数据映射
    private static final Map<Handler, HandlerMetadata> handlerMetadataMap = new HashMap<>();
    // 优先级匹配规则
    private static final Map<Pattern, HandlerMatcher> priorityMatchers = new LinkedHashMap<>();

    public static final ReadWriteLock lock = new ReentrantReadWriteLock();

    // 处理器元数据类
    private static class HandlerMetadata {
        String description;
        List<String> exactKeywords;
        List<String> semanticKeywords;
        List<Pattern> patterns;
        int priority;
        String category;

        HandlerMetadata(String description, int priority, String category) {
            this.description = description;
            this.exactKeywords = new ArrayList<>();
            this.semanticKeywords = new ArrayList<>();
            this.patterns = new ArrayList<>();
            this.priority = priority;
            this.category = category;
        }
    }

    // 处理器匹配器接口
    private interface HandlerMatcher {
        boolean matches(String intent, HandlerMetadata metadata);
        int getPriority();
    }

    /**
     * 注册一个 Handler
     */
    public void add(String usage, Handler handler) {
        lock.writeLock().lock();
        try {
            exactMatchMap.put(usage, handler);

            if (usage.contains("处理器")) {
                registerHandlerWithMetadata(usage, handler);
            }

            log.debug("服务[{}]（{}）添加成功", usage, handler.getClass().getSimpleName());
        } catch (Exception e) {
            log.error("添加服务失败", e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 基于意图描述注册处理器，支持多个意图
     */
    public void addWithIntents(String description, Handler handler, String... intents) {
        lock.writeLock().lock();
        try {
            // 注册主描述
            exactMatchMap.put(description, handler);
            registerHandlerWithMetadata(description, handler);

            // 注册额外的意图
            for (String intent : intents) {
                exactMatchMap.put(intent, handler);
            }

            log.debug("服务[{}]及其意图添加成功，意图数量: {}", description, intents.length);
        } catch (Exception e) {
            log.error("添加服务及意图失败", e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 注册处理器元数据
     */
    private void registerHandlerWithMetadata(String description, Handler handler) {
        HandlerMetadata metadata = createHandlerMetadata(description);
        handlerMetadataMap.put(handler, metadata);
        log.debug("处理器元数据注册成功: {} -> {}", handler.getClass().getSimpleName(), metadata.category);
    }

    /**
     * 创建处理器元数据
     */
    private HandlerMetadata createHandlerMetadata(String description) {
        String normalizedDesc = description.toLowerCase();
        HandlerMetadata metadata;

        if (normalizedDesc.contains("增强检索") || normalizedDesc.contains("知识库") || normalizedDesc.contains("rag")) {
            metadata = new HandlerMetadata(description, 1, "RAG");
            metadata.exactKeywords.addAll(Arrays.asList("检索", "搜索", "查找", "知识库", "查询", "文档"));
            metadata.semanticKeywords.addAll(Arrays.asList("获取信息", "寻找资料", "查阅文档", "知识查询"));
            metadata.patterns.add(Pattern.compile(".*在.*?中.*?查找.*"));
            metadata.patterns.add(Pattern.compile(".*从.*?获取.*?信息.*"));
            metadata.patterns.add(Pattern.compile(".*关于.*?的.*?资料.*"));
        } else if (normalizedDesc.contains("工具") || normalizedDesc.contains("功能操作") || normalizedDesc.contains("function")) {
            metadata = new HandlerMetadata(description, 2, "TOOL");
            metadata.exactKeywords.addAll(Arrays.asList("工具", "调用", "执行", "计算", "转换", "函数"));
            metadata.semanticKeywords.addAll(Arrays.asList("使用工具", "执行操作", "进行计算", "数据转换"));
            metadata.patterns.add(Pattern.compile(".*调用.*?工具.*"));
            metadata.patterns.add(Pattern.compile(".*执行.*?功能.*"));
            metadata.patterns.add(Pattern.compile(".*使用.*?(计算|转换|处理).*"));
        } else if (normalizedDesc.contains("大语言模型") || normalizedDesc.contains("对话") || normalizedDesc.contains("llm")) {
            metadata = new HandlerMetadata(description, 3, "LLM");
            metadata.exactKeywords.addAll(Arrays.asList("对话", "聊天", "问答", "回答", "生成", "分析", "解释"));
            metadata.semanticKeywords.addAll(Arrays.asList("文本生成", "内容创作", "智能对话", "问题解答"));
            metadata.patterns.add(Pattern.compile(".*生成.*?(文本|内容|报告).*"));
            metadata.patterns.add(Pattern.compile(".*分析.*?(问题|情况|数据).*"));
            metadata.patterns.add(Pattern.compile(".*解释.*?(概念|原理|现象).*"));
        } else if (normalizedDesc.contains("记忆") || normalizedDesc.contains("上下文") || normalizedDesc.contains("历史")) {
            metadata = new HandlerMetadata(description, 4, "MEMORY");
            metadata.exactKeywords.addAll(Arrays.asList("记忆", "上下文", "历史", "之前", "继续"));
            metadata.semanticKeywords.addAll(Arrays.asList("历史对话", "上下文信息", "之前的内容"));
            metadata.patterns.add(Pattern.compile(".*基于.*?(历史|记忆|上下文).*"));
            metadata.patterns.add(Pattern.compile(".*继续.*?(之前|刚才).*"));
        } else {
            metadata = new HandlerMetadata(description, 5, "GENERAL");
        }

        return metadata;
    }

    /**
     * 智能获取Handler - 多级匹配策略
     */
    public Handler getHandler(String intent) {
        lock.readLock().lock();
        try {
            log.debug("正在查找处理器，意图描述: {}", intent);
            String normalizedIntent = normalize(intent);

            // 1. 精确匹配
            Handler exactHandler = exactMatchMap.get(intent);
            if (exactHandler != null) {
                log.debug("精确匹配成功: {} -> {}", intent, exactHandler.getClass().getSimpleName());
                return exactHandler;
            }

            // 2. 智能意图匹配
            List<HandlerCandidate> candidates = findCandidates(normalizedIntent);
            if (!candidates.isEmpty()) {
                Handler bestHandler = selectBestHandler(candidates, normalizedIntent);
                if (bestHandler != null) {
                    log.info("智能匹配成功: [{}] -> {} (置信度: {})",
                            intent, bestHandler.getClass().getSimpleName(),
                            candidates.get(0).confidence);
                    return bestHandler;
                }
            }

            // 3. 匹配失败处理
            logMatchFailure(intent, normalizedIntent);
            return null;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 候选处理器类
     */
    private static class HandlerCandidate {
        Handler handler;
        double confidence;
        String matchReason;

        HandlerCandidate(Handler handler, double confidence, String matchReason) {
            this.handler = handler;
            this.confidence = confidence;
            this.matchReason = matchReason;
        }
    }

    /**
     * 查找候选处理器
     */
    private List<HandlerCandidate> findCandidates(String normalizedIntent) {
        List<HandlerCandidate> candidates = new ArrayList<>();

        for (Map.Entry<Handler, HandlerMetadata> entry : handlerMetadataMap.entrySet()) {
            Handler handler = entry.getKey();
            HandlerMetadata metadata = entry.getValue();

            double confidence = calculateConfidence(normalizedIntent, metadata);
            if (confidence > 0.3) { // 置信度阈值
                String matchReason = getMatchReason(normalizedIntent, metadata);
                candidates.add(new HandlerCandidate(handler, confidence, matchReason));
            }
        }

        // 按置信度排序
        candidates.sort((a, b) -> Double.compare(b.confidence, a.confidence));
        return candidates;
    }

    /**
     * 计算匹配置信度
     */
    private double calculateConfidence(String intent, HandlerMetadata metadata) {
        double confidence = 0.0;

        // 1. 精确关键词匹配 (权重: 0.4)
        for (String keyword : metadata.exactKeywords) {
            if (intent.contains(keyword)) {
                confidence += 0.4;
            }
        }

        // 2. 语义关键词匹配 (权重: 0.3)
        for (String semantic : metadata.semanticKeywords) {
            if (intent.contains(semantic) || semanticSimilarity(intent, semantic) > 0.7) {
                confidence += 0.3;
            }
        }

        // 3. 正则模式匹配 (权重: 0.5)
        for (Pattern pattern : metadata.patterns) {
            if (pattern.matcher(intent).matches()) {
                confidence += 0.5;
            }
        }

        // 4. 优先级调整
        confidence = confidence * (1.0 / metadata.priority);

        // 5. 特殊场景增强
        confidence += getScenarioBonus(intent, metadata);

        return Math.min(confidence, 1.0); // 最大置信度为1.0
    }

    /**
     * 获取场景奖励分数
     */
    private double getScenarioBonus(String intent, HandlerMetadata metadata) {
        double bonus = 0.0;

        switch (metadata.category) {
            case "RAG":
                if (intent.matches(".*?(什么是|介绍|了解).*") ||
                        intent.matches(".*?资料.*?在哪.*") ||
                        intent.contains("查找") && intent.contains("信息")) {
                    bonus += 0.2;
                }
                break;
            case "TOOL":
                if (intent.matches(".*?(计算|转换|处理).*?数据.*") ||
                        intent.contains("调用") || intent.contains("执行")) {
                    bonus += 0.2;
                }
                break;
            case "LLM":
                if (intent.matches(".*?(怎么|如何|为什么).*") ||
                        intent.contains("分析") || intent.contains("解释") ||
                        intent.contains("生成")) {
                    bonus += 0.2;
                }
                break;
            case "MEMORY":
                if (intent.contains("之前") || intent.contains("刚才") ||
                        intent.contains("继续") || intent.contains("上次")) {
                    bonus += 0.3;
                }
                break;
        }

        return bonus;
    }

    /**
     * 简单的语义相似度计算
     */
    private double semanticSimilarity(String text1, String text2) {
        Set<String> set1 = new HashSet<>(Arrays.asList(text1.split("\\s+")));
        Set<String> set2 = new HashSet<>(Arrays.asList(text2.split("\\s+")));

        Set<String> intersection = new HashSet<>(set1);
        intersection.retainAll(set2);

        Set<String> union = new HashSet<>(set1);
        union.addAll(set2);

        return union.isEmpty() ? 0.0 : (double) intersection.size() / union.size();
    }

    /**
     * 获取匹配原因
     */
    private String getMatchReason(String intent, HandlerMetadata metadata) {
        List<String> reasons = new ArrayList<>();

        for (String keyword : metadata.exactKeywords) {
            if (intent.contains(keyword)) {
                reasons.add("关键词:" + keyword);
            }
        }

        for (Pattern pattern : metadata.patterns) {
            if (pattern.matcher(intent).matches()) {
                reasons.add("模式匹配");
            }
        }

        return reasons.isEmpty() ? "语义匹配" : String.join(",", reasons);
    }

    /**
     * 选择最佳处理器
     */
    private Handler selectBestHandler(List<HandlerCandidate> candidates, String intent) {
        if (candidates.isEmpty()) {
            return null;
        }

        HandlerCandidate best = candidates.get(0);

        // 如果最高置信度很低，进行二次确认
        if (best.confidence < 0.5) {
            log.warn("最佳匹配置信度较低: {} ({})", best.confidence, best.matchReason);
        }

        // 如果有多个高置信度候选者，选择优先级最高的
        if (candidates.size() > 1 &&
                Math.abs(candidates.get(0).confidence - candidates.get(1).confidence) < 0.1) {

            HandlerMetadata bestMeta = handlerMetadataMap.get(best.handler);
            HandlerMetadata secondMeta = handlerMetadataMap.get(candidates.get(1).handler);

            if (secondMeta.priority < bestMeta.priority) {
                best = candidates.get(1);
                log.debug("基于优先级选择处理器: {}", best.handler.getClass().getSimpleName());
            }
        }

        return best.handler;
    }

    /**
     * 记录匹配失败信息
     */
    private void logMatchFailure(String originalIntent, String normalizedIntent) {
        log.warn("未找到匹配的处理器");
        log.warn("原始意图: {}", originalIntent);
        log.warn("标准化意图: {}", normalizedIntent);
        log.warn("当前注册的处理器类别: {}",
                handlerMetadataMap.values().stream()
                        .map(m -> m.category)
                        .distinct()
                        .collect(Collectors.joining(", ")));
    }

    private String normalize(String text) {
        return text.toLowerCase()
                .replaceAll("[，。！？、；：\"''()【】{}\\[\\]]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    /**
     * 获取处理器调试信息
     */
    public String getHandlerDebugInfo() {
        lock.readLock().lock();
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("=== 处理器注册信息 ===\n");

            for (Map.Entry<Handler, HandlerMetadata> entry : handlerMetadataMap.entrySet()) {
                Handler handler = entry.getKey();
                HandlerMetadata metadata = entry.getValue();

                sb.append(String.format("处理器: %s\n", handler.getClass().getSimpleName()));
                sb.append(String.format("  类别: %s (优先级: %d)\n", metadata.category, metadata.priority));
                sb.append(String.format("  关键词: %s\n", metadata.exactKeywords));
                sb.append(String.format("  语义词: %s\n", metadata.semanticKeywords));
                sb.append("---\n");
            }

            return sb.toString();
        } finally {
            lock.readLock().unlock();
        }
    }

    // 保持向后兼容的方法
    public Handler getHandlerByUsage(String usage) {
        lock.readLock().lock();
        try {
            return exactMatchMap.get(usage);
        } finally {
            lock.readLock().unlock();
        }
    }

    public Map<String, Handler> getAllHandlers() {
        lock.readLock().lock();
        try {
            return Collections.unmodifiableMap(new HashMap<>(exactMatchMap));
        } finally {
            lock.readLock().unlock();
        }
    }

    public void remove(String usage) {
        lock.writeLock().lock();
        try {
            Handler removed = exactMatchMap.remove(usage);
            if (removed != null) {
                handlerMetadataMap.remove(removed);
                log.info("服务[{}]移除成功", usage);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void clearAll() {
        lock.writeLock().lock();
        try {
            exactMatchMap.clear();
            handlerMetadataMap.clear();
            log.info("已清空所有服务注册");
        } finally {
            lock.writeLock().unlock();
        }
    }

    public String getRegistrationStats() {
        lock.readLock().lock();
        try {
            Map<String, Long> categoryStats = handlerMetadataMap.values().stream()
                    .collect(Collectors.groupingBy(m -> m.category, Collectors.counting()));

            return String.format("注册统计 - 总数: %d, 分类: %s",
                    exactMatchMap.size(), categoryStats);
        } finally {
            lock.readLock().unlock();
        }
    }
}