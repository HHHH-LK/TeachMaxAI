package com.aiproject.smartcampus.model.functioncalling.toolutils;

import com.aiproject.smartcampus.commons.redis.RedisSort;
import com.aiproject.smartcampus.mapper.*;
import com.aiproject.smartcampus.pojo.bo.Side;
import com.aiproject.smartcampus.pojo.bo.SimpleKnowledgeAnalysisBO;
import com.aiproject.smartcampus.pojo.po.*;
import com.aiproject.smartcampus.pojo.vo.KnowledgePointVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.PreDestroy;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static com.aiproject.smartcampus.commons.utils.JsonUtils.parseRelationsFromJson;

/**
 * @program: TeacherMaxAI
 * @description: agent智能创建塔（优化版-修复背景生成）
 * @author: lk_hhh
 * @create: 2025-08-07 17:10
 **/

@Slf4j
@Component
@RequiredArgsConstructor
public class TowerCreateToolUtils {

    private final KnowledgePointMapper knowledgePointMapper;
    private final ChatLanguageModel chatLanguageModel;
    private final CourseMapper courseMapper;
    private final TowerMapper towerMapper;
    private final TransactionTemplate transactionTemplate;
    private final TowerFloorMapper towerFloorMapper;
    private final ItemMapper itemMapper;
    private final TaskMapper taskMapper;
    private final RedisSort redisSort;
    private final BossMapper bossMapper;

    // 测试期建议 true：等待故事生成完成再返回，便于立即在DB看到描述
    private static final boolean WAIT_STORY_COMPLETION = false;
    private static final int MAX_POINTS_PER_FLOOR = 3;

    // 背景故事线程池（命名，非守护线程）
    private final ExecutorService STORY_BACKGROUND_CREATE_EXECUTOR = new ThreadPoolExecutor(
            20, 20,
            60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(200),
            r -> {
                Thread t = new Thread(r);
                t.setName("tower-story-exec-" + UUID.randomUUID());
                t.setDaemon(false);
                return t;
            },
            new ThreadPoolExecutor.CallerRunsPolicy()
    );

