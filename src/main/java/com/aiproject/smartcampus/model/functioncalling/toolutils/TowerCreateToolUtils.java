package com.aiproject.smartcampus.model.functioncalling.toolutils;

import com.aiproject.smartcampus.mapper.*;
import com.aiproject.smartcampus.pojo.bo.Side;
import com.aiproject.smartcampus.pojo.bo.SimpleKnowledgeAnalysisBO;
import com.aiproject.smartcampus.pojo.po.*;
import com.aiproject.smartcampus.pojo.vo.KnowledgePointVO;
import com.aiproject.smartcampus.pojo.vo.StudentKnowledgePointVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static com.aiproject.smartcampus.commons.utils.JsonUtils.parseRelationsFromJson;

/**
 * @program: TeacherMaxAI
 * @description: agent智能创建塔
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
    private final BossMapper bossMapper;


    private Executor STORY_BACKGROUND_CREATE_EXECUTOR = Executors.newFixedThreadPool(5);
    private Map<String, KnowledgePointNode> POINTNAME_TO_NODE_MAP = new ConcurrentHashMap<>();
    private Map<KnowledgePointNode, List<KnowledgePointNode>> KNOWLEDGE_POINT_NODE_MAP = new ConcurrentHashMap<>();
    private Map<KnowledgePointNode, Integer> INDEGREE_MAP = new HashMap<>();
    private Map<String, Tower> COURSE_TOWER_MAP = new HashMap<>();


    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class KnowledgePointNode {

        String pointId;
        String pointName;
        String description;
        String difficultyLevel;
        Double accuracyRate;

    }


    public Boolean createTowerByStudentIdAndCourseId(String studentId, String courseId) {

        //查询该学生课程中的所有知识点
        List<KnowledgePointVO> studentKnowledgePointVOS = knowledgePointMapper.getCourseKnowledgePoints(Integer.valueOf(courseId));

        //获取所有知识点的名字
        List<String> pointNameList = studentKnowledgePointVOS.stream().map(a -> a.getPointName()).toList();
        log.info("知识点名字列表：{}", pointNameList.toString());

        //获取所有pointId并获取所有知识点的通过率
        List<String> pointIdList = studentKnowledgePointVOS.stream().map(a -> String.valueOf(a.getPointId())).toList();
        List<SimpleKnowledgeAnalysisBO> simpleKnowledgeAnalysis = knowledgePointMapper.getSimpleKnowledgeAnalysis(pointIdList, studentId);

        //获取知识点id对应的通过率
        Map<Integer, Double> POINT_ACCURACYRATE_MAP = simpleKnowledgeAnalysis.stream()
                .sorted(
                        (a, b) ->
                                a.getAccuracyRate().compareTo(b.getAccuracyRate())
                ).collect(Collectors.toMap(
                        SimpleKnowledgeAnalysisBO::getPointId,
                        SimpleKnowledgeAnalysisBO::getAccuracyRate,
                        (v1, v2) -> (v1 + v2) / 2,
                        LinkedHashMap::new)
                );

        //初始化知识图谱节点
        initializationKnowledgeGraph(simpleKnowledgeAnalysis);

        //创建对应课程学科的知识图谱
        createKnowledgeGraph(pointNameList);

        //初始化塔层与塔层相关信息
        createTowerDefault(courseId);


        return true;

    }

    /**
     * 知识图谱节点初始化
     */
    private void initializationKnowledgeGraph(List<SimpleKnowledgeAnalysisBO> simpleKnowledgeAnalysis) {

        simpleKnowledgeAnalysis.stream().forEach(
                a -> {
                    //创建节点
                    KnowledgePointNode knowledgePointNode = new KnowledgePointNode();
                    String pointName = a.getPointName();
                    knowledgePointNode.setPointId(String.valueOf(a.getPointId()));
                    knowledgePointNode.setPointName(pointName);
                    knowledgePointNode.setDescription(a.getDescription());
                    knowledgePointNode.setDifficultyLevel(a.getDifficultyLevel());
                    knowledgePointNode.setAccuracyRate(a.getAccuracyRate());

                    //添加节点映射
                    POINTNAME_TO_NODE_MAP.put(pointName, knowledgePointNode);
                    //添加知识图谱节点初始化
                    KNOWLEDGE_POINT_NODE_MAP.put(knowledgePointNode, new ArrayList<>());

                }
        );

    }

    /**
     * 创建初始固定塔层（初始值默认拓扑排序后的层数）
     */
    private void createTowerDefault(String courseId) {

        //获取每层知识点的结果
        Map<Integer, List<Integer>> TOWER_FLOOR_POINTS_MAP = getTowerFloorPointIds();

        Tower towerInfo = getTowerInfo(TOWER_FLOOR_POINTS_MAP.size(), courseId);
        //加入db中
        initTowerToDB(towerInfo, courseId);

        //初始化塔层
        initTowerFloorToDB(towerInfo, TOWER_FLOOR_POINTS_MAP);

        //初始化每层塔任务
        initTowerFloorTaskToDB(towerInfo, TOWER_FLOOR_POINTS_MAP);

        //初始化每层怪物
        initTowerFloorBossToDB(towerInfo);


    }

    /**
     * 为每层添加boss
     */
    private void initTowerFloorBossToDB(Tower towerInfo) {

        //获取主塔中所有的塔层
        Long towerId = towerInfo.getTowerId();
        LambdaQueryWrapper<TowerFloor> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(TowerFloor::getTowerId, towerId);
        List<TowerFloor> towerFloors = towerFloorMapper.selectList(lambdaQueryWrapper);

        transactionTemplate.execute(status -> {
                    try {
                        //为每层创建boss
                        towerFloors.stream().forEach(floor -> {
                            Monster monster = new Monster();
                            String bossName = setBossName(floor.getFloorId());
                            int bossHp = setBossHp(floor.getFloorNo());
                            monster.setName(bossName);
                            monster.setHp(bossHp);
                            monster.setFloorId(floor.getFloorId());

                            int insert = bossMapper.insert(monster);
                            if (insert <= 0) {
                                log.error("boss:[{}]创建失败", bossName);
                                throw new RuntimeException("<boss>:" + bossName + "创建失败");
                            }

                            log.info("boss:[{}]<创建成功>", bossName);

                        });

                        return true;
                    } catch (Exception e) {
                        // 事务回滚
                        status.setRollbackOnly();
                        return false;
                    }
                }
        );


    }

    /**
     * 根据层数计算 Boss 血量
     * 基础血量：100
     * 每层递增系数：20%
     * 每5层阶段Boss血量额外×1.5倍
     */
    private int setBossHp(int floorNo) {
        // 基础血量
        int baseHp = 100;

        // 每层增加比例（例如0.2 = 20%）
        double growthRate = 0.2;

        // 按层数递增（线性）
        int hp = (int) Math.round(baseHp + baseHp * (floorNo * growthRate));

        // 每5层是阶段Boss，额外增加难度
        if (floorNo % 5 == 0) {
            hp = (int) Math.round(hp * 1.5);
        }

        return hp;
    }


    /**
     * 为boss设置名字
     */
    private String setBossName(Long floorId) {

        return "boss-" + floorId + "号";

    }

    /**
     * 初始化塔层任务到数据库中
     */
    private void initTowerFloorTaskToDB(Tower towerInfo, Map<Integer, List<Integer>> TOWER_FLOOR_POINTS_MAP) {

        //获取主塔中所有的塔层
        Long towerId = towerInfo.getTowerId();
        LambdaQueryWrapper<TowerFloor> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(TowerFloor::getTowerId, towerId);
        List<TowerFloor> towerFloors = towerFloorMapper.selectList(lambdaQueryWrapper);

        transactionTemplate.execute(status -> {
                    try {
                        towerFloors.stream().forEach(towerFloor -> {
                            Long floorId = towerFloor.getFloorId();
                            Integer floorNo = towerFloor.getFloorNo();
                            String points = TOWER_FLOOR_POINTS_MAP.get(floorNo).toString();
                            //计算塔层的经验
                            Integer towerFloorExp = getTowerFloorExp(floorNo);
                            //设置塔层的道具的稀有度
                            Integer towerFloorRarity = getTowerFloorItem(floorNo);

                            Task task = new Task();
                            task.setFloorId(floorId);
                            task.setPointIds(points);
                            //默认数量为1
                            task.setRewardItemQty(1);
                            task.setRewardExp(towerFloorExp);
                            task.setRewardItemRarity(towerFloorRarity);

                            int insert = taskMapper.insert(task);
                            if (insert <= 0) {
                                log.error("任务插入失败");
                                throw new RuntimeException("<任务插入失败>");
                            }

                            log.info("<任务插入成功>{}", task.toString());

                        });

                        return true;
                    } catch (Exception e) {
                        // 事务回滚
                        status.setRollbackOnly();
                        return false;

                    }

                }
        );


    }

    /**
     * 创建塔层到数据库中
     */
    private void initTowerFloorToDB(Tower towerInfo, Map<Integer, List<Integer>> TOWER_FLOOR_POINTS_MAP) {

        Long towerId = towerInfo.getTowerId();
        Integer totalFloors = towerInfo.getTotalFloors();

        //开启事务
        transactionTemplate.execute(status -> {
            try {
                for (int i = 1; i <= totalFloors; i++) {

                    TowerFloor towerFloor = new TowerFloor();
                    towerFloor.setTowerId(towerId);
                    towerFloor.setFloorNo(i);
                    towerFloor.setUnlocked(false);
                    towerFloor.setIsPass(0);

                    int insert = towerFloorMapper.insert(towerFloor);
                    if (insert <= 0) {
                        log.error("第{}层塔创建失败", i);
                        throw new RuntimeException("第" + i + "层塔创建失败");
                    }
                }

                return true;
            } catch (Exception e) {
                // 事务回滚
                status.setRollbackOnly();
                return false;
            }

        });

        STORY_BACKGROUND_CREATE_EXECUTOR.execute(() -> {
            // 获取主塔背景
            LambdaQueryWrapper<Tower> towerQuery = new LambdaQueryWrapper<>();
            towerQuery.eq(Tower::getTowerId, towerId);
            Tower tower = towerMapper.selectOne(towerQuery);
            if (tower == null) {
                log.error("主塔{}不存在", towerId);
                throw new RuntimeException("主塔不存在");
            }

            String description = tower.getDescription();

            for (Map.Entry<Integer, List<Integer>> towerFloorPoints : TOWER_FLOOR_POINTS_MAP.entrySet()) {
                Integer floorNo = towerFloorPoints.getKey();

                LambdaQueryWrapper<TowerFloor> floorQuery = new LambdaQueryWrapper<>();
                floorQuery.eq(TowerFloor::getTowerId, towerId);
                floorQuery.eq(TowerFloor::getFloorNo, floorNo);
                TowerFloor towerFloor = towerFloorMapper.selectOne(floorQuery);

                if (towerFloor == null) {
                    log.warn("<塔层>{}<没有加入背景故事>", floorNo);
                    continue;
                }

                List<Integer> knowledgePoints = towerFloorPoints.getValue();
                List<String> knowledgeNameList = knowledgePoints.stream()
                        .map(a -> knowledgePointMapper.getPonintNameById(String.valueOf(a)))
                        .toList();

                String prompt = "请基于塔的故事背景" + description + "以及该层的知识点" + knowledgeNameList + "生成该塔的故事背景，只返回故事背景";

                String towerFloorDescription = chatLanguageModel.chat(UserMessage.userMessage(prompt))
                        .aiMessage()
                        .text();

                if (towerFloorDescription == null || towerFloorDescription.trim().isEmpty()) {
                    log.warn("AI返回空背景故事，跳过更新，floorNo={}", floorNo);
                    continue;
                }

                towerFloorDescription = towerFloorDescription.trim();

                LambdaUpdateWrapper<TowerFloor> updateWrapper = new LambdaUpdateWrapper<>();
                updateWrapper.eq(TowerFloor::getTowerId, towerId);
                updateWrapper.eq(TowerFloor::getFloorNo, floorNo);
                updateWrapper.set(TowerFloor::getDescription, towerFloorDescription);

                int updateCount = towerFloorMapper.update(null, updateWrapper);
                log.info("更新塔层背景故事，towerId={}, floorNo={}, 影响行数={}", towerId, floorNo, updateCount);

                if (updateCount <= 0) {
                    log.error("更新塔层背景故事失败，towerId={}, floorNo={}", towerId, floorNo);
                    throw new RuntimeException("更新塔层背景故事失败");
                }

                log.info("<<插入背景故事成功>> {}", towerFloorDescription);
            }
        });


    }

    /**
     * 创建主塔到数据库中
     */
    private void initTowerToDB(Tower towerInfo, String courseId) {

        //查询该学生课程中的所有知识点
        List<KnowledgePointVO> studentKnowledgePointVOS = knowledgePointMapper.getCourseKnowledgePoints(Integer.valueOf(courseId));
        List<String> pointNameList = studentKnowledgePointVOS.stream().map(a -> a.getPointName()).toList();
        log.info("知识点名字列表：{}", pointNameList.toString());

        //开启事务
        transactionTemplate.execute(status -> {
            try {
                int insert = towerMapper.insert(towerInfo);
                if (insert <= 0) {
                    log.error("课程:" + courseId + "学习塔插入失败");
                    throw new RuntimeException("课程:" + courseId + "学习塔插入失败");
                }
                log.info("课程:" + courseId + "学习塔插入成功");
                return true;
            } catch (Exception e) {
                // 事务回滚
                status.setRollbackOnly();
                return false;
            }
        });

        //异步创建塔的背景故事
        STORY_BACKGROUND_CREATE_EXECUTOR.execute(() -> {

            String CREATE_DESCRIPTION_PROMPT = "帮我为" + towerInfo.getName() + "他含有以下知识点" + pointNameList + "它的总层数为" + towerInfo.getTotalFloors() + "请基于以上信息为其创建一个故事背景,要求只返回故事背景";
            String description = chatLanguageModel.chat(UserMessage.userMessage(CREATE_DESCRIPTION_PROMPT)).aiMessage().text().trim();
            LambdaUpdateWrapper<Tower> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(Tower::getCourseId, courseId);
            updateWrapper.set(Tower::getDescription, description);
            int update = towerMapper.update(null, updateWrapper);
            if (update <= 0) {
                log.error("课程:" + courseId + "学习塔故事背景插入失败");
                throw new RuntimeException("课程:" + courseId + "学习塔故事背景插入失败");
            }

        });
    }

    /**
     * 计算第几层能够获得的经验值
     */
    private Integer getTowerFloorExp(Integer floorNo) {
        if (floorNo <= 5) {
            return 10 + floorNo * 15;
        } else if (floorNo <= 10) {
            return 100 + (floorNo - 5) * 30;
        } else {
            return 250 + (floorNo - 10) * 50;
        }
    }

    /**
     * 根据通关层数获取奖励稀有度
     *
     * @param floorNo 当前通关层数
     * @return 稀有度
     */
    private Integer getTowerFloorItem(Integer floorNo) {
        int rarity;
        if (floorNo <= 3) {
            rarity = 0; // 普通
        } else if (floorNo <= 6) {
            rarity = 1; // 稀有
        } else {
            rarity = 2; // 史诗
        }

        //返回稀有度
        return rarity;
    }

    /**
     * 模拟从数据库中根据稀有度获取随机道具
     */
    private Item getRandomItemByRarity(int rarity) {
        // 这里假设用 MyBatis-Plus 查询
        List<Item> items = itemMapper.selectList(
                new QueryWrapper<Item>().eq("rarity", rarity)
        );

        if (items.isEmpty()) {
            return null;
        }

        // 随机选一个
        Random random = new Random();
        return items.get(random.nextInt(items.size()));
    }

    /**
     * 获取塔相关信息
     */
    public Tower getTowerInfo(Integer size, String courseId) {


        String courseName = courseMapper.getCourseByid(Integer.valueOf(courseId));
        String towerName = courseName + "_学习塔";


        Tower tower = new Tower();
        tower.setName(towerName);
        tower.setCourseId(Long.valueOf(courseId));
        tower.setTotalFloors(size);


        return tower;

    }

    /**
     * 获取塔层知识点分布
     */
    private Map<Integer, List<Integer>> getTowerFloorPointIds() {

        //初始化知识图谱入度
        initIndegreeMap();
        log.info("知识图谱入度初始化成功，结果为【{}】", INDEGREE_MAP);

        //获取每层知识点分布
        Map<Integer, List<Integer>> levelToPointIds = new HashMap<>();

        // 复制入度防止修改原始数据
        Map<KnowledgePointNode, Integer> indegree = new HashMap<>(INDEGREE_MAP);

        Queue<KnowledgePointNode> queue = new LinkedList<>();
        Map<KnowledgePointNode, Integer> nodeLevelMap = new HashMap<>();

        // 初始化入度为0的节点，层级设为1
        for (Map.Entry<KnowledgePointNode, Integer> entry : indegree.entrySet()) {
            if (entry.getValue() == 0) {
                KnowledgePointNode node = entry.getKey();
                queue.offer(node);
                nodeLevelMap.put(node, 1);
                levelToPointIds.computeIfAbsent(1, k -> new ArrayList<>()).add(Integer.parseInt(node.getPointId()));
            }
        }

        // 反向邻接表：父节点 -> 子节点列表
        Map<KnowledgePointNode, List<KnowledgePointNode>> parentToChildrenMap = new HashMap<>();
        for (Map.Entry<KnowledgePointNode, List<KnowledgePointNode>> entry : KNOWLEDGE_POINT_NODE_MAP.entrySet()) {
            KnowledgePointNode child = entry.getKey();
            for (KnowledgePointNode parent : entry.getValue()) {
                parentToChildrenMap.computeIfAbsent(parent, k -> new ArrayList<>()).add(child);
            }
        }

        // 拓扑排序遍历
        while (!queue.isEmpty()) {
            KnowledgePointNode current = queue.poll();
            int currentLevel = nodeLevelMap.get(current);

            List<KnowledgePointNode> children = parentToChildrenMap.getOrDefault(current, Collections.emptyList());
            for (KnowledgePointNode child : children) {
                indegree.put(child, indegree.get(child) - 1);
                if (indegree.get(child) == 0) {
                    queue.offer(child);
                    int childLevel = currentLevel + 1;
                    nodeLevelMap.put(child, childLevel);
                    levelToPointIds.computeIfAbsent(childLevel, k -> new ArrayList<>()).add(Integer.parseInt(child.getPointId()));
                }
            }
        }

        for (HashMap.Entry<Integer, List<Integer>> entry : levelToPointIds.entrySet()) {
            log.info("第{}层：知识点列表【{}】", entry.getKey(), entry.getValue());
        }

        return levelToPointIds;

    }

    /**
     * 初始化知识图谱入度
     */
    private void initIndegreeMap() {

        KNOWLEDGE_POINT_NODE_MAP.entrySet().forEach(
                a -> INDEGREE_MAP.put(a.getKey(), a.getValue().size())
        );


    }

    /**
     * 知识图谱化算法
     */
    private void createKnowledgeGraph(List<String> pointNameList) {

        //获取系统提示词
        String knowledgeGraphPrompt = createKnowledgeGraphPrompt();

        //构建用户提示词
        StringBuilder knowledgeGraphBuilder = new StringBuilder("请按照这下面的知识点列表进行处理");
        for (String pointName : pointNameList) {
            knowledgeGraphBuilder.append(pointName).append("\n");
        }
        String userMessage = knowledgeGraphBuilder.toString();

        //获取知识点依赖列表
        String responseList = chatLanguageModel.chat(SystemMessage.systemMessage(knowledgeGraphPrompt), UserMessage.userMessage(userMessage)).aiMessage().text();
        log.info("responseList: {}", responseList);

        //解析成节点信息并存入知识图谱中
        parseToNode(responseList);


    }

    /**
     * 解析成节点存入知识图谱中
     */
    private void parseToNode(String responseList) {
        //解析出知识图谱节点
        List<Side> relations = parseRelationsFromJson(responseList);
        log.info("成功解析 {} 个任务依赖关系", relations.size());

        //获取对应的节点信息
        for (Side side : relations) {

            String fromNodeName = side.getFrom();
            String toNodeName = side.getTo();

            // 正确方向：前置 -> 后续
            KnowledgePointNode preNode = POINTNAME_TO_NODE_MAP.get(fromNodeName);
            KnowledgePointNode postNode = POINTNAME_TO_NODE_MAP.get(toNodeName);

            // 后续节点添加前置节点
            List<KnowledgePointNode> preNodes = KNOWLEDGE_POINT_NODE_MAP.get(postNode);

            preNodes.add(preNode);
            KNOWLEDGE_POINT_NODE_MAP.put(postNode, preNodes);

            log.info("知识图谱添加节点:后续节点[{}]成功添加前置节点[{}]", toNodeName, fromNodeName);

        }

        log.info("知识图谱·成功构建 [{}]", KNOWLEDGE_POINT_NODE_MAP);

    }

    /**
     * 智能推荐算法
     */
    private List<String> recommondKonledgePoint() {

        return null;
    }

    /**
     * 构建知识图谱实现系统提示词
     */
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
                ### 必须依赖（强制先学）
                满足以下条件时建立依赖：
                1. **核心概念依赖**：知识点B的理解绝对需要知识点A的概念基础
                2. **直接逻辑依赖**：知识点B是知识点A的直接延伸或深化
                3. **技能前置依赖**：掌握知识点B必须先具备知识点A的操作技能
                
                **重要**：以下情况避免建立依赖关系：
                - 可以通过更基础概念支撑的知识点（如ArrayList可直接依赖数据类型）
                - 相对独立的工具性概念（如异常处理可只依赖控制结构）
                - 同类型的并列特性（如封装和继承应该并列而非串行）
                
                ### 不建立依赖的情况
                1. 同类型的并列知识点（如不同数据类型、不同算法）
                2. 独立的工具性知识点
                3. 可独立理解和应用的知识单元
                4. 仅在高级应用中才产生关联的知识点
                
                ===============================
                ## 层级平衡构建策略
                ### 强制平衡原则
                - **每层严格限制2-3个知识点**，绝不允许单层超过3个知识点
                - **避免过长串行链条**，超过4层的串行依赖需要拆分重构
                - **优先构建并行分支**，让更多知识点可以同时学习
                - **智能依赖分配**，将集中的依赖关系分散到不同层级
                
                ### 依赖关系优化策略
                1. **最小依赖原则**：只建立真正必要的依赖，避免过度依赖
                2. **并行分支设计**：将知识体系分为多个相对独立的学习分支
                3. **依赖前移策略**：让部分知识点依赖更基础的概念，而非中间层概念
                4. **层级重分配**：当发现某层知识点过多时，主动寻找可前移的知识点
                
                ### 具体实现规则
                - 数据结构类知识点(ArrayList, HashMap等)可直接依赖基础概念
                - 工具性知识点(异常处理等)可依赖控制结构而非面向对象
                - 独立特性(封装、继承)应分散到不同层级
                - 确保每个层级都有1-3个可并行学习的知识点
                
                ===============================
                ## 学习路径优化原则
                ### 认知负荷管理
                - 避免单个学习阶段包含过多新概念
                - 确保每个知识点都有充分的前置基础
                - 相似或易混淆的知识点应分层学习
                
                ### 学习动机维护
                - 早期层级应包含核心、实用的知识点
                - 避免过长的前置链条影响学习积极性
                - 确保每个层级都有相对完整的学习成果
                
                ===============================
                ## 输出格式要求
                严格返回如下JSON格式：
                {
                  "relations": [
                    "前置知识点:后续知识点",
                    ...
                  ]
                }
                
                ### 格式规范
                - 仅输出JSON内容，不添加任何解释性文字
                - 使用半角冒号":"连接前置与后续知识点
                - 知识点名称与输入完全一致，不得修改
                - 依赖关系应覆盖所有必要的强依赖
                
                ### 质量保证
                - 确保输出的图结构为DAG（无循环）
                - 依赖链条长度适中，避免过长的串行路径
                - 每个知识点的前置依赖数量控制在1-2个以内
                
                ===============================
                ## 学科适用性示例
                ### 编程基础（4层结构）
                - L1：基本语法 → L2：控制结构 → L3：函数设计 → L4：面向对象编程
                - L1：数据类型 → L2：数据结构 → L3：算法基础 → L4：复杂算法
                
                ### 数学基础（4层结构）
                - L1：数系概念 → L2：基本运算 → L3：方程求解 → L4：函数应用
                - L1：几何图形 → L2：几何性质 → L3：几何计算 → L4：几何证明
                
                ===============================
                ## 特别注意事项
                ### 层级平衡检查
                - 生成依赖关系后，心理模拟拓扑排序结果
                - 如果预期某层会有超过3个知识点，主动调整依赖策略
                - 优先让工具性、数据结构类知识点前移到更基础的层级
                - 确保面向对象特性(继承、封装、多态)适当分散
                
                ### 依赖关系决策优先级
                1. **层级平衡 > 完美依赖**：宁可稍微弱化依赖也要保证层级平衡
                2. **并行学习 > 串行学习**：优先设计可以同时学习的知识点组合
                3. **实用学习路径 > 理论完美路径**：确保学习者能够平稳推进
                
                请严格按照层级平衡原则分析输入的知识点，确保输出的依赖关系能够形成每层2-3个知识点的平衡结构。
                """;
        return KNOWLEDGE_GRAPH_PROMPT;
    }


}