package com.aiproject.smartcampus.service.impl;

import com.aiproject.smartcampus.commons.client.Result;
import com.aiproject.smartcampus.commons.redis.QuestionRedisCache;
import com.aiproject.smartcampus.commons.redis.RedisSort;
import com.aiproject.smartcampus.mapper.*;
import com.aiproject.smartcampus.model.functioncalling.toolutils.TowerCreateToolUtils;
import com.aiproject.smartcampus.pojo.bo.StudentWrongKnowledgeBO;
import com.aiproject.smartcampus.pojo.po.*;
import com.aiproject.smartcampus.service.TowerService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PreDestroy;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * @program: TeacherMaxAI
 * @description: 优化后的塔服务实现类（修复重复执行与日志可见性）
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TowerServiceImpl implements TowerService {

    private final TowerCreateToolUtils towerCreateToolUtils;
    private final TowerMapper towerMapper;
    private final CourseEnrollmentMapper courseEnrollmentMapper;
    private final TowerFloorMapper towerFloorMapper;
    private final RedisSort redisSort;
    private final GameUserMapper gameUserMapper;
    private final TaskMapper taskMapper;
    private final TaskServiceImpl taskServiceImpl;
    private final BossMapper bossMapper;
    private final KnowledgePointMapper knowledgePointMapper;
    private final QuestionRedisCache questionRedisCache;
    private final ChatLanguageModel chatLanguageModel;
    private final QuestionBankMapper questionBankMapper;
    private final CourseMapper courseMapper;
    private final StringRedisTemplate redisTemplate;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    // 线程池（保证非守护线程，防止任务被提前终止）
    private final ExecutorService createQuestionExecutor = Executors.newFixedThreadPool(
            5, r -> {
                Thread t = new Thread(r, "CreateQuestion-" + r.hashCode());
                t.setDaemon(false);
                return t;
            }
    );

    private final ExecutorService updateQuestionExecutor = Executors.newFixedThreadPool(
            10, r -> {
                Thread t = new Thread(r, "UpdateQuestion-" + r.hashCode());
                t.setDaemon(false);
                return t;
            }
    );

    // 常量
    private static final int INITIAL_QUESTION_COUNT = 5;
    private static final int MIN_QUESTION_COUNT = 3;
    private static final int UPDATE_QUESTION_COUNT = 2;
    private static final String QUESTION_TASK_KEY_PREFIX = "question_task:";

    // 最小处理间隔（秒）避免短时间重复执行
    private static final int PROCESS_MIN_INTERVAL_SEC = 5;

    // 任务队列（任务上下文）
    private final ConcurrentHashMap<String, QuestionTaskInfo> activeQuestionTasks = new ConcurrentHashMap<>();
    // 执行中的 key（并发防重的二次防线）
    private final ConcurrentHashMap<String, Boolean> processingKeys = new ConcurrentHashMap<>();

    static {
        initObjectMapper();
    }

    private static void initObjectMapper() {
        objectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        SimpleModule module = new SimpleModule();
        module.addDeserializer(QuestionBank.QuestionType.class, new JsonDeserializer<QuestionBank.QuestionType>() {
            @Override
            public QuestionBank.QuestionType deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
                String value = p.getText();
                return QuestionBank.QuestionType.fromValue(value);
            }
        });
        module.addDeserializer(QuestionBank.DifficultyLevel.class, new JsonDeserializer<QuestionBank.DifficultyLevel>() {
            @Override
            public QuestionBank.DifficultyLevel deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
                String value = p.getText();
                return QuestionBank.DifficultyLevel.fromValue(value);
            }
        });
        module.addDeserializer(BigDecimal.class, new JsonDeserializer<BigDecimal>() {
            @Override
            public BigDecimal deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
                String text = p.getText();
                try {
                    return new BigDecimal(text);
                } catch (Exception e) {
                    log.warn("BigDecimal解析失败，使用默认值0: {}", text);
                    return BigDecimal.ZERO;
                }
            }
        });

        objectMapper.registerModule(module);
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SortedUser {
        String studentId;
        String studentName;
        Double maxTowerFloorNo;
        Integer studentLevel;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    private static class QuestionTaskInfo {
        private String towerId;
        private String floorId;
        private String courseId;
        private String studentId;
        private LocalDateTime lastUpdateTime;
        private volatile boolean processing;
    }

    public enum Difficulty {
        EASY, MEDIUM, HARD
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Boolean> createTowerByAgent(String studentId, String courseId) {
        try {
            if (!isStudentEnrolledInCourse(studentId, courseId)) {
                return Result.error("学生未选择该课程");
            }
            Boolean result = towerCreateToolUtils.createTowerByStudentIdAndCourseId(studentId, courseId);
            if (!result) {
                log.error("创建塔失败 - studentId: {}, courseId: {}", studentId, courseId);
                return Result.error("创建个性化塔失败");
            }
            log.info("成功创建塔 - studentId: {}, courseId: {}", studentId, courseId);
            return Result.success(true);
        } catch (Exception e) {
            log.error("创建塔异常 - studentId: {}, courseId: {}", studentId, courseId, e);
            return Result.error("创建个性化塔失败");
        }
    }

    @Override
    public Result<List<Tower>> getTowerByStudentId(String studentId) {
        try {
            LambdaQueryWrapper<Tower> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Tower::getStudentId, Long.valueOf(studentId));
            List<Tower> towers = towerMapper.selectList(queryWrapper);
            return Result.success(Objects.requireNonNullElseGet(towers, ArrayList::new));
        } catch (Exception e) {
            log.error("获取学生塔列表失败 - studentId: {}", studentId, e);
            return Result.error("获取塔列表失败");
        }
    }

    @Override
    public Result<List<TowerFloor>> getTowerFloorByTowerId(String towerId) {
        try {
            LambdaQueryWrapper<TowerFloor> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(TowerFloor::getTowerId, Long.valueOf(towerId));
            List<TowerFloor> towerFloors = towerFloorMapper.selectList(queryWrapper);
            return Result.success(Objects.requireNonNullElseGet(towerFloors, ArrayList::new));
        } catch (Exception e) {
            log.error("获取塔层列表失败 - towerId: {}", towerId, e);
            return Result.error("获取塔层列表失败");
        }
    }

    @Override
    public Result<List<SortedUser>> getUserSortByOneTowerId(String courseId) {
        try {
            Set<ZSetOperations.TypedTuple<String>> sortedList = redisSort.getCourseLeaderboard(courseId);
            List<SortedUser> sortedUserList = toSortedUserList(sortedList);
            return Result.success(sortedUserList);
        } catch (Exception e) {
            log.error("获取课程排行榜失败 - courseId: {}", courseId, e);
            return Result.error("获取排行榜失败");
        }
    }

    @Override
    public Result<List<SortedUser>> getTotleSorted() {
        try {
            Set<ZSetOperations.TypedTuple<String>> totleSortedList = redisSort.getTotalLeaderboard();
            List<SortedUser> sortedUsers = toSortedUserList(totleSortedList);
            return Result.success(sortedUsers);
        } catch (Exception e) {
            log.error("获取总排行榜失败", e);
            return Result.error("获取排行榜失败");
        }
    }

    @Override
    public Result<String> getTowerStoryByTowerId(String towerId) {
        try {
            Tower tower = getTowerById(towerId);
            if (tower == null) return Result.error("塔不存在");
            String description = tower.getDescription();
            if (description == null || description.trim().isEmpty()) {
                return Result.error("故事情节为空");
            }
            return Result.success(description);
        } catch (Exception e) {
            log.error("获取塔故事失败 - towerId: {}", towerId, e);
            return Result.error("获取故事失败");
        }
    }

    @Override
    public Result<String> getTowerFloorStoryByTowerFloorId(String towerFloorId) {
        try {
            TowerFloor towerFloor = getTowerFloorById(towerFloorId);
            if (towerFloor == null) return Result.error("塔层不存在");
            String description = towerFloor.getDescription();
            if (description == null || description.trim().isEmpty()) {
                return Result.error("故事情节为空");
            }
            return Result.success(description);
        } catch (Exception e) {
            log.error("获取塔层故事失败 - towerFloorId: {}", towerFloorId, e);
            return Result.error("获取故事失败");
        }
    }

    @Override
    public Result<TowerFloor> getTowerFloorInfoByFloorId(String towerFloorId) {
        try {
            TowerFloor towerFloor = getTowerFloorById(towerFloorId);
            if (towerFloor == null) return Result.error("塔层不存在");
            return Result.success(towerFloor);
        } catch (Exception e) {
            log.error("获取塔层信息失败 - towerFloorId: {}", towerFloorId, e);
            return Result.error("获取塔层信息失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result setIsPass(String towerFloorId, Integer isPass) {
        try {
            LambdaUpdateWrapper<TowerFloor> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(TowerFloor::getFloorId, Long.valueOf(towerFloorId))
                    .set(TowerFloor::getIsPass, isPass);
            int update = towerFloorMapper.update(null, updateWrapper);
            if (update == 0) {
                log.warn("修改通过状态失败，可能塔层不存在 - towerFloorId: {}", towerFloorId);
                return Result.error("修改通过状态失败");
            }
            log.info("成功修改塔层通过状态 - towerFloorId: {}, isPass: {}", towerFloorId, isPass);
            return Result.success();
        } catch (Exception e) {
            log.error("修改通过状态异常 - towerFloorId: {}, isPass: {}", towerFloorId, isPass, e);
            return Result.error("修改通过状态失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result loadTest(String towerId, String floorId, String courseId, String studentId) {
        try {
            log.info("开始初始化测试 - towerId: {}, floorId: {}, courseId: {}, studentId: {}",
                    towerId, floorId, courseId, studentId);

            // 注册任务（就地更新，不替换对象；记录时间，避免短时间重复执行）
            registerQuestionTask(towerId, floorId, courseId, studentId);

            Task task = getTaskByFloorId(floorId);
            if (task == null) return Result.error("塔层任务不存在");

            List<Integer> pointsList = taskServiceImpl.parsePoints(task.getPointIds());
            List<StudentWrongKnowledgeBO> relevantWrongKnowledge = getRelevantWrongKnowledge(studentId, pointsList);

            Monster monster = getMonsterByFloorId(floorId);
            if (monster == null) return Result.error("Boss不存在");

            int questionCountByBossHp = getQuestionCountByBossHp(
                    monster.getHp() == null ? 0 : monster.getHp(), 100);

            String courseName = courseMapper.findCourseNameByid(courseId);
            Tower tower = getTowerById(towerId);
            TowerFloor towerFloor = getTowerFloorById(floorId);
            if (tower == null || towerFloor == null) return Result.error("塔或塔层信息不存在");

            Map<Difficulty, Double> weightsForFloor =
                    getWeightsForFloor(towerFloor.getFloorNo(), tower.getTotalFloors());

            List<Difficulty> allDifficulties = randomDifficultyListWithLimits(
                    towerFloor.getFloorNo(), tower.getTotalFloors(),
                    questionCountByBossHp, weightsForFloor, 4, 2, 1.8);

            List<Difficulty> initialDifficulties = allDifficulties.subList(0,
                    Math.min(INITIAL_QUESTION_COUNT, allDifficulties.size()));

            // 同时写入“总难度列表”（便于查看）和“消费队列”（用于弹出）
            cacheDifficultyList(floorId, allDifficulties);

            // 初始化Redis缓存
            initializeQuestionCache(floorId);

            // 异步生成初始题目
            generateQuestionsAsync(courseId, floorId, initialDifficulties,
                    relevantWrongKnowledge, courseName, createQuestionExecutor);

            log.info("成功初始化测试 - floorId: {}", floorId);
            return Result.success();

        } catch (Exception e) {
            log.error("初始化测试失败 - towerId: {}, floorId: {}, courseId: {}, studentId: {}",
                    towerId, floorId, courseId, studentId, e);
            return Result.error("初始化测试失败");
        }
    }

    /**
     * 定时批处理所有任务（带并发防重与限流）
     */
    @Scheduled(fixedDelay = 10000)
    public void scheduledCheckAndUpdateQuestions() {
        if (activeQuestionTasks.isEmpty()) {
            return;
        }

        List<Map.Entry<String, QuestionTaskInfo>> entries = new ArrayList<>(activeQuestionTasks.entrySet());
        log.info("定时任务触发，本轮检查 {} 个活跃任务", entries.size());

        List<CompletableFuture<Void>> futures = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (Map.Entry<String, QuestionTaskInfo> entry : entries) {
            String key = entry.getKey();
            QuestionTaskInfo task = entry.getValue();

            // 限流：最小处理间隔内跳过
            if (task.getLastUpdateTime() != null &&
                    java.time.Duration.between(task.getLastUpdateTime(), now).getSeconds() < PROCESS_MIN_INTERVAL_SEC) {
                log.info("跳过任务(限流) - key={}, lastUpdateTime={}", key, task.getLastUpdateTime());
                continue;
            }

            // 并发防重：同 key 正在执行则跳过
            if (Boolean.TRUE.equals(processingKeys.putIfAbsent(key, true))) {
                log.info("跳过任务(正在执行) - key={}", key);
                continue;
            }

            CompletableFuture<Void> f = CompletableFuture.runAsync(() -> {
                try {
                    task.setProcessing(true);
                    log.info("开始处理任务 - key={}, floorId={}, studentId={}",
                            key, task.getFloorId(), task.getStudentId());
                    checkAndUpdateQuestion(task.getTowerId(), task.getFloorId(), task.getCourseId(), task.getStudentId());
                    task.setLastUpdateTime(LocalDateTime.now());
                    log.info("完成处理任务 - key={}", key);
                } catch (Exception e) {
                    log.error("处理任务异常 - key=" + key, e);
                } finally {
                    task.setProcessing(false);
                    processingKeys.remove(key);
                }
            }, updateQuestionExecutor);

            futures.add(f);
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .orTimeout(30, TimeUnit.SECONDS)
                .exceptionally(ex -> {
                    log.warn("部分定时任务执行超时", ex);
                    return null;
                });
    }

    /**
     * 主业务逻辑：检查缓存题目数量，不足则补题
     */
    private void checkAndUpdateQuestion(String towerId, String floorId, String courseId, String studentId) {
        try {
            if (towerId == null || floorId == null || courseId == null || studentId == null) {
                log.info("缺少必要参数，跳过任务执行");
                return;
            }

            String createTag = questionRedisCache.isCreateTagInCache(Integer.valueOf(floorId));
            if (!"1".equals(createTag)) {
                log.info("塔层【{}】未初始化题库，跳过更新", floorId);
                return;
            }

            int currentQuestionCount = getCurrentQuestionCount(floorId);
            if (currentQuestionCount > MIN_QUESTION_COUNT) {
                log.info("塔【{}】塔层【{}】题目充足({})，无需更新", towerId, floorId, currentQuestionCount);
                return;
            }

            log.info("开始更新塔层【{}】题目，当前数量: {}", floorId, currentQuestionCount);

            List<Difficulty> nextDifficulties = getNextDifficulties(floorId, UPDATE_QUESTION_COUNT);
            if (nextDifficulties.isEmpty()) {
                log.warn("无法获取难度列表，跳过更新 - floorId: {}", floorId);
                return;
            }

            Task task = getTaskByFloorId(floorId);
            if (task == null) {
                log.warn("未找到塔层任务数据 - floorId: {}", floorId);
                return;
            }

            List<Integer> pointsList = taskServiceImpl.parsePoints(task.getPointIds());
            List<StudentWrongKnowledgeBO> relevantWrongKnowledge = getRelevantWrongKnowledge(studentId, pointsList);
            String courseName = Optional.ofNullable(courseMapper.findCourseNameByid(courseId)).orElse("未知课程");

            generateQuestionsAsync(courseId, floorId, nextDifficulties,
                    relevantWrongKnowledge, courseName, updateQuestionExecutor);

        } catch (Exception e) {
            log.error("检查更新题目异常 - towerId: {}, floorId: {}", towerId, floorId, e);
        }
    }

    /**
     * 注册或更新任务（不替换对象，保留 processing 状态）
     */
    private void registerQuestionTask(String towerId, String floorId, String courseId, String studentId) {
        String taskKey = generateTaskKey(floorId, studentId);
        LocalDateTime now = LocalDateTime.now();

        activeQuestionTasks.compute(taskKey, (k, old) -> {
            if (old == null) {
                log.info("注册题目任务 - 新增 key={}", taskKey);
                return new QuestionTaskInfo(towerId, floorId, courseId, studentId, now, false);
            } else {
                old.setTowerId(towerId);
                old.setFloorId(floorId);
                old.setCourseId(courseId);
                old.setStudentId(studentId);
                old.setLastUpdateTime(now);
                log.info("注册题目任务 - 更新 key={}", taskKey);
                return old;
            }
        });
    }

    private CompletableFuture<List<Integer>> generateQuestionsAsync(
            String courseId, String floorId, List<Difficulty> difficulties,
            List<StudentWrongKnowledgeBO> wrongKnowledge, String courseName, Executor executor) {

        return CompletableFuture
                .supplyAsync(() -> {
                    try {
                        return generateQuestionsWithAI(courseId, difficulties, wrongKnowledge, courseName);
                    } catch (Exception e) {
                        log.error("AI生成题目失败", e);
                        throw new RuntimeException("AI生成题目失败", e);
                    }
                }, executor)
                .thenCompose(questionIds ->
                        CompletableFuture.supplyAsync(() -> {
                            try {
                                updateQuestionCache(floorId, questionIds);
                                markAsInitialized(floorId);
                                return questionIds;
                            } catch (Exception e) {
                                log.error("更新缓存失败", e);
                                throw new RuntimeException("更新缓存失败", e);
                            }
                        }, executor)
                )
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("生成题目异步任务异常 - floorId: {}", floorId, ex);
                    } else {
                        log.info("成功生成题目 - floorId: {}, count: {}", floorId, result.size());
                    }
                });
    }

    private List<Integer> generateQuestionsWithAI(String courseId, List<Difficulty> difficulties,
                                                  List<StudentWrongKnowledgeBO> wrongKnowledge, String courseName) throws Exception {

        Map<String, Double> pointAccessRateMap = wrongKnowledge.stream()
                .collect(Collectors.toMap(
                        StudentWrongKnowledgeBO::getPointName,
                        StudentWrongKnowledgeBO::getAccuracyRate,
                        (existing, replacement) -> existing));

        Map<String, Integer> pointIdMap = wrongKnowledge.stream()
                .collect(Collectors.toMap(
                        StudentWrongKnowledgeBO::getPointName,
                        StudentWrongKnowledgeBO::getPointId,
                        (existing, replacement) -> existing));

        String prompt = createQuestionPrompts(difficulties, pointAccessRateMap,
                pointIdMap, difficulties.size(), courseName);

        log.info("调用AI生成题目 - 难度数量: {}", difficulties.size());
        var resp = chatLanguageModel.chat(UserMessage.userMessage(prompt));
        String aiResponse = (resp == null || resp.aiMessage() == null) ? null : resp.aiMessage().text();
        if (aiResponse == null || aiResponse.trim().isEmpty()) {
            throw new IOException("AI 返回空响应");
        }
        log.debug("AI返回题目JSON: {}", aiResponse);

        List<QuestionBank> questionBanks = parseQuestionBanks(aiResponse);

        LocalDateTime now = LocalDateTime.now();
        questionBanks.forEach(questionBank -> {
            questionBank.setCourseId(Integer.valueOf(courseId));
            questionBank.setCreatedBy(0);
            questionBank.setCreatedAt(now);
            questionBank.setUpdatedAt(now);
        });

        questionBankMapper.insert(questionBanks);
        log.info("成功插入{}道题目到数据库", questionBanks.size());

        return questionBanks.stream()
                .map(QuestionBank::getQuestionId)
                .collect(Collectors.toList());
    }

    private boolean isStudentEnrolledInCourse(String studentId, String courseId) {
        LambdaQueryWrapper<CourseEnrollment> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CourseEnrollment::getStudentId, Long.valueOf(studentId))
                .eq(CourseEnrollment::getCourseId, Long.valueOf(courseId));
        return courseEnrollmentMapper.selectOne(queryWrapper) != null;
    }

    private Tower getTowerById(String towerId) {
        LambdaQueryWrapper<Tower> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Tower::getTowerId, Long.valueOf(towerId));
        return towerMapper.selectOne(queryWrapper);
    }

    private TowerFloor getTowerFloorById(String floorId) {
        LambdaQueryWrapper<TowerFloor> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TowerFloor::getFloorId, Long.valueOf(floorId));
        return towerFloorMapper.selectOne(queryWrapper);
    }

    private Task getTaskByFloorId(String floorId) {
        LambdaQueryWrapper<Task> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Task::getFloorId, Long.valueOf(floorId));
        return taskMapper.selectOne(queryWrapper);
    }

    private Monster getMonsterByFloorId(String floorId) {
        LambdaQueryWrapper<Monster> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Monster::getFloorId, Long.valueOf(floorId));
        return bossMapper.selectOne(queryWrapper);
    }

    private List<StudentWrongKnowledgeBO> getRelevantWrongKnowledge(String studentId, List<Integer> pointsList) {
        List<StudentWrongKnowledgeBO> allWrongKnowledge = knowledgePointMapper.getStudentWrongKnowledgeByStudentId(studentId);
        if (allWrongKnowledge == null || allWrongKnowledge.isEmpty()) {
            log.info("学生【{}】无错误知识点", studentId);
            return new ArrayList<>();
        }
        return allWrongKnowledge.stream()
                .distinct()
                .filter(knowledge -> pointsList.contains(knowledge.getPointId()))
                .collect(Collectors.toList());
    }

    private void removeQuestionTask(String floorId, String studentId) {
        String taskKey = generateTaskKey(floorId, studentId);
        activeQuestionTasks.remove(taskKey);
        processingKeys.remove(taskKey);
        log.info("注销题目任务 - key: {}", taskKey);
    }

    private String generateTaskKey(String floorId, String studentId) {
        return QUESTION_TASK_KEY_PREFIX + floorId + ":" + studentId;
    }

    private void cacheDifficultyList(String floorId, List<Difficulty> difficulties) {
        List<String> difficultyStrings = difficulties.stream()
                .map(difficulty -> switch (difficulty) {
                    case EASY -> "0";
                    case MEDIUM -> "1";
                    case HARD -> "2";
                })
                .collect(Collectors.toList());

        Integer fid = Integer.valueOf(floorId);
        // 写入消费队列（用于弹出）
        questionRedisCache.setQuestionDifficultyListInCache(fid, difficultyStrings);
        // 写入总列表（用于查看/调试）
        questionRedisCache.setTotalQuestionDifficultyInCache(fid, difficultyStrings);

        log.info("缓存塔层【{}】难度分布: {}", floorId, difficultyStrings);
    }

    private void initializeQuestionCache(String floorId) {
        questionRedisCache.setQuestionNumToCache(Integer.valueOf(floorId), 0);
        log.info("初始化塔层【{}】题目缓存", floorId);
    }

    private int getCurrentQuestionCount(String floorId) {
        try {
            String countStr = questionRedisCache.getQuestionNumInCache(Integer.valueOf(floorId));
            return Integer.parseInt(Optional.ofNullable(countStr).orElse("0"));
        } catch (NumberFormatException e) {
            log.warn("解析题目数量失败 - floorId: {}", floorId, e);
            return 0;
        }
    }

    private List<Difficulty> getNextDifficulties(String floorId, int count) {
        try {
            // 从消费队列批量弹出
            List<String> difficultyStrings = questionRedisCache.popDifficultyListInCache(Integer.valueOf(floorId), count);
            return difficultyStrings.stream()
                    .map(str -> switch (str) {
                        case "0" -> Difficulty.EASY;
                        case "1" -> Difficulty.MEDIUM;
                        case "2" -> Difficulty.HARD;
                        default -> {
                            log.warn("未知难度值: {}", str);
                            yield Difficulty.MEDIUM;
                        }
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("获取难度列表失败 - floorId: {}", floorId, e);
            return new ArrayList<>();
        }
    }

    private void updateQuestionCache(String floorId, List<Integer> questionIds) {
        // 增加题目计数 + 写入题目ID队列
        questionRedisCache.incrQuestionNumInCache(Integer.valueOf(floorId), questionIds.size());
        questionRedisCache.setQuestionToCache(Integer.valueOf(floorId), questionIds);
        log.info("更新塔层【{}】题目缓存，新增{}道题目", floorId, questionIds.size());
    }

    private void markAsInitialized(String floorId) {
        questionRedisCache.setCreateTagToCache(Integer.valueOf(floorId), "1");
        log.info("标记塔层【{}】为已初始化", floorId);
    }

    public static List<QuestionBank> parseQuestionBanks(String json) throws IOException {
        try {
            return objectMapper.readValue(json, new TypeReference<List<QuestionBank>>() {
            });
        } catch (IOException e) {
            log.error("解析题目JSON失败: {}", json, e);
            throw e;
        }
    }

    public String createQuestionPrompts(List<Difficulty> difficulties,
                                        Map<String, Double> pointAccessRateMap,
                                        Map<String, Integer> pointIdMap,
                                        Integer questionCount,
                                        String courseName) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是一名专业的出题专家，请根据以下信息为学生生成个性化定制化练习题。\n");
        prompt.append("请严格按照以下字段和格式生成题目，方便程序直接解析。\n\n");
        prompt.append("总共需要生成 ").append(questionCount).append(" 道题目。\n");
        prompt.append("课程名称：").append(courseName).append("\n\n");
        Map<Difficulty, Long> diffCountMap = difficulties.stream()
                .collect(Collectors.groupingBy(d -> d, Collectors.counting()));
        prompt.append("题目难度分布如下：\n");
        for (Difficulty diff : Difficulty.values()) {
            long count = diffCountMap.getOrDefault(diff, 0L);
            if (count > 0) {
                prompt.append("- ").append(diff.name()).append(" 难度：").append(count).append(" 道\n");
            }
        }
        prompt.append("\n");
        prompt.append("知识点掌握情况（通过率低的优先出题，并结合场景个性化出题）：\n");
        pointAccessRateMap.entrySet().stream()
                .sorted(Map.Entry.comparingByValue())
                .forEach(e -> {
                    Integer pointId = pointIdMap.get(e.getKey());
                    prompt.append("- 知识点：").append(e.getKey())
                            .append("（point_id: ").append(pointId).append("）")
                            .append("，通过率：").append(String.format("%.2f%%", e.getValue() * 100))
                            .append("\n");
                });
        prompt.append("\n");
        prompt.append("请结合知识点设计真实应用情境，使题目贴近实际生活或职业场景。\n\n");
        prompt.append("生成规则：\n");
        prompt.append("1. 题目必须紧密结合上述知识点内容，并根据通过率个性化设计情境和题干。\n");
        prompt.append("2. \"point_id\" 字段必须严格使用上方对应的整数 ID。\n");
        prompt.append("3. 难度分布必须严格遵守上述统计。\n");
        prompt.append("4. 选择题选项必须使用如下 JSON 数组格式，且至少一个选项 is_correct 为 true：\n");
        prompt.append("[\n");
        prompt.append("  {\"label\": \"A\", \"content\": \"选项内容\", \"is_correct\": false},\n");
        prompt.append("  {\"label\": \"B\", \"content\": \"选项内容\", \"is_correct\": true},\n");
        prompt.append("  {\"label\": \"C\", \"content\": \"选项内容\", \"is_correct\": false}\n");
        prompt.append("]\n");
        prompt.append("5. 填空题和简答题的选项字段应设置为 null。\n");
        prompt.append("6. 正确答案字段必须是 JSON 格式，格式应与题型相符。\n");
        prompt.append("7. 所有 JSON 格式必须有效，不能包含注释、尾随逗号或多余内容。\n\n");
        prompt.append("请严格生成 ").append(questionCount).append(" 道题目，整体以纯 JSON 数组格式输出，示例如下：\n");
        prompt.append("[\n");
        prompt.append("  {\n");
        prompt.append("    \"question_id\": null,\n");
        prompt.append("    \"course_id\": -1,\n");
        prompt.append("    \"point_id\": 1,\n");
        prompt.append("    \"question_type\": \"single_choice\",\n");
        prompt.append("    \"question_content\": \"这里是题目描述\",\n");
        prompt.append("    \"question_options\": \"[{\\\"label\\\": \\\"A\\\", \\\"content\\\": \\\"选项1\\\", \\\"is_correct\\\": false}, {\\\"label\\\": \\\"B\\\", \\\"content\\\": \\\"选项2\\\", \\\"is_correct\\\": true}]\",\n");
        prompt.append("    \"correct_answer\": \"[\\\"B\\\"]\",\n");
        prompt.append("    \"explanation\": \"答案解析\",\n");
        prompt.append("    \"difficulty_level\": \"easy\",\n");
        prompt.append("    \"score_points\": 1.0\n");
        prompt.append("  }\n");
        prompt.append("]\n");
        prompt.append("请严格保证题目内容与题目类型(question_type)的对应\n");
        prompt.append("请只输出 JSON 数组，切勿输出任何解释性文字、注释或多余内容。");
        return prompt.toString();
    }

    public List<SortedUser> toSortedUserList(Set<ZSetOperations.TypedTuple<String>> sortedList) {
        if (sortedList == null || sortedList.isEmpty()) {
            return new ArrayList<>();
        }
        List<SortedUser> sortedUsers = sortedList.stream()
                .map(tuple -> {
                    SortedUser sortedUser = new SortedUser();
                    sortedUser.setMaxTowerFloorNo(tuple.getScore());
                    sortedUser.setStudentId(tuple.getValue());
                    return sortedUser;
                })
                .collect(Collectors.toList());

        List<String> studentIds = sortedUsers.stream().map(SortedUser::getStudentId).distinct().collect(Collectors.toList());
        if (studentIds.isEmpty()) return sortedUsers;

        try {
            LambdaQueryWrapper<GameUser> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.in(GameUser::getStudentId, studentIds);
            List<GameUser> gameUsers = gameUserMapper.selectList(queryWrapper);

            Map<String, GameUser> gameUserMap = gameUsers.stream()
                    .collect(Collectors.toMap(
                            g -> String.valueOf(g.getStudentId()),
                            g -> g,
                            (existing, replacement) -> existing));

            for (SortedUser su : sortedUsers) {
                GameUser gu = gameUserMap.get(su.getStudentId());
                if (gu != null) {
                    su.setStudentName(gu.getGameName());
                    su.setStudentLevel(gu.getLevel());
                } else {
                    log.warn("用户不存在，studentId={}", su.getStudentId());
                    su.setStudentName("未知用户");
                    su.setStudentLevel(0);
                }
            }
        } catch (Exception e) {
            log.error("查询用户信息失败", e);
        }

        return sortedUsers;
    }

    public static Map<Difficulty, Double> getWeightsForFloor(int floor, int totalFloors) {
        double maxEasy = 0.6, minEasy = 0.1;
        double maxHard = 0.7, minHard = 0.1;

        double progress = (double) (floor - 1) / Math.max(1, totalFloors - 1);
        progress = Math.pow(progress, 1.5);

        double easy = maxEasy - (maxEasy - minEasy) * progress;
        double hard = minHard + (maxHard - minHard) * progress;
        double medium = Math.max(0, 1.0 - easy - hard);

        Map<Difficulty, Double> weights = new EnumMap<>(Difficulty.class);
        weights.put(Difficulty.EASY, Math.max(0.05, easy));
        weights.put(Difficulty.MEDIUM, Math.max(0.05, medium));
        weights.put(Difficulty.HARD, Math.max(0.05, hard));

        double total = weights.values().stream().mapToDouble(Double::doubleValue).sum();
        weights.replaceAll((k, v) -> v / total);

        return weights;
    }

    public static int getQuestionCountByBossHp(int bossHp, int maxHp) {
        int minQuestions = 3;
        int maxQuestions = 20;
        if (maxHp <= 0) return minQuestions;
        double ratio = Math.min(1.0, (double) Math.max(0, bossHp) / maxHp);
        return (int) Math.round(minQuestions + ratio * (maxQuestions - minQuestions));
    }

    public static List<Difficulty> randomDifficultyListWithLimits(
            int floor, int totalFloors, int questionCount, Map<Difficulty, Double> weightMap,
            int baseMaxEasy, int baseMaxHard, double curveFactor) {

        if (questionCount <= 0) return new ArrayList<>();

        List<Difficulty> result = new ArrayList<>();
        Random random = new Random();

        double progress = (double) (floor - 1) / Math.max(1, totalFloors - 1);
        double curvedProgress = Math.pow(progress, curveFactor);

        int maxEasyCount = Math.max(1, (int) Math.round(baseMaxEasy * (1.0 - curvedProgress)));
        int maxHardCount = Math.min(questionCount - 1, (int) Math.round(baseMaxHard * (1.0 + curvedProgress)));

        int easyCount = 0, hardCount = 0;

        for (int i = 0; i < questionCount; i++) {
            Difficulty chosen = null;

            for (int tryCount = 0; tryCount < 10; tryCount++) {
                double r = random.nextDouble();
                double cumulative = 0.0;
                for (Map.Entry<Difficulty, Double> entry : weightMap.entrySet()) {
                    cumulative += entry.getValue();
                    if (r <= cumulative) {
                        chosen = entry.getKey();
                        break;
                    }
                }
                if (chosen == Difficulty.EASY && easyCount >= maxEasyCount) continue;
                if (chosen == Difficulty.HARD && hardCount >= maxHardCount) continue;
                break;
            }

            if (chosen == null ||
                    (chosen == Difficulty.EASY && easyCount >= maxEasyCount) ||
                    (chosen == Difficulty.HARD && hardCount >= maxHardCount)) {
                chosen = Difficulty.MEDIUM;
            }

            if (chosen == Difficulty.EASY) easyCount++;
            else if (chosen == Difficulty.HARD) hardCount++;

            result.add(chosen);
        }

        return result;
    }

    @Scheduled(fixedRate = 300000)
    public void cleanupExpiredTasks() {
        LocalDateTime expireTime = LocalDateTime.now().minusHours(1);
        activeQuestionTasks.entrySet().removeIf(entry -> {
            QuestionTaskInfo task = entry.getValue();
            if (task.getLastUpdateTime().isBefore(expireTime)) {
                log.info("清理过期任务 - key: {}", entry.getKey());
                processingKeys.remove(entry.getKey());
                return true;
            }
            return false;
        });
    }

    @PreDestroy
    public void destroy() {
        log.info("开始清理TowerService资源...");
        shutdownExecutor(createQuestionExecutor, "CreateQuestion");
        shutdownExecutor(updateQuestionExecutor, "UpdateQuestion");
        activeQuestionTasks.clear();
        processingKeys.clear();
        log.info("TowerService资源清理完成");
    }

    private void shutdownExecutor(ExecutorService executor, String name) {
        if (executor != null && !executor.isShutdown()) {
            try {
                executor.shutdown();
                if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                    log.warn("{}线程池未能在10秒内正常关闭，强制关闭", name);
                    executor.shutdownNow();
                    if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                        log.error("{}线程池强制关闭失败", name);
                    }
                }
                log.info("{}线程池已关闭", name);
            } catch (InterruptedException e) {
                log.warn("等待{}线程池关闭时被中断", name);
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    public Result<Boolean> unregisterQuestionTask(String floorId, String studentId) {
        try {
            removeQuestionTask(floorId, studentId);
            return Result.success(true);
        } catch (Exception e) {
            log.error("注销任务失败 - floorId: {}, studentId: {}", floorId, studentId, e);
            return Result.error("注销任务失败");
        }
    }

    public Result<Integer> getActiveTaskCount() {
        return Result.success(activeQuestionTasks.size());
    }
}