    @PreDestroy
    public void shutdown() {
        STORY_BACKGROUND_CREATE_EXECUTOR.shutdown();
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class KnowledgePointNode {
        String pointId;
        String pointName;
        String description;
        String difficultyLevel;
        Double accuracyRate;
        Set<String> parents = new HashSet<>();
        Set<String> children = new HashSet<>();
    }

    public Boolean createTowerByStudentIdAndCourseId(String studentId, String courseId) {
        try {
            // 1) 课程知识点
            List<KnowledgePointVO> kpList = knowledgePointMapper.getCourseKnowledgePoints(Integer.valueOf(courseId));
            if (kpList == null || kpList.isEmpty()) {
                log.warn("课程 {} 没有知识点，不创建学习塔", courseId);
                return false;
            }
            Map<String, String> idToName = kpList.stream()
                    .collect(Collectors.toMap(
                            kp -> String.valueOf(kp.getPointId()),
                            KnowledgePointVO::getPointName,
                            (a, b) -> a,
                            LinkedHashMap::new
                    ));
            List<String> pointIds = new ArrayList<>(idToName.keySet());
            List<String> pointNames = new ArrayList<>(idToName.values());

            List<SimpleKnowledgeAnalysisBO> analysisList = knowledgePointMapper.getSimpleKnowledgeAnalysis(pointIds, studentId);
            if (analysisList == null) analysisList = Collections.emptyList();

            // 2) 去状态化图
            Map<String, KnowledgePointNode> nodesById = new LinkedHashMap<>();
            Map<String, String> nameToId = new HashMap<>();

            for (SimpleKnowledgeAnalysisBO a : analysisList) {
                KnowledgePointNode n = new KnowledgePointNode();
                n.setPointId(String.valueOf(a.getPointId()));
                n.setPointName(a.getPointName());
                n.setDescription(a.getDescription());
                n.setDifficultyLevel(a.getDifficultyLevel());
                n.setAccuracyRate(a.getAccuracyRate());
                nodesById.put(n.getPointId(), n);
                nameToId.put(n.getPointName(), n.getPointId());
            }
            for (KnowledgePointVO vo : kpList) {
                String pid = String.valueOf(vo.getPointId());
                if (!nodesById.containsKey(pid)) {
                    KnowledgePointNode n = new KnowledgePointNode();
                    n.setPointId(pid);
                    n.setPointName(vo.getPointName());
                    n.setDescription("");
                    n.setDifficultyLevel("");
                    n.setAccuracyRate(null);
                    nodesById.put(pid, n);
                    nameToId.putIfAbsent(n.getPointName(), n.getPointId());
                }
            }

            // 3) LLM 依赖
            List<Side> relations = buildKnowledgeRelations(pointNames);
            log.info("LLM 返回依赖关系条数: {}", relations == null ? 0 : relations.size());
            if (relations != null) {
                for (Side side : relations) {
                    if (side == null || side.getFrom() == null || side.getTo() == null) continue;
                    String fromName = side.getFrom().trim();
                    String toName = side.getTo().trim();
                    String fromId = nameToId.get(fromName);
                    String toId = nameToId.get(toName);
                    if (fromId == null || toId == null) {
                        log.warn("忽略未知知识点依赖: {} -> {}", fromName, toName);
                        continue;
                    }
                    if (fromId.equals(toId)) {
                        log.warn("忽略自依赖: {} -> {}", fromName, toName);
                        continue;
                    }
                    KnowledgePointNode from = nodesById.get(fromId);
                    KnowledgePointNode to = nodesById.get(toId);
                    if (to.getParents().add(fromId)) {
                        from.getChildren().add(toId);
                    }
                }
            }

            Map<String, Integer> indegree = new HashMap<>();
            for (KnowledgePointNode n : nodesById.values()) {
                indegree.put(n.getPointId(), n.getParents().size());
            }

            // 4) 分层
            Map<Integer, List<Integer>> floorToPointIds = buildBalancedFloors(nodesById, indegree, MAX_POINTS_PER_FLOOR);
            if (floorToPointIds.isEmpty()) {
                log.warn("拓扑分层为空，启用兜底分层");
                int i = 1, idx = 0;
                List<String> allIds = new ArrayList<>(nodesById.keySet());
                while (idx < allIds.size()) {
                    List<Integer> group = allIds.subList(idx, Math.min(idx + MAX_POINTS_PER_FLOOR, allIds.size()))
                            .stream().map(Integer::valueOf).collect(Collectors.toList());
                    floorToPointIds.put(i++, group);
                    idx += MAX_POINTS_PER_FLOOR;
                }
            }

            // 5) 创建塔
            int totalFloors = floorToPointIds.size();
            Tower tower = getTowerInfo(totalFloors, courseId, studentId);
            insertTower(tower);

            Map<Integer, Long> floorNoToId = initFloorsTasksAndBosses(tower, floorToPointIds);

            // 6) 排行榜初始化
            redisSort.setStudentMaxFloor(String.valueOf(tower.getTowerId()), studentId, "0");
            redisSort.setStudentTotalScore(studentId, "0");

            // 7) 生成背景（异步），并使用条件更新，避免主键映射问题
            CompletableFuture<String> towerDescFuture = CompletableFuture.supplyAsync(() -> {
                log.info("开始生成主塔故事背景");
                try {
                    String desc = generateTowerBackground(tower.getName(), new ArrayList<>(idToName.values()), totalFloors);
                    if (desc == null || desc.isBlank()) {
                        desc = "在远古遗迹之巅，知识化作阶梯，探路者循着微光——每一层都是一次理解的突破。";
                    }
                    LambdaUpdateWrapper<Tower> uw = new LambdaUpdateWrapper<>();
                    uw.eq(Tower::getTowerId, tower.getTowerId())
                            .set(Tower::getDescription, desc);
                    int update = towerMapper.update(null, uw);
                    log.info("主塔背景已生成(前80字)：{}", desc.substring(0, Math.min(80, desc.length())));
                    if (update <= 0) {
                        log.error("主塔背景更新失败, towerId={}", tower.getTowerId());
                    }
                    return desc;
                } catch (Exception e) {
                    log.error("生成主塔背景失败", e);
                    return "";
                }
            }, STORY_BACKGROUND_CREATE_EXECUTOR);

            List<CompletableFuture<Void>> floorStoryFutures = new ArrayList<>();
            for (Map.Entry<Integer, List<Integer>> e : floorToPointIds.entrySet()) {
                Integer floorNo = e.getKey();
                List<Integer> pids = e.getValue();
                List<String> names = pids.stream().map(String::valueOf).map(idToName::get).filter(Objects::nonNull).collect(Collectors.toList());
                Long floorId = floorNoToId.get(floorNo);
                if (floorId == null) continue;

                CompletableFuture<Void> f = towerDescFuture.thenAcceptAsync(towerDesc -> {
                    try {
                        log.info("开始生成塔层故事背景");
                        String floorStory = generateFloorBackground(towerDesc, floorNo, names);
                        if (floorStory == null || floorStory.isBlank()) {
                            floorStory = "尘封的石壁泛起微光，本层将围绕这些知识展开试炼。";
                        }
                        LambdaUpdateWrapper<TowerFloor> uf = new LambdaUpdateWrapper<>();
                        uf.eq(TowerFloor::getFloorId, floorId)
                                .set(TowerFloor::getDescription, floorStory);
                        int update = towerFloorMapper.update(null, uf);
                        log.info("楼层 {} 背景已生成(前80字)：{}", floorNo, floorStory.substring(0, Math.min(80, floorStory.length())));
                        if (update <= 0) {
                            log.error("楼层 {} 背景更新失败, floorId={}", floorNo, floorId);
                        }
                    } catch (Exception ex) {
                        log.error("生成楼层 {} 背景失败", floorNo, ex);
                    }
                }, STORY_BACKGROUND_CREATE_EXECUTOR);
                floorStoryFutures.add(f);
            }

            if (WAIT_STORY_COMPLETION) {
                try {
                    CompletableFuture<Void> allFloors = CompletableFuture.allOf(floorStoryFutures.toArray(new CompletableFuture[0]));
                    allFloors.get(60, TimeUnit.SECONDS);
                    log.info("背景故事生成已完成（同步等待）");
                } catch (Exception ex) {
                    log.warn("等待背景故事生成超时或异常，任务将继续在后台执行", ex);
                }
            }

            log.info("学习塔创建成功, towerId={}, totalFloors={}", tower.getTowerId(), totalFloors);
            return true;
        } catch (Exception e) {
            log.error("创建学习塔失败", e);
            return false;
        }
    }

    private void insertTower(Tower tower) {
        transactionTemplate.execute(status -> {
            try {
                int insert = towerMapper.insert(tower);
                if (insert <= 0 || tower.getTowerId() == null) {
                    throw new RuntimeException("主塔插入失败或未返回主键");
                }
                log.info("主塔插入成功, towerId={}", tower.getTowerId());
                return true;
            } catch (Exception e) {
                status.setRollbackOnly();
                throw e;
            }
        });
    }

    private Map<Integer, Long> initFloorsTasksAndBosses(Tower tower, Map<Integer, List<Integer>> floorToPointIds) {
        Map<Integer, Long> floorNoToId = new HashMap<>();

        transactionTemplate.execute(status -> {
            try {
                // 插入楼层
                for (int i = 1; i <= tower.getTotalFloors(); i++) {
                    TowerFloor floor = new TowerFloor();
                    floor.setTowerId(tower.getTowerId());
                    floor.setFloorNo(i);
                    floor.setIsPass(0);
                    floor.setUnlocked(i == 1);
                    int insert = towerFloorMapper.insert(floor);
                    if (insert <= 0) {
                        throw new RuntimeException("第" + i + "层塔创建失败");
                    }
                    floorNoToId.put(i, floor.getFloorId());
                }

                // 插入任务
                for (Map.Entry<Integer, List<Integer>> e : floorToPointIds.entrySet()) {
                    Integer floorNo = e.getKey();
                    List<Integer> pids = e.getValue();
                    Long floorId = floorNoToId.get(floorNo);
                    if (floorId == null) {
                        throw new RuntimeException("未找到 floorId, floorNo=" + floorNo);
                    }
                    Task task = new Task();
                    task.setFloorId(floorId);
                    task.setPointIds(pids.toString()); // 若需要CSV: pids.stream().map(String::valueOf).collect(Collectors.joining(","))
                    task.setRewardItemQty(1);
                    task.setRewardExp(getTowerFloorExp(floorNo));
                    task.setRewardItemRarity(getTowerFloorItem(floorNo));
                    int insert = taskMapper.insert(task);
                    if (insert <= 0) {
                        throw new RuntimeException("任务插入失败, floorNo=" + floorNo);
                    }
                }

                // 插入 Boss
                for (int i = 1; i <= tower.getTotalFloors(); i++) {
                    Monster monster = new Monster();
                    monster.setName(setBossNameByFloorNo(i));
                    monster.setHp(setBossHp(i));
                    monster.setFloorId(floorNoToId.get(i));
                    int insert = bossMapper.insert(monster);
                    if (insert <= 0) {
                        throw new RuntimeException("Boss 插入失败, floorNo=" + i);
                    }
                }

                log.info("楼层/任务/Boss 插入完成, towerId={}", tower.getTowerId());
                return true;
            } catch (Exception e) {
                status.setRollbackOnly();
                throw e;
            }
        });

        return floorNoToId;
    }

    // 若需要重复建塔时先清理旧数据，可启用此方法
    @SuppressWarnings("unused")
    private void cleanupExistingTowers(String courseId, String studentId) {
        List<Tower> towers = towerMapper.selectList(new LambdaQueryWrapper<Tower>()
                .eq(Tower::getCourseId, Long.valueOf(courseId))
                .eq(Tower::getStudentId, Long.valueOf(studentId)));
        if (towers == null || towers.isEmpty()) return;

        transactionTemplate.execute(status -> {
            try {
                for (Tower t : towers) {
                    List<TowerFloor> floors = towerFloorMapper.selectList(new LambdaQueryWrapper<TowerFloor>()
                            .eq(TowerFloor::getTowerId, t.getTowerId()));
                    for (TowerFloor f : floors) {
                        taskMapper.delete(new QueryWrapper<Task>().eq("floor_id", f.getFloorId()));
                        bossMapper.delete(new QueryWrapper<Monster>().eq("floor_id", f.getFloorId()));
                    }
                    towerFloorMapper.delete(new QueryWrapper<TowerFloor>().eq("tower_id", t.getTowerId()));
                    towerMapper.delete(new QueryWrapper<Tower>().eq("tower_id", t.getTowerId()));
                }
                return true;
            } catch (Exception e) {
                status.setRollbackOnly();
                throw e;
            }
        });
        log.info("已清理旧塔数据, courseId={}, studentId={}", courseId, studentId);
    }

    private List<Side> buildKnowledgeRelations(List<String> pointNameList) {
        try {
            String sys = createKnowledgeGraphPrompt();
            String user = "请按照下面的知识点列表进行处理：\n" +
                    pointNameList.stream().collect(Collectors.joining("\n"));
            List<ChatMessage> messages = Arrays.asList(
                    SystemMessage.systemMessage(sys),
                    UserMessage.userMessage(user)
            );
            String resp = chatLanguageModel.chat(messages).aiMessage().text();
            if (resp == null || resp.trim().isEmpty()) return Collections.emptyList();
            return parseRelationsFromJson(resp.trim());
        } catch (Exception e) {
            log.error("解析知识依赖失败，将使用无依赖兜底策略", e);
            return Collections.emptyList();
        }
    }

    /**
     * 分层算法（硬约束：每层最多 maxPerFloor 个），带环的情况下自动断环
     */
    private Map<Integer, List<Integer>> buildBalancedFloors(Map<String, KnowledgePointNode> nodesById,
                                                            Map<String, Integer> indegreeInit,
                                                            int maxPerFloor) {
        Map<Integer, List<Integer>> levelMap = new LinkedHashMap<>();
        Map<String, Integer> indegree = new HashMap<>(indegreeInit);
        int total = nodesById.size();
        Set<String> processed = new HashSet<>();

        // 初始 0 入度集合
        List<String> zeroList = indegree.entrySet().stream()
                .filter(e -> e.getValue() == 0)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        Comparator<String> cmp = Comparator
                .comparing((String id) -> Optional.ofNullable(nodesById.get(id).getAccuracyRate()).orElse(1.0))
                .thenComparing(id -> nodesById.get(id).getPointName(), Comparator.nullsLast(Comparator.naturalOrder()));

        int level = 1;
        while (processed.size() < total) {
            if (zeroList.isEmpty()) {
                List<String> remain = indegree.entrySet().stream()
                        .filter(e -> !processed.contains(e.getKey()))
                        .sorted(Comparator.comparingInt(Map.Entry::getValue))
                        .map(Map.Entry::getKey)
                        .collect(Collectors.toList());
                if (remain.isEmpty()) break;
                zeroList.addAll(remain);
                log.warn("检测到循环依赖，启动断环策略。本层将强制选择未处理的最小入度节点作为起点。");
            }

            zeroList.sort(cmp);
            List<String> currentLayer = new ArrayList<>();
            int take = Math.min(maxPerFloor, zeroList.size());
            for (int i = 0; i < take; i++) currentLayer.add(zeroList.get(i));

            List<String> carryOver = new ArrayList<>();
            if (zeroList.size() > take) {
                carryOver.addAll(zeroList.subList(take, zeroList.size()));
            }

            levelMap.put(level, currentLayer.stream().map(Integer::valueOf).collect(Collectors.toList()));
            processed.addAll(currentLayer);

            List<String> newZeros = new ArrayList<>();
            for (String id : currentLayer) {
                KnowledgePointNode node = nodesById.get(id);
                for (String childId : node.getChildren()) {
                    if (processed.contains(childId)) continue;
                    indegree.computeIfPresent(childId, (k, v) -> v - 1);
                    if (indegree.get(childId) != null && indegree.get(childId) == 0) newZeros.add(childId);
                }
            }

            zeroList = new ArrayList<>();
            zeroList.addAll(carryOver);
            zeroList.addAll(newZeros);

            level++;
        }

        for (Map.Entry<Integer, List<Integer>> e : levelMap.entrySet()) {
            log.info("第{}层：知识点ID {}", e.getKey(), e.getValue());
        }
        return levelMap;
    }

    private String generateTowerBackground(String towerName, List<String> pointNames, int totalFloors) {
        String prompt = "请为学习塔【" + towerName + "】生成一个完整的世界观故事背景。\n" +
                "它包含的知识点有：" + pointNames + "；总层数为：" + totalFloors + "。\n" +
                "要求：\n" +
                "- 有整体世界观和核心任务驱动\n" +
                "- 融合知识点的主题意象，但不要逐条解释知识点\n" +
                "- 语言生动有代入感\n" +
                "- 只返回故事背景文本，不要包含任何解释或提示";
        try {
            String text = chatLanguageModel.chat(
                    Arrays.asList(
                            SystemMessage.systemMessage("你是擅长写世界观背景的叙事设计师，只输出背景文本。"),
                            UserMessage.userMessage(prompt)
                    )
            ).aiMessage().text();
            return text == null ? "" : text.trim();
        } catch (Exception e) {
            log.error("LLM 生成主塔背景异常", e);
            return "";
        }
    }

    private String generateFloorBackground(String towerBackground, int floorNo, List<String> knowledgeNames) {
        String prompt = String.format(
                "结合总塔的背景：%s\n以及第%d层的知识主题：%s\n请写出本层的故事背景（只返回背景文本，不要解释或提示）。",
                towerBackground == null ? "" : towerBackground, floorNo, knowledgeNames
        );
        try {
            String text = chatLanguageModel.chat(
                    Arrays.asList(
                            SystemMessage.systemMessage("你是擅长章节化叙事的写作者，只输出背景文本。"),
                            UserMessage.userMessage(prompt)
                    )
            ).aiMessage().text();
            return text == null ? "" : text.trim();
        } catch (Exception e) {
            log.error("LLM 生成第{}层背景异常", floorNo, e);
            return "";
        }
    }

    public Tower getTowerInfo(Integer size, String courseId, String studentId) {
        String courseName = courseMapper.getCourseByid(Integer.valueOf(courseId));
        String towerName = courseName + "_学习塔";
        Tower tower = new Tower();
        tower.setName(towerName);
        tower.setCourseId(Long.valueOf(courseId));
        tower.setTotalFloors(size == null ? 0 : size);
        tower.setStudentId(Long.valueOf(studentId));
        return tower;
    }

    private Integer getTowerFloorExp(Integer floorNo) {
        if (floorNo == null || floorNo <= 0) return 0;
        if (floorNo <= 10) return 100 + floorNo * 50;
        else if (floorNo <= 25) return 700 + (floorNo - 10) * 100;
        else if (floorNo <= 50) return 2200 + (floorNo - 25) * 150;
        else if (floorNo <= 75) return 5950 + (floorNo - 50) * 200;
        else return 11950 + (floorNo - 75) * 300;
    }

    private Integer getTowerFloorItem(Integer floorNo) {
        if (floorNo == null || floorNo <= 3) return 0;
        if (floorNo <= 6) return 1;
        return 2;
    }

    @SuppressWarnings("unused")
    private Item getRandomItemByRarity(int rarity) {
        List<Item> items = itemMapper.selectList(new QueryWrapper<Item>().eq("rarity", rarity));
        if (items == null || items.isEmpty()) return null;
        return items.get(new Random().nextInt(items.size()));
    }

    private int setBossHp(int floorNo) {
        int baseHp = 200;
        double growthRate = 0.15;
        int hp = (int) Math.round(baseHp + baseHp * (floorNo * growthRate));
        if (floorNo % 5 == 0) hp = (int) Math.round(hp * 1.5);
        return hp;
    }

    private String setBossNameByFloorNo(int floorNo) {
        return "boss-" + floorNo + "号";
    }

    private String createKnowledgeGraphPrompt() {
        String KNOWLEDGE_GRAPH_PROMPT = """
                你是一名专业的教学设计专家，负责构建知识点间的学习依赖关系，生成符合认知规律的学习路径图（有向无环图，DAG）。
                
                ===============================
                ## 核心目标
                - 构建符合人类学习认知规律的知识依赖关系
                - 确保学习路径循序渐进，避免认知负荷过载
                - 每个学习层级最多包含3个知识点，便于并行掌握
                - 严格遵循"概念理解 → 原理掌握 → 技能应用 → 综合运用"的学习规律
                
                ===============================
                ## 依赖关系判定标准
                必须依赖（强制先学）：
                1. 核心概念依赖：B 的理解绝对需要 A 的概念基础
                2. 直接逻辑依赖：B 是 A 的直接延伸或深化
                3. 技能前置依赖：掌握 B 必须先具备 A 的操作技能
                避免建立依赖：
                - 工具性、并列特性、可由更基础概念支撑
                
                ===============================
                ## 输出格式要求
                严格返回如下JSON：
                {
                  "relations": [
                    "前置知识点:后续知识点",
                    ...
                  ]
                }
                仅输出JSON内容，不添加任何解释性文字；知识点名称与输入完全一致。
                """;
        return KNOWLEDGE_GRAPH_PROMPT;
    }
}