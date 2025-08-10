package com.aiproject.smartcampus.service.impl;

import com.aiproject.smartcampus.commons.client.Result;
import com.aiproject.smartcampus.commons.redis.QuestionRedisCache;
import com.aiproject.smartcampus.commons.redis.RedisSort;
import com.aiproject.smartcampus.commons.utils.UserToTypeUtils;
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
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * @program: TeacherMaxAI
 * @description:
 * @author: lk_hhh
 * @create: 2025-08-07 16:54
 **/

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

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final ExecutorService CREATE_QUEXTION_EXECUTOR = Executors.newFixedThreadPool(
            5, r -> {
                Thread t = new Thread(r);
                t.setDaemon(false); // 非守护线程
                return t;
            }
    );
    private final ExecutorService UPDATE_QUEXTION_EXECUTOR = Executors.newFixedThreadPool(
            10, r -> {
                Thread t = new Thread(r);
                t.setDaemon(false); // 非守护线程
                return t;
            }
    );

    static {
        // 设置属性命名策略，支持snake_case转camelCase映射
        objectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);

        // 忽略未知字段，避免JSON中多余字段导致失败
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        // 注册自定义枚举反序列化，调用fromValue方法
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

        // 注册BigDecimal反序列化（可选，Jackson默认支持）
        module.addDeserializer(BigDecimal.class, new JsonDeserializer<BigDecimal>() {
            @Override
            public BigDecimal deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
                String text = p.getText();
                try {
                    return new BigDecimal(text);
                } catch (Exception e) {
                    return BigDecimal.ZERO; // 解析失败返回0或自定义逻辑
                }
            }
        });

        objectMapper.registerModule(module);
    }

    private final CourseMapper courseMapper;


    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SortedUser {

        String studentId;
        String studentName;
        Double maxTowerFloorNo;
        Integer studentLevel;

    }

    /**
     * 抽取知识点难度算法（根据boss血量来进行生成）
     */
    public static enum Difficulty {
        EASY, MEDIUM, HARD

    }


    @Override
    public Result<Boolean> createTowerByAgent(String studentId, String courseId) {

        //首先判断该学生是否选了这门课
        LambdaQueryWrapper<CourseEnrollment> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CourseEnrollment::getStudentId, studentId);
        queryWrapper.eq(CourseEnrollment::getCourseId, courseId);
        CourseEnrollment courseEnrollment = courseEnrollmentMapper.selectOne(queryWrapper);
        if (courseEnrollment == null) {
            return Result.error("学生未选择该课程");
        }

        //智能生成
        Boolean towerByStudentIdAndCourseId = towerCreateToolUtils.createTowerByStudentIdAndCourseId(studentId, courseId);

        if (!towerByStudentIdAndCourseId) {
            log.error("创建塔失败");
            return Result.error("创建个性化塔失败");
        }

        return Result.success();
    }

    @Override
    public Result<List<Tower>> getTowerByStudentId(String studentId) {

        LambdaQueryWrapper<Tower> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Tower::getStudentId, studentId);
        List<Tower> towers = towerMapper.selectList(queryWrapper);

        return Result.success(Objects.requireNonNullElseGet(towers, ArrayList::new));

    }

    @Override
    public Result<List<TowerFloor>> getTowerFloorByTowerId(String towerId) {

        LambdaQueryWrapper<TowerFloor> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TowerFloor::getTowerId, towerId);
        List<TowerFloor> towerFloors = towerFloorMapper.selectList(queryWrapper);

        return Result.success(Objects.requireNonNullElseGet(towerFloors, ArrayList::new));
    }

    @Override
    public Result<List<SortedUser>> getUserSortByOneTowerId(String courseId) {

        Set<ZSetOperations.TypedTuple<String>> sortedList = redisSort.getSortedList(courseId);

        List<SortedUser> sortedUserList = toSortedUserList(sortedList);

        return Result.success(sortedUserList);
    }

    @Override
    public Result<List<SortedUser>> getTotleSorted() {

        Set<ZSetOperations.TypedTuple<String>> totleSortedList = redisSort.getTotleSortedList();

        List<SortedUser> sortedUsers = toSortedUserList(totleSortedList);

        return Result.success(sortedUsers);

    }

    @Override
    public Result<String> getTowerStoryByTowerId(String towerId) {

        LambdaQueryWrapper<Tower> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Tower::getTowerId, towerId);
        Tower tower = towerMapper.selectOne(queryWrapper);
        if (tower == null) {
            log.error("塔层不存在");
            return Result.error("塔层不存在");
        }

        String description = tower.getDescription();

        if (description == null) {
            log.error("故事情节为空");
            return Result.error("故事情节为空");
        }

        return Result.success(description);
    }

    @Override
    public Result<String> getTowerFloorStoryByTowerFloorId(String towerFloorId) {

        LambdaQueryWrapper<TowerFloor> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TowerFloor::getFloorId, towerFloorId);
        TowerFloor towerFloor = towerFloorMapper.selectOne(queryWrapper);
        if (towerFloor == null) {
            log.error("该层不存在");
            return Result.error("塔层不存在");
        }

        String description = towerFloor.getDescription();

        if (description == null) {
            log.error("故事情节为空");
            return Result.error("故事情节为空");
        }

        return Result.success(description);
    }

    @Override
    public Result<TowerFloor> getTowerFloorInfoByFloorId(String towerFloorId) {

        LambdaQueryWrapper<TowerFloor> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TowerFloor::getFloorId, towerFloorId);
        TowerFloor towerFloor = towerFloorMapper.selectOne(queryWrapper);
        if (towerFloor == null) {
            log.error("塔层为空");
            return Result.error("塔层为空");
        }

        return Result.success(towerFloor);
    }

    @Override
    public Result setIsPass(String towerFloorId, Integer isPass) {

        LambdaUpdateWrapper<TowerFloor> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(TowerFloor::getTowerId, towerFloorId);
        updateWrapper.set(TowerFloor::getIsPass, isPass);
        int update = towerFloorMapper.update(updateWrapper);
        if (update == 0) {
            log.error("修改通过状态失败");
            return Result.error("修改通过状态失败");
        }

        return Result.success();
    }

    /**
     * 初始化生成3道题目
     */
    @Override
    public Result loadTest(String towerId, String floorId, String courseId, String studentId) {

        //设置初始化题目数量
        Integer LIMIT_QUESTION_NO = 3;

        LambdaQueryWrapper<Task> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Task::getFloorId, floorId);
        Task task = taskMapper.selectOne(queryWrapper);
        List<Integer> pointsList = taskServiceImpl.parsePoints(task.getPointIds());

        List<StudentWrongKnowledgeBO> studentWrongKnowledgeByStudentId = knowledgePointMapper.getStudentWrongKnowledgeByStudentId(studentId);
        List<StudentWrongKnowledgeBO> pointList = studentWrongKnowledgeByStudentId.stream().distinct().filter(a -> {
            for (Integer pointId : pointsList) {
                if (a.getPointId().equals(pointId)) {
                    return true;
                }
            }
            return false;
        }).toList();

        //获取当前层数的boss的血量并进行动态生成题目数量
        LambdaQueryWrapper<Monster> monsterQueryWrapper = new LambdaQueryWrapper<>();
        monsterQueryWrapper.eq(Monster::getFloorId, floorId);
        Monster monster = bossMapper.selectOne(monsterQueryWrapper);

        if (monster == null) {
            log.error("boss不存在");
            return Result.error("boss不存在");
        }

        //根据boss血量获取需要生成的题目数量（计算每层难度的分配）
        int questionCountByBossHp = getQuestionCountByBossHp(monster.getHp() == null ? 0 : monster.getHp(), 100);

        String courseNameByid = courseMapper.findCourseNameByid(courseId);
        LambdaQueryWrapper<Tower> towerQueryWrapper = new LambdaQueryWrapper<>();
        towerQueryWrapper.eq(Tower::getTowerId, towerId);
        Tower tower = towerMapper.selectOne(towerQueryWrapper);
        Integer totalFloors = tower.getTotalFloors();

        LambdaQueryWrapper<TowerFloor> towerFloorQueryWrapper = new LambdaQueryWrapper<>();
        towerFloorQueryWrapper.eq(TowerFloor::getFloorId, floorId);
        TowerFloor towerFloor = towerFloorMapper.selectOne(towerFloorQueryWrapper);
        Integer floorNo = towerFloor.getFloorNo();

        //获取该层的难度分配
        Map<Difficulty, Double> weightsForFloor = getWeightsForFloor(floorNo, totalFloors);

        //生成当前层的难度列表（设置最大简单题数量，最大难题数量）后续需要持久化存储
        List<Difficulty> difficulties = randomDifficultyListWithLimits(floorNo, totalFloors, questionCountByBossHp, weightsForFloor, 4, 2, 1.8)
                .stream().limit(LIMIT_QUESTION_NO).collect(Collectors.toList());

        //持久化
        List<String> questionDifficultyList = difficulties.stream().map(a -> switch (a) {
            case Difficulty.HARD -> "2";
            case Difficulty.MEDIUM -> "1";
            default -> "0";

        }).toList();

        //构建出知识点通过率映射map，知识点id映射map，以及创建题目提示词（从异步中提取出来加快异步非守护线程的执行效率）
        Map<String, Double> pointAccessRateMap = pointList.stream()
                .collect(Collectors.toMap(StudentWrongKnowledgeBO::getPointName, StudentWrongKnowledgeBO::getAccuracyRate));
        Map<String, Integer> pointIdMap = pointList.stream()
                .collect(Collectors.toMap(StudentWrongKnowledgeBO::getPointName, StudentWrongKnowledgeBO::getPointId));
        String questionPrompts = createQuestionPrompts(difficulties, pointAccessRateMap, pointIdMap, LIMIT_QUESTION_NO, courseNameByid);

        questionRedisCache.setQuestionDifficultyListInCache(Integer.valueOf(floorId), questionDifficultyList);
        log.info("缓存塔层【{}】难度排布成功", floorId);

        //初始化redis中的队列
        questionRedisCache.setQuestionNumToCache(Integer.valueOf(floorId), 0);
        questionRedisCache.setQuestionToCache(Integer.valueOf(floorId), new ArrayList<>());
        log.info("初始化塔层【{}】题目成功", floorId);

        //异步实现创建当前层数题目（初始题目）
        exeCreateQuestion(courseId, floorId, LIMIT_QUESTION_NO, questionPrompts, CREATE_QUEXTION_EXECUTOR);

        return Result.success();
    }


    /**
     * 定时任务检查并更新题目
     */
    @Scheduled(fixedDelay = 5000)
    public void checkAndUpdateQuestion(String towerId, String floorId, String courseId, String studentId) {

        //设置最小题目数量
        final int MIN_QUESTION_NO = 2;
        final int UPDATE_QUESTION_NO = 3;

        //查询当前题目数量
        int questionNumInCache = Integer.parseInt(questionRedisCache.getQuestionNumInCache(Integer.valueOf(floorId)));

        //判断是否初始化过(未初始化则退出)
        String createTagInCache = questionRedisCache.isCreateTagInCache(Integer.valueOf(floorId));
        if ("0".equals(createTagInCache)) {
            return;
        }

        //判断是否充足
        if (questionNumInCache > MIN_QUESTION_NO) {
            log.info("塔【{}】塔层【{}】题目充足", towerId, floorId);
            return;
        }

        //进行补充题目到题库中
        List<String> questionDifficultyListInCache = questionRedisCache.getQuestionDifficultyListInCache(Integer.valueOf(floorId), UPDATE_QUESTION_NO);
        List<Difficulty> list = questionDifficultyListInCache.stream().map(a ->
                switch (a) {
                    case "0" -> Difficulty.EASY;
                    case "1" -> Difficulty.MEDIUM;
                    case "2" -> Difficulty.HARD;
                    default -> null;
                }
        ).toList();

        //获取知识点错误信息
        LambdaQueryWrapper<Task> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Task::getFloorId, floorId);
        Task task = taskMapper.selectOne(queryWrapper);
        List<Integer> pointsList = taskServiceImpl.parsePoints(task.getPointIds());
        List<StudentWrongKnowledgeBO> studentWrongKnowledgeByStudentId = knowledgePointMapper.getStudentWrongKnowledgeByStudentId(studentId);
        List<StudentWrongKnowledgeBO> pointList = studentWrongKnowledgeByStudentId.stream().distinct()
                .filter(a -> {
                    for (Integer pointId : pointsList) {
                        if (a.getPointId().equals(pointId)) {
                            return true;
                        }
                    }
                    return false;
                }).toList();
        Map<String, Double> pointAccessRateMap = pointList.stream()
                .collect(Collectors.toMap(StudentWrongKnowledgeBO::getPointName, StudentWrongKnowledgeBO::getAccuracyRate));
        Map<String, Integer> pointIdMap = pointList.stream()
                .collect(Collectors.toMap(StudentWrongKnowledgeBO::getPointName, StudentWrongKnowledgeBO::getPointId));

        //获取课程名字
        String courseNameByid = courseMapper.findCourseNameByid(courseId);
        String questionPrompts = createQuestionPrompts(list, pointAccessRateMap, pointIdMap, UPDATE_QUESTION_NO, courseNameByid);

        //执行更新题目
        CompletableFuture<List<Integer>> listCompletableFuture = exeCreateQuestion(courseId, floorId, UPDATE_QUESTION_NO, questionPrompts, UPDATE_QUEXTION_EXECUTOR);
        listCompletableFuture.join();


    }

    public CompletableFuture<List<Integer>> exeCreateQuestion(String courseId, String floorId, Integer createQuestionNum, String createQuestionPrompts, Executor executor) {

        //异步执行创建
        CompletableFuture<List<Integer>> questionCreateFuture = CompletableFuture.supplyAsync(
                () -> {
                    log.info("开始异步生成题目中.....");
                    try {
                        log.info("准备调用 chatLanguageModel.chat 方法...");
                        String question = chatLanguageModel.chat(UserMessage.userMessage(createQuestionPrompts)).aiMessage().text();
                        log.info("AI生成的题目为{}", question);

                        List<QuestionBank> questionBanks = parseQuestionBanks(question);
                        questionBanks.forEach(questionBank -> {
                            questionBank.setCourseId(Integer.valueOf(courseId));
                            questionBank.setCreatedBy(0);
                            questionBank.setCreatedAt(LocalDateTime.now());
                            questionBank.setUpdatedAt(LocalDateTime.now());
                        });
                        log.info("解析后的题目为{}", questionBanks);

                        questionBankMapper.insert(questionBanks);
                        log.info("成功插入到DB中");

                        List<Integer> pointlist = questionBanks.stream()
                                .map(QuestionBank::getQuestionId)
                                .toList();
                        log.info("<获取到的插入后的题目id列表>{}", pointlist);

                        return pointlist;
                    } catch (Exception e) {
                        log.error("异步任务执行异常", e);
                        throw new RuntimeException(e);
                    }
                }, executor
        ).thenApplyAsync((questionIdList) -> {
            log.info("开始进行redis缓存的处理");
            questionRedisCache.deCressQuestionNumInCache(Integer.valueOf(floorId), 3);
            questionRedisCache.setQuestionToCache(Integer.valueOf(floorId), questionIdList);
            log.info("缓存构建完毕");
            return null;
        }, executor).thenApplyAsync(
                //执行更新初始化状态
                (ss) -> {
                    log.info("开始为塔层【{}】设置初始化状态", floorId);
                    questionRedisCache.setCreateTagToCache(Integer.valueOf(floorId), "1");
                    //从难度系数列表中扣减已经生成的题目
                    questionRedisCache.getQuestionDifficultyListInCache(Integer.valueOf(floorId), Integer.valueOf(createQuestionNum));
                    log.info("成功为塔层【{}】设置初始化状态", floorId);
                    return null;
                }, executor
        );

        return questionCreateFuture;

    }

    /**
     * 解析AI生成的JSON字符串为题库实体列表
     *
     * @param json AI返回的题目JSON数组字符串
     * @return List<QuestionBank>
     * @throws IOException 解析异常
     */
    public static List<QuestionBank> parseQuestionBanks(String json) throws IOException {
        return objectMapper.readValue(json, new TypeReference<List<QuestionBank>>() {
        });
    }

    /**
     * 构建创建题目的提示词
     */
    public String createQuestionPrompts(List<Difficulty> difficulties,
                                        Map<String, Double> pointAccessRateMap,
                                        Map<String, Integer> pointIdMap,
                                        Integer QUESTION_NO,
                                        String courseNameByid) {

        StringBuilder prompt = new StringBuilder();
        prompt.append("你是一名专业的出题专家，请根据以下信息为学生生成个性化定制化练习题。\n");
        prompt.append("请严格按照以下字段和格式生成题目，方便程序直接解析。\n\n");

        // 总题数
        prompt.append("总共需要生成 ").append(QUESTION_NO).append(" 道题目。\n");

        // 课程
        prompt.append("课程名称：").append(courseNameByid).append("\n\n");

        // 题目难度分布
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

        // 知识点与通过率 + 对应 ID
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

        // 题目情境要求
        prompt.append("请结合知识点设计真实应用情境，使题目贴近实际生活或职业场景。\n\n");

        // 生成规则
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
        prompt.append("7. 所有 JSON 格式必须有效，不能包含注释、尾随逗号或多余字符。\n\n");

        // 出题质量控制
        prompt.append("质量控制：\n");
        prompt.append("- 题目不得重复或过于相似。\n");
        prompt.append("- 题干必须紧扣知识点，不得偏题。\n");
        prompt.append("- 选项应清晰且有区分度，避免含糊选项。\n");
        prompt.append("- 不得使用“以上都是”、“以上都不对”等无效答案。\n");
        prompt.append("- 单选题正确答案唯一，多选题正确答案必须完全匹配。\n");
        prompt.append("- 不得生成未定义知识点或超范围内容。\n\n");

        // 输出格式示例
        prompt.append("请严格生成 ").append(QUESTION_NO).append(" 道题目，整体以纯 JSON 数组格式输出，示例如下：\n");
        prompt.append("[\n");
        prompt.append("  {\n");
        prompt.append("    \"question_id\": null,\n");
        prompt.append("    \"course_id\": -1,\n");
        prompt.append("    \"point_id\": 1,\n");
        prompt.append("    \"question_type\": \"single_choice\",\n");
        prompt.append("    \"question_content\": \"这里是题目描述\",\n");
        // 这里的 question_options 是字符串形式，注意双引号和内部引号都要转义
        prompt.append("    \"question_options\": \"[{\\\"label\\\": \\\"A\\\", \\\"content\\\": \\\"选项1\\\", \\\"is_correct\\\": false}, {\\\"label\\\": \\\"B\\\", \\\"content\\\": \\\"选项2\\\", \\\"is_correct\\\": true}]\",\n");
        prompt.append("    \"correct_answer\": \"[\\\"B\\\"]\",\n");
        prompt.append("    \"explanation\": \"答案解析\",\n");
        prompt.append("    \"difficulty_level\": \"easy\",\n");
        prompt.append("    \"score_points\": 1.0\n");
        prompt.append("  }\n");
        prompt.append("]\n");


        prompt.append("请只输出 JSON 数组，切勿输出任何解释性文字、注释或多余内容。");

        return prompt.toString();
    }


    /**
     * 将获取的排名的studentId 转换成排行榜上的信息
     */
    public List<SortedUser> toSortedUserList(Set<ZSetOperations.TypedTuple<String>> totleSortedList) {

        List<SortedUser> sortedUsers = totleSortedList.stream().map(a -> {
            SortedUser sortedUser = new SortedUser();
            Double maxTowerFloorNo = a.getScore();
            String studentId = a.getValue();
            sortedUser.setMaxTowerFloorNo(maxTowerFloorNo);
            sortedUser.setStudentId(studentId);
            return sortedUser;
        }).toList();

        // 批量查询所有学生信息
        List<String> studentIds = sortedUsers.stream().map(SortedUser::getStudentId).distinct().collect(Collectors.toList());

        LambdaQueryWrapper<GameUser> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(GameUser::getStudentId, studentIds);
        List<GameUser> gameUsers = gameUserMapper.selectList(queryWrapper);

        // 建立 studentId -> GameUser 的映射
        Map<String, GameUser> gameUserMap = gameUsers.stream().collect(Collectors.toMap(g -> String.valueOf(g.getStudentId()), g -> g));

        // 填充名字和等级
        for (SortedUser su : sortedUsers) {
            GameUser gu = gameUserMap.get(su.getStudentId());
            if (gu != null) {
                su.setStudentName(gu.getGameName());
                su.setStudentLevel(gu.getLevel());
            } else {
                log.error("用户不存在，studentId={}", su.getStudentId());
                throw new RuntimeException("用户不存在");
            }
        }

        return sortedUsers;
    }

    /**
     * 计算每层难度权重（曲线递增）
     */
    public static Map<Difficulty, Double> getWeightsForFloor(int floor, int totalFloors) {
        double maxEasy = 0.6, minEasy = 0.1;
        double maxHard = 0.7, minHard = 0.1;

        double progress = (double) (floor - 1) / (totalFloors - 1);
        // 指数曲线，后期陡增难度
        progress = Math.pow(progress, 1.5);

        double easy = maxEasy - (maxEasy - minEasy) * progress;
        double hard = minHard + (maxHard - minHard) * progress;
        double medium = Math.max(0, 1.0 - easy - hard);

        Map<Difficulty, Double> weights = new EnumMap<>(Difficulty.class);
        weights.put(Difficulty.EASY, easy);
        weights.put(Difficulty.MEDIUM, medium);
        weights.put(Difficulty.HARD, hard);
        return weights;
    }

    /**
     * 题目数量根据Boss血量动态决定
     *
     * @param bossHp 当前Boss血量
     * @param maxHp  Boss最大血量
     */
    public static int getQuestionCountByBossHp(int bossHp, int maxHp) {
        int minQuestions = 3;
        int maxQuestions = 20;

        double ratio = Math.min(1.0, (double) bossHp / maxHp);
        return (int) Math.round(minQuestions + ratio * (maxQuestions - minQuestions));
    }

    /**
     * 根据权重随机生成题目难度列表，带简单题/难题数量上限保底
     */
    public static List<Difficulty> randomDifficultyListWithLimits(
            int floor, int totalFloors, int questionCount, Map<Difficulty, Double> weightMap,
            int baseMaxEasy, int baseMaxHard, double curveFactor) {

        List<Difficulty> result = new ArrayList<>();
        Random random = new Random();

        // 原始进度 0 ~ 1
        double progress = (double) (floor - 1) / (totalFloors - 1);

        // 曲线进度 (指数增长，前期平缓，后期陡升)
        double curvedProgress = Math.pow(progress, curveFactor);

        // 动态调整上限
        int maxEasyCount = Math.max(1, (int) Math.round(baseMaxEasy * (1.0 - curvedProgress)));
        int maxHardCount = Math.min(questionCount - 1, (int) Math.round(baseMaxHard * (1.0 + curvedProgress)));

        int easyCount = 0;
        int hardCount = 0;

        for (int i = 0; i < questionCount; i++) {
            Difficulty chosen = null;

            // 最多尝试10次选合适难度
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

                if (chosen == Difficulty.EASY && easyCount >= maxEasyCount) {
                    continue;
                }
                if (chosen == Difficulty.HARD && hardCount >= maxHardCount) {
                    continue;
                }
                break;
            }

            // 如果没选到合适的，强制选中等题
            if (chosen == null ||
                    (chosen == Difficulty.EASY && easyCount >= maxEasyCount) ||
                    (chosen == Difficulty.HARD && hardCount >= maxHardCount)) {
                chosen = Difficulty.MEDIUM;
            }

            if (chosen == Difficulty.EASY) {
                easyCount++;
            }
            if (chosen == Difficulty.HARD) {
                hardCount++;
            }

            result.add(chosen);
        }

        return result;
    }


}