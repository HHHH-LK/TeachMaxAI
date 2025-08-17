package com.aiproject.smartcampus.service.impl;

import com.aiproject.smartcampus.commons.client.Result;
import com.aiproject.smartcampus.commons.utils.PasswordEncryptionUtils;
import com.aiproject.smartcampus.commons.utils.UserToTypeUtils;
import com.aiproject.smartcampus.exception.StudentExpection;
import com.aiproject.smartcampus.mapper.*;
import com.aiproject.smartcampus.pojo.bo.SimpleKnowledgeAnalysisBO;
import com.aiproject.smartcampus.pojo.dto.StudentAnswerDTO;
import com.aiproject.smartcampus.pojo.dto.StudentExamAnswerDTO;
import com.aiproject.smartcampus.pojo.po.QuestionBank;
import com.aiproject.smartcampus.pojo.po.Student;
import com.aiproject.smartcampus.pojo.po.StudentAnswer;
import com.aiproject.smartcampus.pojo.po.User;
import com.aiproject.smartcampus.pojo.vo.StudentKnowledgePointVO;
import com.aiproject.smartcampus.pojo.vo.StudentSelectAllVO;
import com.aiproject.smartcampus.pojo.vo.StudentWrongQuestionVO;
import com.aiproject.smartcampus.service.StudentService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PreDestroy;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class StudentServiceImpl extends ServiceImpl<StudentMapper, Student> implements StudentService {

    private final StudentMapper studentMapper;
    private final UserMapper userMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final UserToTypeUtils userToTypeUtils;
    private final QuestionBankMapper questionBankMapper;
    private final ExamMapper examMapper;
    private final StudentAnswerMapper studentAnswerMapper;
    private final ChatLanguageModel chatLanguageModel;
    private final KnowledgePointMapper knowledgePointMapper;

    // 并行执行器（学习链接生成/校验/刷新）
    private final ExecutorService studyLinkExecutor = new ThreadPoolExecutor(
            8, 8, 60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(512),
            r -> {
                Thread t = new Thread(r);
                t.setName("study-link-exec-" + t.getId());
                t.setDaemon(false);
                return t;
            },
            new ThreadPoolExecutor.CallerRunsPolicy()
    );

    @PreDestroy
    public void shutdown() {
        try {
            studyLinkExecutor.shutdown();
            if (!studyLinkExecutor.awaitTermination(8, TimeUnit.SECONDS)) {
                studyLinkExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // ================= 基础学生服务 =================

    @Override
    public List<StudentSelectAllVO> selectAllStudents() {
        return studentMapper.selectAllStudents();
    }

    @Override
    public Student findByStudentNumber(String studentNumber) {
        return studentMapper.findByStudentNumber(studentNumber);
    }

    @Override
    public List<Student> findByClassName(String className) {
        return studentMapper.findByClassName(className);
    }

    @Override
    @Transactional
    public void updateStudent(Student student) {
        User user = new User();
        BeanUtils.copyProperties(student.getUser(), user);
        studentMapper.updateById(student);
        user.setPasswordHash(PasswordEncryptionUtils.encryption(user.getPasswordHash()));
        userMapper.updateById(user);
    }

    @Override
    public String academicAnalysis(String courseId) {
        try {
            String studentId = "1"; // or userToTypeUtils.change();

            // 错题
            log.info("开始检索学生{}错误题目信息", studentId);
            List<StudentWrongQuestionVO> wrongList = studentMapper.selectWrongQuestion(studentId, courseId);
            log.info("检索数据成功,数据条数{}", wrongList.size());
            StringBuilder wrongBuf = new StringBuilder();
            for (StudentWrongQuestionVO vo : wrongList) {
                wrongBuf.append(vo.toString()).append("\n");
            }

            // 知识点掌握
            log.info("开始检索学生{}知识点信息", studentId);
            List<StudentKnowledgePointVO> kpList = knowledgePointMapper.selectKnowledgePointByStudentIdOptionalCourseId(studentId, courseId);
            log.info("检索数据成功,数据条数{}", kpList.size());
            StringBuilder kpBuf = new StringBuilder();
            for (StudentKnowledgePointVO vo : kpList) {
                kpBuf.append(vo.toString()).append("\n");
            }

            String prompt = buildAnalysisPrompt(kpBuf.toString(), wrongBuf.toString());
            log.info("开始生成学情分析报告");
            ChatResponse response = chatLanguageModel.chat(UserMessage.userMessage(prompt));
            log.info("学情分析报告生成完毕");

            return response.aiMessage().text();
        } catch (Exception e) {
            log.info("学习情报生成失败", e);
            throw new RuntimeException("学习情报生成失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void finshExam(StudentExamAnswerDTO studentExamAnswerDTO) {
        List<StudentAnswerDTO> list = studentExamAnswerDTO.getStudentExamAnswerDTOList();
        int i = 0;
        for (StudentAnswerDTO dto : list) {
            Integer qid = dto.getQuestionId();
            QuestionBank qb = questionBankMapper.selectById(qid);
            if (qb == null) {
                log.error("题目{}不存在", qid);
                throw new StudentExpection("考试题目不存在");
            }
            log.info("提取出学生的第{}题目信息", i++);

            String studentId = userToTypeUtils.change();
            StudentAnswer sa = new StudentAnswer();
            sa.setStudentId(Integer.valueOf(studentId));
            sa.setExamId(Integer.valueOf(studentExamAnswerDTO.getExamId()));
            sa.setQuestionId(qid);
            sa.setStudentAnswer(dto.getFormattedAnswer());
            studentAnswerMapper.insert(sa);
        }
        log.info("题目入库完毕,学生一共提交了[{}]道题目", list.size());
    }

    // ================= 学习链接编排（并行生成+校验+评分） =================

    private static final String STUDY_LINK_CACHE_PREFIX = "study:link:";

    interface LinkProvider {
        CompletableFuture<List<Candidate>> getCandidates(QueryContext ctx);
    }

    // LLM 提供者
    class LlmProvider implements LinkProvider {
        private final ChatLanguageModel chat;
        private final String systemPrompt;

        LlmProvider(ChatLanguageModel chat, String systemPrompt) {
            this.chat = chat;
            this.systemPrompt = systemPrompt;
        }

        @Override
        public CompletableFuture<List<Candidate>> getCandidates(QueryContext ctx) {
            return CompletableFuture.supplyAsync(() -> {
                String linkPrompt = buildLinkPromptForProvider(ctx.getKeywords(), ctx.getCandidatesPerProvider(), ctx.getBannedDomains());
                try {
                    String text = chat.chat(Arrays.asList(
                            SystemMessage.systemMessage(systemPrompt),
                            UserMessage.userMessage(linkPrompt)
                    )).aiMessage().text();
                    List<LinkCandidate> links = parseCandidates(text);
                    return links.stream().map(l -> toCandidate(l, "llm")).collect(Collectors.toList());
                } catch (Exception e) {
                    log.warn("LLMProvider 生成候选异常: {}", e.toString());
                    return Collections.emptyList();
                }
            }, studyLinkExecutor);
        }
    }

    // 启发式提供者：权威站点入口
    class HeuristicProvider implements LinkProvider {
        @Override
        public CompletableFuture<List<Candidate>> getCandidates(QueryContext ctx) {
            return CompletableFuture.supplyAsync(() -> {
                List<Candidate> out = new ArrayList<>();
                String first = ctx.getKeywords().stream().findFirst().orElse("学习");
                String q = urlEncode(first);
                out.add(c("https://developer.mozilla.org/zh-CN/search?q=" + q, "MDN 相关文档", "MDN", "heuristic"));
                out.add(c("https://zh.wikipedia.org/w/index.php?search=" + q, "维基百科相关条目", "Wikipedia", "heuristic"));
                out.add(c("https://docs.oracle.com/javase/8/docs/api/", "Oracle Java 官方文档索引", "Oracle Docs", "heuristic"));
                out.add(c("https://www.runoob.com/?s=" + q, "菜鸟教程相关内容", "Runoob", "heuristic"));

                Set<String> banned = new HashSet<>(ctx.getBannedDomains());
                return out.stream().filter(cd -> !banned.contains(extractHost(cd.getUrl()))).collect(Collectors.toList());
            }, studyLinkExecutor);
        }

        private Candidate c(String url, String title, String source, String provider) {
            Candidate cd = new Candidate();
            cd.setUrl(url);
            cd.setTitle(title);
            cd.setSource(source);
            cd.setProvider(provider);
            return cd;
        }
    }

    private static class QueryContext {
        private String studentId;
        private String pointId;
        private Set<String> keywords;
        private int candidatesPerProvider;
        private List<String> bannedDomains;
        private int totalTimeoutSec;

        public String getStudentId() {
            return studentId;
        }

        public void setStudentId(String studentId) {
            this.studentId = studentId;
        }

        public String getPointId() {
            return pointId;
        }

        public void setPointId(String pointId) {
            this.pointId = pointId;
        }

        public Set<String> getKeywords() {
            return keywords;
        }

        public void setKeywords(Set<String> keywords) {
            this.keywords = keywords;
        }

        public int getCandidatesPerProvider() {
            return candidatesPerProvider;
        }

        public void setCandidatesPerProvider(int candidatesPerProvider) {
            this.candidatesPerProvider = candidatesPerProvider;
        }

        public List<String> getBannedDomains() {
            return bannedDomains;
        }

        public void setBannedDomains(List<String> bannedDomains) {
            this.bannedDomains = bannedDomains;
        }

        public int getTotalTimeoutSec() {
            return totalTimeoutSec;
        }

        public void setTotalTimeoutSec(int totalTimeoutSec) {
            this.totalTimeoutSec = totalTimeoutSec;
        }
    }

    private static class Candidate {
        private String url;
        private String title;
        private String source;
        private String provider;
        private String canonicalUrl;
        private double ruleScore;
        private double finalScore;
        private String error;
        private int httpStatus;
        private long fetchMillis;
        private boolean contentMatch;

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getSource() {
            return source;
        }

        public void setSource(String source) {
            this.source = source;
        }

        public String getProvider() {
            return provider;
        }

        public void setProvider(String provider) {
            this.provider = provider;
        }

        public String getCanonicalUrl() {
            return canonicalUrl;
        }

        public void setCanonicalUrl(String canonicalUrl) {
            this.canonicalUrl = canonicalUrl;
        }

        public double getRuleScore() {
            return ruleScore;
        }

        public void setRuleScore(double ruleScore) {
            this.ruleScore = ruleScore;
        }

        public double getFinalScore() {
            return finalScore;
        }

        public void setFinalScore(double finalScore) {
            this.finalScore = finalScore;
        }

        public String getError() {
            return error;
        }

        public void setError(String error) {
            this.error = error;
        }

        public int getHttpStatus() {
            return httpStatus;
        }

        public void setHttpStatus(int httpStatus) {
            this.httpStatus = httpStatus;
        }

        public long getFetchMillis() {
            return fetchMillis;
        }

        public void setFetchMillis(long fetchMillis) {
            this.fetchMillis = fetchMillis;
        }

        public boolean isContentMatch() {
            return contentMatch;
        }

        public void setContentMatch(boolean contentMatch) {
            this.contentMatch = contentMatch;
        }
    }

    static class LinkCandidate {
        public String url;
        public String title;
        public String source;
    }

    private Candidate toCandidate(LinkCandidate l, String provider) {
        Candidate c = new Candidate();
        c.setUrl(l.url);
        c.setTitle(l.title);
        c.setSource(l.source);
        c.setProvider(provider);
        return c;
    }

    // 对外入口：先查缓存，缓存不可用则并行生成并写缓存
    @Override
    public Result<String> getStudyByAgent(String studentId, String pointId) {
        try {
            if (pointId == null || pointId.trim().isEmpty()) {
                return Result.error("pointId 不能为空");
            }

            String cacheKey = STUDY_LINK_CACHE_PREFIX + studentId + ":" + pointId;
            String cached = stringRedisTemplate.opsForValue().get(cacheKey);
            if (cached != null && canAccess(cached, 1500)) {
                log.info("学习链接命中缓存: {}", cached);
                return Result.success(cached);
            }

            // 拉取分析与关键词
            List<String> pointIds = Collections.singletonList(pointId);
            List<SimpleKnowledgeAnalysisBO> analysis = knowledgePointMapper.getSimpleKnowledgeAnalysis(pointIds, studentId);
            if (analysis == null || analysis.isEmpty()) {
                return Result.error("未找到该知识点的分析信息");
            }
            Set<String> keywords = buildKeywords(analysis);

            // 并行生成并选优
            String best = computeBestStudyLink(studentId, pointId, keywords);
            if (best == null) {
                String fallback = buildFallbackQuery(keywords);
                return Result.success(fallback);
            }

            stringRedisTemplate.opsForValue().set(cacheKey, best, 24, TimeUnit.HOURS);
            log.info("最佳学习链接: {}", best);
            return Result.success(best);

        } catch (Exception e) {
            log.error("getStudyByAgent 异常", e);
            return Result.error("生成学习链接失败: " + e.getMessage());
        }
    }

    // 计算最优学习链接（不走缓存）
    private String computeBestStudyLink(String studentId, String pointId, Set<String> keywords) {
        QueryContext ctx = new QueryContext();
        ctx.setStudentId(studentId);
        ctx.setPointId(pointId);
        ctx.setKeywords(keywords);
        ctx.setCandidatesPerProvider(5);
        ctx.setBannedDomains(new ArrayList<>());
        ctx.setTotalTimeoutSec(8);

        List<LinkProvider> providers = Arrays.asList(
                new LlmProvider(chatLanguageModel, createSystemPrompts()),
                new HeuristicProvider()
        );

        List<CompletableFuture<List<Candidate>>> futures = providers.stream()
                .map(p -> p.getCandidates(ctx))
                .toList();

        List<Candidate> all = new ArrayList<>();
        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .get(ctx.getTotalTimeoutSec(), TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("候选生成阶段部分超时，收集已完成结果: {}", e.toString());
        }
        for (CompletableFuture<List<Candidate>> f : futures) {
            all.addAll(f.getNow(Collections.emptyList()));
        }
        if (all.isEmpty()) return null;

        // 去重
        Map<String, Candidate> uniq = new LinkedHashMap<>();
        for (Candidate c : all) {
            String can = canonicalize(c.getUrl());
            if (!skipBanned(can, ctx.getBannedDomains())) {
                c.setCanonicalUrl(can);
                uniq.putIfAbsent(can, c);
            }
        }
        List<Candidate> dedup = new ArrayList<>(uniq.values());
        if (dedup.isEmpty()) return null;

        // 并行抓取评分
        List<CompletableFuture<Candidate>> vFutures = dedup.stream()
                .map(c -> CompletableFuture.supplyAsync(() -> fetchAndScore(c, ctx), studyLinkExecutor))
                .collect(Collectors.toList());

        Candidate best = null;
        for (CompletableFuture<Candidate> f : vFutures) {
            try {
                Candidate c = f.get(5, TimeUnit.SECONDS);
                if (best == null || c.getFinalScore() > best.getFinalScore()) best = c;
            } catch (Exception ignored) {
            }
        }
        if (best == null || best.getFinalScore() <= 0 || best.getUrl() == null) return null;
        return best.getUrl();
    }

    // ================= 定时任务：每30分钟刷新学习链接缓存 =================

    // 每次最多处理的缓存条目，避免对外网/模型造成瞬时高压（可按量调整）
    private static final int MAX_REFRESH_PER_RUN = 200;

    @Scheduled(cron = "0 0/30 * * * ?") // 每30分钟执行
    public void refreshStudyLinksCacheJob() {
        String pattern = STUDY_LINK_CACHE_PREFIX + "*";
        List<String> keys = new ArrayList<>();
        // 使用 SCAN 安全遍历
        try (RedisConnection conn = Objects.requireNonNull(stringRedisTemplate.getConnectionFactory()).getConnection();
             Cursor<byte[]> cursor = conn.scan(ScanOptions.scanOptions().match(pattern).count(500).build())) {
            while (cursor.hasNext() && keys.size() < MAX_REFRESH_PER_RUN) {
                keys.add(new String(cursor.next(), StandardCharsets.UTF_8));
            }
        } catch (Exception e) {
            log.error("扫描学习链接缓存失败", e);
            return;
        }

        if (keys.isEmpty()) {
            log.info("学习链接刷新：无缓存可刷新");
            return;
        }

        log.info("学习链接刷新：准备刷新 {} 条缓存（最多处理 {}）", keys.size(), MAX_REFRESH_PER_RUN);

        AtomicInteger updated = new AtomicInteger(0);
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (String key : keys) {
            futures.add(CompletableFuture.runAsync(() -> {
                try {
                    if (refreshStudyLinkForKey(key)) {
                        updated.incrementAndGet();
                    }
                } catch (Exception e) {
                    log.warn("刷新学习链接失败 key={}", key, e);
                }
            }, studyLinkExecutor));
        }

        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .orTimeout(25, TimeUnit.SECONDS)
                    .join();
        } catch (Exception e) {
            log.warn("学习链接刷新：部分任务超时", e);
        }

        log.info("学习链接刷新：完成，刷新条数={}/{}", updated.get(), keys.size());
    }

    // 刷新单个缓存Key：若不可达或不匹配则重新生成并写回；可达且匹配则仅续期
    private boolean refreshStudyLinkForKey(String key) {
        try {
            String[] parts = key.split(":");
            if (parts.length < 4) return false;
            String studentId = parts[2];
            String pointId = parts[3];

            String url = stringRedisTemplate.opsForValue().get(key);
            // 取关键词
            List<SimpleKnowledgeAnalysisBO> analysis = knowledgePointMapper.getSimpleKnowledgeAnalysis(
                    Collections.singletonList(pointId), studentId);
            if (analysis == null || analysis.isEmpty()) return false;
            Set<String> keywords = buildKeywords(analysis);

            boolean needRecompute = true;
            if (url != null) {
                Validation v = validateUrlForScore(url, keywords);
                if (v.ok && v.contentMatch) {
                    // 可达且匹配，续期
                    stringRedisTemplate.expire(key, 24, TimeUnit.HOURS);
                    needRecompute = false;
                }
            }

            if (!needRecompute) return false;

            // 重新计算
            String best = computeBestStudyLink(studentId, pointId, keywords);
            if (best != null && best.startsWith("http")) {
                stringRedisTemplate.opsForValue().set(key, best, 24, TimeUnit.HOURS);
                log.info("学习链接刷新：已更新 key={} newUrl={}", key, best);
                return true;
            } else {
                log.warn("学习链接刷新：重算失败，保留旧值 key={}, oldUrl={}", key, url);
                return false;
            }
        } catch (Exception e) {
            log.warn("刷新单个学习链接异常 key={}", key, e);
            return false;
        }
    }

    // ==================== 生成/校验/评分工具 ====================

    private String buildLinkPromptForProvider(Set<String> keywords, int count, List<String> bannedDomains) {
        String kws = keywords.isEmpty() ? "无" : String.join("、", keywords);
        String ban = bannedDomains.isEmpty() ? "无" : String.join(", ", bannedDomains);
        return "背景关键词：" + kws + "\n"
                + "请生成 " + count + " 个可直接访问的高质量学习资料链接，避免域名：" + ban + "。\n"
                + "严格按系统消息中的 JSON 输出规范返回，禁止任何额外文本。";
    }

    private Set<String> buildKeywords(List<SimpleKnowledgeAnalysisBO> analysis) {
        Set<String> ks = new LinkedHashSet<>();
        for (SimpleKnowledgeAnalysisBO bo : analysis) {
            if (bo.getPointName() != null) ks.add(bo.getPointName().trim());
            if (bo.getKeywords() != null) {
                String[] arr = bo.getKeywords().split("[,，、;；\\s]+");
                for (String s : arr) {
                    s = s.trim();
                    if (s.length() >= 2) ks.add(s);
                }
            }
        }
        if (ks.size() > 12) {
            return ks.stream().limit(12).collect(Collectors.toCollection(LinkedHashSet::new));
        }
        return ks;
    }

    private Candidate fetchAndScore(Candidate c, QueryContext ctx) {
        long start = System.nanoTime();
        Validation v = validateUrlForScore(c.getUrl(), ctx.getKeywords());
        long ms = (System.nanoTime() - start) / 1_000_000;

        c.setHttpStatus(v.statusCode);
        c.setContentMatch(v.contentMatch);
        c.setFetchMillis(ms);
        c.setError(v.error);

        double score = 0;
        score += v.keywordHits >= 2 ? 0.4 : (v.keywordHits == 1 ? 0.2 : 0.0);
        score += domainWeight(c.getUrl());                         // 0~0.3
        score += c.getUrl().startsWith("https://") ? 0.05 : 0.0;
        score += isChinese(v.bodySample) ? 0.1 : 0.0;              // 中文优先
        score += Math.max(0, 0.15 - Math.min(ms, 3000) / 20000.0); // 越快越好
        if (looksLikeLoginOrPaywall(v.bodySample)) score -= 0.4;   // 登录/付费页惩罚

        if (!v.ok || !v.contentMatch) score = 0.0;
        score = Math.max(0.0, Math.min(1.0, score));

        c.setRuleScore(score);
        c.setFinalScore(score);
        return c;
    }

    private static class Validation {
        String url;
        boolean ok;
        boolean contentMatch;
        int statusCode;
        String error;
        int keywordHits;
        String bodySample;
    }

    private Validation validateUrlForScore(String urlStr, Set<String> keywords) {
        Validation v = new Validation();
        v.url = urlStr;
        v.ok = false;
        v.statusCode = -1;
        v.keywordHits = 0;

        HttpURLConnection conn = null;
        try {
            URL url = new URL(urlStr);
            conn = (HttpURLConnection) url.openConnection();
            conn.setInstanceFollowRedirects(true);
            conn.setConnectTimeout((int) Duration.ofMillis(2500).toMillis());
            conn.setReadTimeout((int) Duration.ofMillis(3500).toMillis());
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (StudyAgent)");
            conn.setRequestProperty("Accept", "text/html,application/json,text/plain;q=0.9,*/*;q=0.8");
            conn.connect();

            int code = conn.getResponseCode();
            v.statusCode = code;
            if (code >= 200 && code < 400) {
                String ctype = conn.getContentType();
                if (ctype != null && (ctype.toLowerCase(Locale.ROOT).contains("text")
                        || ctype.toLowerCase(Locale.ROOT).contains("html")
                        || ctype.toLowerCase(Locale.ROOT).contains("json"))) {
                    try (InputStream is = conn.getInputStream()) {
                        byte[] buf = readLimited(is, 160_000);
                        String body = new String(buf, guessCharset(ctype));
                        v.bodySample = body.substring(0, Math.min(3000, body.length()));

                        int hits = 0;
                        String lower = body.toLowerCase(Locale.ROOT);
                        for (String k : keywords) {
                            if (k != null && !k.isEmpty() && lower.contains(k.toLowerCase(Locale.ROOT))) {
                                hits++;
                            }
                        }
                        v.keywordHits = hits;
                        v.contentMatch = hits >= 1;
                        v.ok = true;
                    }
                } else {
                    v.error = "unsupported-content-type";
                }
            } else {
                v.error = "http-" + code;
            }
        } catch (Exception e) {
            v.error = e.getClass().getSimpleName();
        } finally {
            if (conn != null) conn.disconnect();
        }
        return v;
    }

    private byte[] readLimited(InputStream is, int max) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream(Math.min(max, 32768));
        byte[] tmp = new byte[8192];
        int total = 0;
        int r;
        while ((r = is.read(tmp)) != -1) {
            if (total + r > max) {
                bos.write(tmp, 0, max - total);
                break;
            } else {
                bos.write(tmp, 0, r);
                total += r;
            }
        }
        return bos.toByteArray();
    }

    private String guessCharset(String contentType) {
        if (contentType != null) {
            String[] parts = contentType.split(";");
            for (String p : parts) {
                String lp = p.toLowerCase(Locale.ROOT);
                if (lp.contains("charset=")) {
                    String cs = p.substring(lp.indexOf("charset=") + 8).trim();
                    if (!cs.isEmpty()) return cs.replace("\"", "");
                }
            }
        }
        return StandardCharsets.UTF_8.name();
    }

    private boolean isChinese(String s) {
        if (s == null) return false;
        return s.codePoints().anyMatch(cp -> Character.UnicodeScript.of(cp) == Character.UnicodeScript.HAN);
    }

    private boolean looksLikeLoginOrPaywall(String s) {
        if (s == null) return false;
        String t = s.toLowerCase(Locale.ROOT);
        return t.contains("登录") || t.contains("signin") || t.contains("subscribe") || t.contains("paywall")
                || t.contains("会员") || t.contains("付费") || t.contains("log in");
    }

    private double domainWeight(String url) {
        String host = extractHost(url);
        if (host == null) return 0;
        Map<String, Double> w = new HashMap<>();
        w.put("developer.mozilla.org", 0.30);
        w.put("docs.oracle.com", 0.28);
        w.put("zh.wikipedia.org", 0.25);
        w.put("khanacademy.org", 0.22);
        w.put("runoob.com", 0.18);
        return w.getOrDefault(host, 0.08);
    }

    private String canonicalize(String url) {
        try {
            URL u = new URL(url);
            String host = u.getHost().toLowerCase(Locale.ROOT);
            String path = u.getPath() == null ? "" : u.getPath();
            return "https://" + host + path; // 丢弃 query/fragment
        } catch (Exception e) {
            return url;
        }
    }

    private boolean skipBanned(String url, List<String> bannedDomains) {
        String host = extractHost(url);
        if (host == null) return false;
        for (String b : bannedDomains) if (host.endsWith(b)) return true;
        return false;
    }

    private String extractHost(String urlStr) {
        try {
            return new URL(urlStr).getHost();
        } catch (Exception e) {
            return null;
        }
    }

    private String urlEncode(String s) {
        try {
            return URLEncoder.encode(s, "UTF-8");
        } catch (Exception e) {
            return s;
        }
    }

    private String buildFallbackQuery(Set<String> keywords) {
        String q = (keywords == null || keywords.isEmpty()) ? "学习资料"
                : keywords.stream().limit(5).collect(Collectors.joining(" "));
        try {
            return "https://cn.bing.com/search?q=" + URLEncoder.encode(q, "UTF-8");
        } catch (Exception e) {
            return "https://cn.bing.com/";
        }
    }

    private String createSystemPrompts() {
        String p = """
                你是一名“学习资源链接生成器”，负责为给定的知识点与关键词生成可直接访问、与内容高度相关的学习资料链接。
                
                你的目标（必须全部满足）：
                - 链接可直接访问：无需登录、无需付费、非被屏蔽或需跳过中间跳转页（Landing/短链/广告页/404/搜索结果页等一律禁止）。
                - 内容高度相关：落地页正文应围绕用户消息中给出的知识点与关键词展开（用户消息会提供关键词、禁用域以及上一轮错误反馈）。
                - 来源权威稳定：优先官方文档、权威教材/学术/教育机构、知名技术社区或高质量教程站点；HTTPS 优先。
                - 域名约束：严禁使用用户消息中列出的禁用域名；避免内容农场、聚合复制站、明显广告/营销页面。
                - 语言偏好：优先中文；若中文质量不足可给英文权威资源。
                - 去重与规范：链接去重（按 URL）；移除跟踪参数（如 utm_* 等）；避免短链与重定向中间页；不要返回搜索结果页。
                
                输出格式（只允许输出以下 JSON，严禁输出任何解释、代码块标记或额外文本）：
                {
                  "links": [
                    { "url": "https://...", "title": "页面标题（非泛化词，如“主页/文章列表”等）", "source": "站点/机构名称（如 MDN、维基百科、清华大学、Oracle Docs）" },
                    ...
                  ]
                }
                要求：
                - 严格输出 JSON 对象，最外层键为 "links"（数组）。
                - links 数组长度必须与用户消息要求数量一致（例如 5 个）。
                - 每个元素包含三个字段且全部有效：url（HTTPS，直达内容页）、title（准确概括页面主题）、source（站点或机构名）。
                - 不得返回搜索结果页、站点主页、登录页、收费墙页、PDF 聚合页、跳转中间页、短链或明显广告页。
                - 不得返回用户消息中“禁用域名”列表内的任何链接。
                - 链接之间需主题互补、来源多样，避免同一站点的重复栏目页。
                
                质量自检清单（请在生成前自检并仅输出最终 JSON）：
                1) 每个 URL 为直达内容页（非列表/搜索/中间页），HTTPS 优先，去除无关跟踪参数。
                2) title 能准确概括落地页主题，非“首页/列表/下载-xx”等泛化词。
                3) source 为真实站点/机构名（如：MDN、Oracle Docs、Khan Academy、菜鸟教程、清华大学等）。
                4) links 数量与用户要求一致；URL 不重复；不包含被禁用域；来源尽量多样化。
                5) 优先官档/学术/权威教程；若为英文高质量资源，可保留，但不要全部英文。
                6) 与用户提供的关键词尽可能紧密（至少围绕 2 个关键词展开），避免泛化页面。
                
                只输出满足以上约束的 JSON，且仅输出一次，不要添加任何额外文本。
                """;
        return p;
    }

    private List<LinkCandidate> parseCandidates(String raw) {
        if (raw == null || raw.trim().isEmpty()) return Collections.emptyList();
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            JsonNode root = mapper.readTree(raw.trim());
            List<LinkCandidate> out = new ArrayList<>();
            if (root.isObject() && root.has("links") && root.get("links").isArray()) {
                for (JsonNode n : root.get("links")) {
                    LinkCandidate c = new LinkCandidate();
                    c.url = n.has("url") ? n.get("url").asText() : null;
                    c.title = n.has("title") ? n.get("title").asText() : null;
                    c.source = n.has("source") ? n.get("source").asText() : null;
                    if (c.url != null && c.url.startsWith("http")) out.add(c);
                }
            } else if (root.isArray()) {
                for (JsonNode n : root) {
                    LinkCandidate c = new LinkCandidate();
                    if (n.isTextual()) c.url = n.asText();
                    else if (n.isObject() && n.has("url")) {
                        c.url = n.get("url").asText();
                        c.title = n.has("title") ? n.get("title").asText() : null;
                        c.source = n.has("source") ? n.get("source").asText() : null;
                    }
                    if (c.url != null && c.url.startsWith("http")) out.add(c);
                }
            }
            LinkedHashMap<String, LinkCandidate> map = new LinkedHashMap<>();
            for (LinkCandidate c : out) map.putIfAbsent(c.url, c);
            return new ArrayList<>(map.values());
        } catch (Exception e) {
            log.warn("解析候选链接失败，将忽略本轮结果: {}", e.toString());
            return Collections.emptyList();
        }
    }

    // ================= 其他已有工具 =================

    private String safe(Object o) {
        return o == null ? "" : String.valueOf(o);
    }

    private String safePercent(Double p) {
        if (p == null) return "0";
        return String.format(Locale.ROOT, "%.2f", p * 100.0);
    }

    private int nz(Integer i) {
        return i == null ? 0 : i;
    }

    private String buildAnalysisPrompt(String knowledgePointData, String questionData) {
        return String.format("""
                你是一位专业的教育数据分析师和学习顾问。请基于以下学生的学习数据，生成一份专业、全面的学情分析报告。
                
                ### 🎯 分析任务
                请深入分析学生的学习状况，生成个性化的学情报告和学习建议。
                
                ### 📊 数据源
                **学生知识点掌握情况：**
                %s
                
                **学生错题记录：**
                %s
                
                ### 📋 分析要求
                请按以下结构生成分析报告（使用markdown格式）：
                
                #### 1. 📈 学习现状总览 (200-250字)
                - 基于知识点掌握数据，评估整体学习水平
                - 分析各课程学习进度和掌握程度分布
                - 概括主要学习特征和表现
                
                #### 2. 🧠 知识掌握深度分析 (300-350字)
                - **课程掌握对比**：各课程间的掌握水平差异
                - **核心知识点分析**：重点关注isCorePoint=true的知识点掌握情况
                - **难度分布**：easy/medium/hard各难度级别的掌握状况
                - **学习投入效果**：practiceCount与practiceScore的关系分析
                - **章节进度评估**：chapterProgressRate反映的学习进度
                
                #### 3. ❌ 错题深度剖析 (300-350字)
                - **错题类型分析**：single_choice/multiple_choice/true_false/fill_blank/short_answer的错误分布
                - **难度集中度**：错题在easy/medium/hard的分布特征
                - **课程薄弱点**：各courseName中的错题集中情况
                - **答题准确性**：scoreEarned与scorePoints的得分率分析
                - **错误模式**：高频错误类型和潜在原因
                
                #### 4. 🔍 学习行为洞察 (200-250字)
                - **练习频次**：practiceCount反映的学习勤奋度
                - **学习时长**：chapterStudyTime体现的时间投入
                - **学习路径**：基于chapterOrder和pointOrderInChapter的学习轨迹
                - **学习专注度**：各知识点投入时间与掌握效果的匹配度
                
                #### 5. ⚖️ 优势与挑战识别 (250-300字)
                **💪 学习优势：**
                - 至少3个具体优势（基于掌握程度高的知识点和错题少的领域）
                - 学习习惯中的积极表现
                
                **🔧 学习挑战：**
                - 至少3个需要改进的方面（基于未掌握的核心知识点和高频错题）
                - 学习效率和方法上的问题
                
                #### 6. 💡 个性化学习建议 (400-450字)
                **🎯 即刻行动（本周内）：**
                - 3-4个基于当前数据的紧急改进建议
                - 重点关注masteryLevel='not_learned'的核心知识点
                
                **📈 短期计划（2-4周）：**
                - 针对错题集中的questionType制定专项练习计划
                - 对practiceScore<60分的知识点进行重点突破
                
                **🚀 中期目标（1-2个月）：**
                - 基于课程整体掌握情况制定系统学习计划
                - 提升章节学习效率和知识点掌握深度
                
                **📚 学习方法建议：**
                - 根据错题类型提供针对性的解题策略
                - 基于学习时长和效果提供时间管理建议
                
                #### 7. ⚠️ 重点关注预警 (150-200字)
                - 需要优先解决的学习问题（基于核心知识点掌握不足）
                - 潜在的学习风险（基于错题趋势和学习投入度）
                - 家长和教师需要重点关注的方面
                
                ### 📝 输出要求
                1. 每个分析结论都要有具体的数据支撑
                2. 使用专业但易懂的教育语言
                3. 建议具体可操作，避免空泛表述
                4. 语调积极正面，注重鼓励和指导
                5. 关键数据和建议用**加粗**突出
                6. 使用emoji增加可读性
                7. 确保分析客观准确，建议切实可行
                
                请开始分析并生成完整的学情报告。
                """, knowledgePointData, questionData);
    }

    // ================== 小工具 ==================

    public static boolean canAccess(String urlStr, int timeout) {
        try {
            URL url = new URL(urlStr);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(timeout);
            connection.setReadTimeout(timeout);
            connection.setRequestMethod("HEAD");
            int responseCode = connection.getResponseCode();
            return (200 <= responseCode && responseCode < 400);
        } catch (Exception e) {
            return false;
        }
    }
}