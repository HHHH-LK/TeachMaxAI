package com.aiproject.smartcampus.model.store;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * 统一记忆管理器 - 解决记忆存储隔离和一致性问题，并提供检索能力降低幻觉
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class UnifiedMemoryManager {

    private final LocalStore localStore;
    private final ChatMemoryStore chatMemoryStore;

    private static final String MEMORY_PREFIX = "smart_campus_summary_memory:";
    private static final int MAX_MEMORY_ROUNDS = 10; // 最多保留10轮对话（每轮=用户+AI）
    private static final int MAX_MESSAGES = MAX_MEMORY_ROUNDS * 2;

    // 每个memoryId一把锁，避免并发写导致覆盖
    private final Map<String, ReentrantLock> locks = new ConcurrentHashMap<>();

    /**
     * 统一存储会话记录（安全合并 + 截断 + 双存储落地）
     *
     * @param userId      用户ID
     * @param userMessage 用户消息
     * @param aiResponse  AI回复
     */
    public void storeConversation(String userId, String userMessage, String aiResponse) {
        if (isBlank(userId) || isBlank(userMessage) || isBlank(aiResponse)) {
            log.warn("存储参数不完整或为空，跳过记忆存储");
            return;
        }

        String memoryId = buildMemoryId(userId);
        ReentrantLock lock = locks.computeIfAbsent(memoryId, id -> new ReentrantLock());

        lock.lock();
        try {
            // 读取现有消息（以ChatMemoryStore为优先，空则从LocalStore恢复）
            List<ChatMessage> existing = loadUnifiedMessages(userId, memoryId);

            // 合并新增一轮消息
            List<ChatMessage> merged = new ArrayList<>(existing.size() + 2);
            merged.addAll(existing);
            merged.add(UserMessage.from(userMessage));
            merged.add(AiMessage.from(aiResponse));

            // 截断到最多N轮，确保从用户消息开始且为偶数条
            List<ChatMessage> trimmed = trimToMaxRounds(merged, MAX_MESSAGES);

            // 将完整列表同步更新两个存储（替换式更新）
            persistToStores(userId, memoryId, trimmed);

            log.debug("统一记忆存储成功，userId: {}, memoryId: {}, 当前条数: {}", userId, memoryId, trimmed.size());
        } catch (Exception e) {
            log.error("统一记忆存储失败，userId: {}", userId, e);
        } finally {
            lock.unlock();
        }
    }

    /**
     * 获取用户记忆消息（只返回截断后的列表）
     *
     * @param userId 用户ID
     */
    public List<ChatMessage> getMemoryMessages(String userId) {
        if (isBlank(userId)) {
            log.warn("用户ID为空，返回空记忆");
            return Collections.emptyList();
        }

        String memoryId = buildMemoryId(userId);
        ReentrantLock lock = locks.computeIfAbsent(memoryId, id -> new ReentrantLock());

        lock.lock();
        try {
            List<ChatMessage> existing = loadUnifiedMessages(userId, memoryId);
            return trimToMaxRounds(existing, MAX_MESSAGES);
        } catch (Exception e) {
            log.warn("获取用户记忆失败，userId: {}, 返回空记忆", userId, e);
            return Collections.emptyList();
        } finally {
            lock.unlock();
        }
    }

    /**
     * 兼容旧签名（但不使用该参数）
     */
    @Deprecated
    public List<ChatMessage> getMemoryMessages(String userId, String unusedUserMessage) {
        return getMemoryMessages(userId);
    }

    /**
     * 清除用户记忆（两个存储同时清除）
     */
    public void clearUserMemory(String userId) {
        if (isBlank(userId)) {
            log.warn("用户ID为空，无法清除记忆");
            return;
        }

        String memoryId = buildMemoryId(userId);
        ReentrantLock lock = locks.computeIfAbsent(memoryId, id -> new ReentrantLock());

        lock.lock();
        try {
            chatMemoryStore.deleteMessages(memoryId);
            localStore.deleteMessages(userId);
            log.info("成功清除用户记忆，userId: {}", userId);
        } catch (Exception e) {
            log.error("清除用户记忆失败，userId: {}", userId, e);
        } finally {
            lock.unlock();
        }
    }

    /**
     * 获取记忆统计信息（尽量按成对轮次统计）
     */
    public MemoryStats getMemoryStats(String userId) {
        try {
            List<ChatMessage> messages = getMemoryMessages(userId);
            int rounds = countRounds(messages);

            return MemoryStats.builder()
                    .userId(userId)
                    .totalMessages(messages.size())
                    .conversationRounds(rounds)
                    .memoryId(buildMemoryId(userId))
                    .build();

        } catch (Exception e) {
            log.error("获取记忆统计失败，userId: {}", userId, e);
            return MemoryStats.empty(userId);
        }
    }

    // =============================================================================
    // 新增：记忆检索（仅返回与查询相关的片段，降低幻觉）
    // =============================================================================

    /**
     * 基于BM25的简单检索，将“对话轮次（用户+AI）”作为检索单元。
     * - 只返回最相关的轮次，避免把无关历史塞给模型
     * - 支持邻居窗口，保留上下文
     */
    public MemorySearchResult searchRelevantMemory(String userId, String query, MemorySearchOptions options) {
        if (isBlank(userId) || isBlank(query)) {
            return MemorySearchResult.empty(userId, buildMemoryId(userId));
        }
        if (options == null) {
            options = MemorySearchOptions.defaultOptions();
        }

        String memoryId = buildMemoryId(userId);
        ReentrantLock lock = locks.computeIfAbsent(memoryId, id -> new ReentrantLock());

        lock.lock();
        try {
            List<ChatMessage> messages = getMemoryMessages(userId); // 已经截断到最多N轮
            if (messages.isEmpty()) {
                return MemorySearchResult.empty(userId, memoryId);
            }

            // 构造轮次
            List<Turn> turns = buildTurns(messages);

            // 计算DF/IDF
            List<String> qTokens = tokenizeCnEn(query);
            if (qTokens.isEmpty()) {
                return MemorySearchResult.empty(userId, memoryId);
            }
            BM25 bm25 = BM25.fromTurns(turns);

            // 为每个轮次计算分数
            for (Turn t : turns) {
                double score = bm25.score(qTokens, t);
                t.score = score;
            }

            // 过滤 + 排序 + 选TopK
            MemorySearchOptions finalOptions = options;
            List<Turn> ranked = turns.stream()
                    .filter(t -> t.score >= finalOptions.getMinScore())
                    .sorted(Comparator.comparingDouble((Turn t) -> t.score).reversed())
                    .limit(options.getTopK())
                    .collect(Collectors.toList());

            if (ranked.isEmpty()) {
                return MemorySearchResult.empty(userId, memoryId);
            }

            // 加入邻居窗口
            Set<Integer> indices = new TreeSet<>();
            for (Turn t : ranked) {
                for (int i = Math.max(0, t.index - options.getNeighborWindow());
                     i <= Math.min(turns.size() - 1, t.index + options.getNeighborWindow()); i++) {
                    indices.add(i);
                }
            }

            // 合并并按原顺序展开为 ChatMessage 列表
            List<ChatMessage> resultMessages = indices.stream()
                    .sorted()
                    .flatMap(i -> turns.get(i).messages.stream())
                    .collect(Collectors.toList());

            return MemorySearchResult.builder()
                    .userId(userId)
                    .memoryId(memoryId)
                    .query(query)
                    .matchedMessages(resultMessages)
                    .matchedTurnsCount(indices.size())
                    .build();

        } catch (Exception e) {
            log.error("检索记忆失败，userId: {}", userId, e);
            return MemorySearchResult.empty(userId, memoryId);
        } finally {
            lock.unlock();
        }
    }

    /**
     * 以可直接拼接到LLM上下文的字符串返回检索结果（可选）
     */
    public String searchRelevantMemoryAsContext(String userId, String query, MemorySearchOptions options) {
        MemorySearchResult result = searchRelevantMemory(userId, query, options);
        if (result.getMatchedMessages().isEmpty()) {
            return ""; // 没命中就不提供历史，减少幻觉
        }
        StringBuilder sb = new StringBuilder();
        sb.append("以下是与本次问题相关的历史对话片段：\n");
        int roundNo = 1;
        List<ChatMessage> msgs = result.getMatchedMessages();
        for (int i = 0; i < msgs.size(); i += 2) {
            ChatMessage u = msgs.get(i);
            ChatMessage a = (i + 1 < msgs.size()) ? msgs.get(i + 1) : null;
            sb.append("第").append(roundNo++).append("轮\n");
            sb.append("用户：").append(safeText(u)).append("\n");
            if (a != null) {
                sb.append("AI：").append(safeText(a)).append("\n");
            }
        }
        return sb.toString();
    }

    // =============================================================================
    // 私有方法
    // =============================================================================

    private String buildMemoryId(String userId) {
        return MEMORY_PREFIX + userId;
    }

    /**
     * 读取统一视图：优先从ChatMemoryStore读取；若为空尝试从LocalStore恢复并回填。
     */
    private List<ChatMessage> loadUnifiedMessages(String userId, String memoryId) {
        List<ChatMessage> messages = null;
        try {
            messages = chatMemoryStore.getMessages(memoryId);
        } catch (Exception e) {
            log.warn("读取ChatMemoryStore失败，memoryId: {}", memoryId, e);
        }

        if (messages == null || messages.isEmpty()) {
            log.debug("ChatMemoryStore无记忆，尝试从LocalStore恢复");
            messages = recoverFromLocalStore(userId, memoryId);
        }

        if (messages == null) {
            messages = Collections.emptyList();
        }
        return messages;
    }

    /**
     * 将完整（合并+裁剪后）的消息列表写入两个存储（替换式）
     */
    private void persistToStores(String userId, String memoryId, List<ChatMessage> fullMessages) {
        Exception localStoreException = null;
        Exception chatMemoryException = null;

        try {
            chatMemoryStore.updateMessages(memoryId, fullMessages);
        } catch (Exception e) {
            chatMemoryException = e;
            log.warn("ChatMemoryStore更新失败，memoryId: {}", memoryId, e);
        }

        try {
            localStore.updateMessages(userId, fullMessages);
        } catch (Exception e) {
            localStoreException = e;
            log.warn("LocalStore更新失败，userId: {}", userId, e);
        }

        if (localStoreException != null && chatMemoryException != null) {
            log.error("两个记忆存储系统都更新失败，userId: {}, memoryId: {}", userId, memoryId);
        }
    }

    /**
     * 从LocalStore恢复记忆到ChatMemoryStore
     */
    private List<ChatMessage> recoverFromLocalStore(String userId, String memoryId) {
        try {
            List<ChatMessage> localMessages = localStore.getMessages(userId);
            if (localMessages != null && !localMessages.isEmpty()) {
                chatMemoryStore.updateMessages(memoryId, localMessages);
                log.info("从LocalStore恢复记忆成功，userId: {}, 消息数: {}", userId, localMessages.size());
                return localMessages;
            }
        } catch (Exception e) {
            log.warn("从LocalStore恢复记忆失败，userId: {}", userId, e);
        }
        return Collections.emptyList();
    }

    /**
     * 截断到最多N条消息（N为偶数），确保从用户消息开始，以保证轮次完整
     */
    private List<ChatMessage> trimToMaxRounds(List<ChatMessage> messages, int maxMessages) {
        if (messages == null || messages.isEmpty()) return Collections.emptyList();
        if (messages.size() <= maxMessages) {
            // 尽量确保从User开始，如果不是User开头则对齐
            return alignToUserStart(new ArrayList<>(messages));
        }

        List<ChatMessage> sliced = new ArrayList<>(messages.subList(messages.size() - maxMessages, messages.size()));
        sliced = alignToUserStart(sliced);
        if (sliced.size() % 2 != 0 && !sliced.isEmpty()) {
            // 丢弃最后一条不完整的消息，保证偶数条
            sliced.remove(sliced.size() - 1);
        }

        log.debug("记忆消息过多，从{}条截取为{}条（对齐用户开头）", messages.size(), sliced.size());
        return sliced;
    }

    /**
     * 若不是用户消息开头，尝试去掉第一条直到对齐或无消息
     */
    private List<ChatMessage> alignToUserStart(List<ChatMessage> messages) {
        while (!messages.isEmpty() && !(messages.get(0) instanceof UserMessage)) {
            messages.remove(0);
        }
        return messages;
    }

    private int countRounds(List<ChatMessage> messages) {
        if (messages == null || messages.isEmpty()) return 0;
        int rounds = 0;
        for (int i = 0; i + 1 < messages.size(); i += 2) {
            if (messages.get(i) instanceof UserMessage) rounds++;
        }
        return rounds;
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private String safeText(ChatMessage m) {
        if (m == null) return "";
        try {
            return m.text();
        } catch (Throwable t) {
            // 兼容不同版本API：有些版本可能是content()
            try {
                return (String) m.getClass().getMethod("content").invoke(m);
            } catch (Exception e) {
                return String.valueOf(m);
            }
        }
    }

    // =============================================================================
    // 简易检索实现（BM25 + 中英文混合分词/字符粒度）
    // =============================================================================

    private static List<Turn> buildTurns(List<ChatMessage> messages) {
        List<Turn> turns = new ArrayList<>();
        for (int i = 0; i + 1 < messages.size(); i += 2) {
            ChatMessage u = messages.get(i);
            ChatMessage a = messages.get(i + 1);
            String combined = (safe(u) + "\n" + safe(a)).trim();
            List<String> tokens = tokenizeCnEn(combined);
            Map<String, Integer> tf = new HashMap<>();
            for (String tok : tokens) {
                tf.merge(tok, 1, Integer::sum);
            }
            Turn t = new Turn();
            t.index = turns.size();
            t.messages = Arrays.asList(u, a);
            t.tokens = tokens;
            t.tf = tf;
            t.length = tokens.size();
            t.text = combined;
            turns.add(t);
        }
        return turns;
    }

    private static String safe(ChatMessage m) {
        if (m == null) return "";
        try {
            return m.text() == null ? "" : m.text();
        } catch (Throwable t) {
            return "";
        }
    }

    // 中英文混合tokenizer：
    // - 英文/数字按\\w+切词
    // - 中文按单字（去标点）近似切分（避免引入第三方分词器）
    // - 统一小写 + 归一化 + 停用词过滤（简版）
    private static List<String> tokenizeCnEn(String text) {
        if (text == null) return Collections.emptyList();
        String norm = Normalizer.normalize(text, Normalizer.Form.NFKC)
                .toLowerCase(Locale.ROOT)
                .replaceAll("[\\p{Punct}]+", " "); // 去标点

        List<String> tokens = new ArrayList<>();
        // 英文/数字
        String[] en = norm.split("\\s+");
        for (String w : en) {
            if (w.matches(".*[a-z0-9].*")) {
                String cleaned = w.replaceAll("[^a-z0-9_]", "");
                if (!cleaned.isEmpty() && !EN_STOP.contains(cleaned)) {
                    tokens.add(cleaned);
                }
            }
        }
        // 中文（单字）
        for (int i = 0; i < norm.length(); i++) {
            char c = norm.charAt(i);
            if (Character.UnicodeScript.of(c) == Character.UnicodeScript.HAN) {
                String s = String.valueOf(c);
                if (!CN_STOP.contains(s)) {
                    tokens.add(s);
                }
            }
        }
        return tokens;
    }

    private static final Set<String> EN_STOP = new HashSet<>(Arrays.asList(
            "the", "is", "are", "am", "a", "an", "and", "or", "to", "of", "in", "on", "for", "with", "this", "that", "it", "as", "at", "by", "be", "was", "were", "from", "but", "not"
    ));
    private static final Set<String> CN_STOP = new HashSet<>(Arrays.asList(
            "的", "了", "呢", "啊", "吧", "吗", "在", "是", "有", "就", "也", "都", "很", "还", "和", "与", "及", "或", "被", "把", "给", "我", "你", "他", "她", "它"
    ));

    @Data
    private static class Turn {
        int index;
        List<ChatMessage> messages;
        String text;
        List<String> tokens;
        Map<String, Integer> tf;
        int length;
        double score;
    }

    private static class BM25 {
        private final int N;              // 文档数（轮次）
        private final double avgdl;       // 平均文档长度
        private final Map<String, Integer> df; // 词项的文档频次
        private final double k1 = 1.2;
        private final double b = 0.75;

        private BM25(int N, double avgdl, Map<String, Integer> df) {
            this.N = N;
            this.avgdl = avgdl;
            this.df = df;
        }

        static BM25 fromTurns(List<Turn> turns) {
            Map<String, Integer> df = new HashMap<>();
            int totalLen = 0;
            for (Turn t : turns) {
                totalLen += t.length;
                Set<String> uniq = new HashSet<>(t.tokens);
                for (String tok : uniq) {
                    df.merge(tok, 1, Integer::sum);
                }
            }
            double avgdl = turns.isEmpty() ? 0 : (double) totalLen / turns.size();
            return new BM25(turns.size(), avgdl, df);
        }

        double score(List<String> qTokens, Turn t) {
            double score = 0.0;
            for (String q : qTokens) {
                int f = t.tf.getOrDefault(q, 0);
                if (f == 0) continue;
                int dfq = df.getOrDefault(q, 0);
                // idf + BM25
                double idf = Math.log((N - dfq + 0.5) / (dfq + 0.5) + 1.0);
                double denom = f + k1 * (1 - b + b * t.length / Math.max(1.0, avgdl));
                score += idf * (f * (k1 + 1)) / denom;
            }
            return score;
        }
    }

    // =============================================================================
    // DTOs
    // =============================================================================

    @Data
    @Builder
    @AllArgsConstructor
    public static class MemoryStats {
        private String userId;
        private String memoryId;
        private int totalMessages;
        private int conversationRounds;

        public static MemoryStats empty(String userId) {
            return MemoryStats.builder()
                    .userId(userId)
                    .memoryId("")
                    .totalMessages(0)
                    .conversationRounds(0)
                    .build();
        }
    }

    @Data
    @Builder
    @AllArgsConstructor
    public static class MemorySearchOptions {
        private int topK;             // 返回最相关的轮次个数
        private double minScore;      // 最低分阈值（无命中则返回空）
        private int neighborWindow;   // 每个命中轮次两侧额外保留的轮次数

        public static MemorySearchOptions defaultOptions() {
            return MemorySearchOptions.builder()
                    .topK(3)
                    .minScore(0.2)
                    .neighborWindow(0)
                    .build();
        }
    }

    @Data
    @Builder
    @AllArgsConstructor
    public static class MemorySearchResult {
        private String userId;
        private String memoryId;
        private String query;
        private List<ChatMessage> matchedMessages; // 展开后的消息序列（用户+AI+...）
        private int matchedTurnsCount;

        public static MemorySearchResult empty(String userId, String memoryId) {
            return MemorySearchResult.builder()
                    .userId(userId)
                    .memoryId(memoryId)
                    .query("")
                    .matchedMessages(Collections.emptyList())
                    .matchedTurnsCount(0)
                    .build();
        }
    }
}