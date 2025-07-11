package com.aiproject.smartcampus.service.impl;

import com.aiproject.smartcampus.commons.client.Result;
import com.aiproject.smartcampus.commons.utils.UserToTypeUtils;
import com.aiproject.smartcampus.exception.StudentExpection;
import com.aiproject.smartcampus.exception.UserExpection;
import com.aiproject.smartcampus.model.functioncalling.NotMasterTestCreateTool;
import com.aiproject.smartcampus.mapper.KnowledgePointMapper;
import com.aiproject.smartcampus.mapper.StudentKnowledgeMasteryMapper;
import com.aiproject.smartcampus.mapper.UserMapper;
import com.aiproject.smartcampus.pojo.bo.SimpleKnowledgeAnalysisBO;
import com.aiproject.smartcampus.pojo.bo.StudentWrongKnowledgeBO;
import com.aiproject.smartcampus.pojo.bo.TBO;
import com.aiproject.smartcampus.pojo.bo.TestTaskBO;
import com.aiproject.smartcampus.pojo.dto.HavingTPointDTO;
import com.aiproject.smartcampus.pojo.vo.KnowledgePointSimpleVO;
import com.aiproject.smartcampus.pojo.vo.StudentWrongKnowledgeVO;
import com.aiproject.smartcampus.service.KnoledgeService;
import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @program: SmartCampus
 * @description:
 * @author: lk_hhh
 * @create: 2025-07-02 18:23
 **/

@Slf4j
@RequiredArgsConstructor
@Service
public class KnoledgeServiceImpl implements KnoledgeService {

    private final UserToTypeUtils userToTypeUtils;
    private final KnowledgePointMapper knowledgePointMapper;
    private final UserMapper userMapper;
    private final RedisTemplate<String, KnowledgePointSimpleVO> knowledgePointSimpleVORedisTemplate;
    private final NotMasterTestCreateTool notMasterTestCreateTool;
    private final StudentKnowledgeMasteryMapper studentKnowledgeMasteryMapper;
    private final ChatLanguageModel chatLanguageModel;
    private final UserToTypeUtils getUserToTypeUtils;

    private final Duration duration = Duration.ofHours(3);
    private final String KnledgeRedisKey = "system:knoledge:imformation:";
    private final Integer pointNum = 10;

    @Override
    public Result getALlOKKnowlegePoint() {

        //String studentId = getUserToTypeUtils.change();
        //todo进行测试
        String studentId = "1";

        if (studentId == null) {
            log.error("该用户未登录[{}]", studentId);
            throw new UserExpection("用户未登录");
        }

        //查询数据
        List<StudentWrongKnowledgeBO> studentWrongKnowledgeByStudentId = knowledgePointMapper.getStudentWrongKnowledgeByStudentId(studentId);

        if (studentWrongKnowledgeByStudentId.isEmpty()) {
            log.warn("用户没有错误的知识点");
            return Result.success(new ArrayList<>());
        }

        //对数据进行处理
        List<StudentWrongKnowledgeVO> list = studentWrongKnowledgeByStudentId.stream()
                .distinct()
                //通过率从低到高进行排序
                .sorted(Comparator.comparingDouble(StudentWrongKnowledgeBO::getAccuracyRate))
                .map(a -> {
                    StudentWrongKnowledgeVO vo = new StudentWrongKnowledgeVO();
                    BeanUtils.copyProperties(a, vo);
                    return vo;
                }).toList();

        return Result.success(list);
    }

    @Override
    public Result getgetKnowlegeInformationBypointId(String pointId) {

        //String studentId = getUserToTypeUtils.change();
        //todo进行测试
        String studentId = "1";

        if (studentId == null) {
            log.error("该用户未登录[{}]", studentId);
            throw new UserExpection("用户未登录");
        }

        //构建索引
        String key = KnledgeRedisKey + studentId + "_" + pointId;

        //查询缓存
        KnowledgePointSimpleVO knowledgePointSimpleVO = knowledgePointSimpleVORedisTemplate.opsForValue().get(key);
        if (knowledgePointSimpleVO != null) {
            return Result.success(knowledgePointSimpleVO);
        }

        KnowledgePointSimpleVO knowledgeInformationByPointId = knowledgePointMapper.getKnowledgeInformationByPointId(pointId, studentId);

        if (knowledgeInformationByPointId == null) {
            log.error("【{}】学生该【{}】知识点不存在", studentId, pointId);
            throw new StudentExpection("知识点不存在");
        }

        //更新缓存
        knowledgePointSimpleVORedisTemplate.opsForValue().set(key, knowledgeInformationByPointId);
        knowledgePointSimpleVORedisTemplate.expire(key, duration);

        return Result.success(knowledgeInformationByPointId);
    }

    @Override
    public Result<String> createListTestByagent(List<String> pointIds,String courseId,String chapter,String content) {

        /*String studentId = getUserToTypeUtils.change();*/

        String studentId = "1";

        if (studentId == null) {
            log.error("该用户未登录[{}]", studentId);
            throw new UserExpection("用户未登录");
        }

        List<SimpleKnowledgeAnalysisBO> simpleKnowledgeAnalysisBOList = knowledgePointMapper.getSimpleKnowledgeAnalysis(pointIds, studentId);
        //进行权值算法抽取
        List<SimpleKnowledgeAnalysisBO> agentCreatepoint = randomChancepoint(simpleKnowledgeAnalysisBOList);

        //进行智能生成
        notMasterTestCreateTool.setTestTaskBO(new TestTaskBO(Integer.valueOf(studentId), Integer.valueOf(courseId),Integer.valueOf(chapter),content));
        notMasterTestCreateTool.setSimpleKnowledgeAnalysisBOList(agentCreatepoint);
        notMasterTestCreateTool.run();
        String result = notMasterTestCreateTool.getResult();

        return Result.success(result);
    }

    @Override
    public Result<String> createListTestUsingTByAgent(List<HavingTPointDTO> pointIds,String courseId,String chapterId,String content) {

        Integer defaultT=3;

        String studentId = getUserToTypeUtils.change();

        if (studentId == null) {
            log.error("该用户未登录[{}]", studentId);
            throw new UserExpection("用户未登录");
        }

        if (pointIds == null || pointIds.isEmpty()) {
            log.error("用户自定义权重列表为空");
            throw new IllegalArgumentException("权重列表不能为空");
        }

        try {
            log.info("开始基于用户自定义权重为学生[{}]生成测试题，原始权重数据：{}", studentId, pointIds);

            //获取相关权重信息，设置默认权重
            List<HavingTPointDTO> updatTPointList = pointIds.stream().map(s -> {
                Integer t = s.getT();
                t = t == null ? defaultT : t; // 默认权重为3
                s.setT(t);
                return s;
            }).toList();

            log.info("权重处理后的数据：{}", updatTPointList);

            //自动进行抽取
            List<SimpleKnowledgeAnalysisBO> selectedKnowledgePoints = randomPointByT(updatTPointList);

            if (selectedKnowledgePoints.isEmpty()) {
                log.warn("未能选择到任何知识点");
                return Result.error("未能选择到有效的知识点");
            }

            // 记录选择的知识点
            List<String> selectedNames = selectedKnowledgePoints.stream()
                    .map(SimpleKnowledgeAnalysisBO::getPointName)
                    .collect(Collectors.toList());
            log.info("基于用户权重选择的知识点：{}", selectedNames);

            // 进行智能生成
            notMasterTestCreateTool.setTestTaskBO(new TestTaskBO(Integer.valueOf(studentId), Integer.valueOf(courseId),Integer.valueOf(chapterId),content));
            notMasterTestCreateTool.setSimpleKnowledgeAnalysisBOList(selectedKnowledgePoints);
            notMasterTestCreateTool.run();
            String result = notMasterTestCreateTool.getResult();

            log.info("基于用户自定义权重的测试题生成成功");
            return Result.success(result);

        } catch (Exception e) {
            log.error("基于用户自定义权重生成测试题失败", e);
            return Result.error("生成测试题失败: " + e.getMessage());
        }
    }

    // TODO:智能进行获取错误知识点信息 后续改成策略模式 - 修复死循环版本（利用对数压缩进行修改处理）
    private List<SimpleKnowledgeAnalysisBO> randomChancepoint(List<SimpleKnowledgeAnalysisBO> simpleKnowledgeAnalysisBOList) {

        double trueParam = 2.0;
        double falseParam = 0.5;
        double totalWeight = 0.0;
        double step = 0.0;

        // 死循环保护变量
        long startTime = System.currentTimeMillis();
        int maxAttempts = 10000;
        long timeoutMs = 10000;
        int attemptCount = 0;

        Map<TBO, Integer> rangeMap = new ConcurrentHashMap<>();
        List<SimpleKnowledgeAnalysisBO> resultList = new ArrayList<>();

        if (simpleKnowledgeAnalysisBOList.size() <= pointNum) {
            log.info("输入知识点数量[{}]不超过目标数量[{}]，返回全部", simpleKnowledgeAnalysisBOList.size(), pointNum);
            return simpleKnowledgeAnalysisBOList;
        }

        // 1. 构建权重区间表（使用对数压缩）
        for (int i = 0; i < simpleKnowledgeAnalysisBOList.size(); i++) {
            SimpleKnowledgeAnalysisBO item = simpleKnowledgeAnalysisBOList.get(i);
            Integer correct = item.getCorrectCount() != null ? item.getCorrectCount() : 0;
            Integer wrong = item.getWrongCount() != null ? item.getWrongCount() : 0;
            Double acc = item.getAccuracyRate();

            // 处理正确率参数
            if (acc == null || acc <= 0) {
                acc = 0.01; // 最小正确率1%
            }

            // 如果acc是百分比(>1)，转换为小数(0-1)
            if (acc > 1.0) {
                acc = acc / 100.0;
            }

            // 确保acc在合理范围内[0.01, 1.0]
            acc = Math.max(0.01, Math.min(1.0, acc));

            double wrongRate = 1.0 - acc;

            // 使用原始公式计算，但加数值保护
            double weight1 = correct * Math.pow(falseParam, wrongRate);
            double weight2 = wrong * Math.pow(trueParam, 1.0 / acc);

            // 检查数值异常，防止无穷大或NaN
            if (Double.isInfinite(weight1) || Double.isNaN(weight1)) {
                weight1 = correct * 2.0; // 降级为线性处理
                log.warn("知识点[{}]的weight1出现数值异常，降级处理", item.getPointName());
            }
            if (Double.isInfinite(weight2) || Double.isNaN(weight2)) {
                weight2 = wrong * 10.0; // 降级为线性处理
                log.warn("知识点[{}]的weight2出现数值异常，降级处理", item.getPointName());
            }

            // 限制单个权重的最大值，防止过度膨胀
            weight1 = Math.min(weight1, 1e10);
            weight2 = Math.min(weight2, 1e10);

            double originalWeight = weight1 + weight2;

            //对数压缩：使用log1p(x) = ln(1+x)
            double compressedWeight = Math.log1p(originalWeight) * 5.0; // 乘以5调整到合适的权重范围

            // 确保最小权重，避免权重为0
            compressedWeight = Math.max(0.1, compressedWeight);

            // 构建权重区间
            TBO range = new TBO(step, step + compressedWeight);
            rangeMap.put(range, i);
            step += compressedWeight;
            totalWeight += compressedWeight;

            log.debug("知识点[{}] - 正确:{}, 错误:{}, 正确率:{:.2f}%, 原始权重:{:.2e}, 压缩权重:{:.4f}, 区间:[{:.4f}, {:.4f}]",
                    item.getPointName(), correct, wrong, acc * 100, originalWeight, compressedWeight, range.getMin(), range.getMax());
        }

        log.info("构建知识点权重区间成功，总权重：{:.4f}，区间表大小：{}", totalWeight, rangeMap.size());

        // 2. 加权随机抽样（原有逻辑 + 保护机制）
        Set<Integer> selectedIndexes = new HashSet<>();
        Random random = ThreadLocalRandom.current();

        while (resultList.size() < pointNum &&
                attemptCount < maxAttempts &&
                (System.currentTimeMillis() - startTime) < timeoutMs) {

            attemptCount++;
            double rnd = random.nextDouble() * totalWeight;
            boolean foundInThisRound = false;

            for (Map.Entry<TBO, Integer> entry : rangeMap.entrySet()) {
                TBO range = entry.getKey();
                int index = entry.getValue();
                if (rnd >= range.getMin() && rnd < range.getMax() && !selectedIndexes.contains(index)) {
                    resultList.add(simpleKnowledgeAnalysisBOList.get(index));
                    selectedIndexes.add(index);
                    foundInThisRound = true;

                    log.debug("选中知识点[{}] - 随机数:{:.4f}, 区间:[{:.4f}, {:.4f}], 第{}次尝试",
                            simpleKnowledgeAnalysisBOList.get(index).getPointName(),
                            rnd, range.getMin(), range.getMax(), attemptCount);
                    break;
                }
            }

            // 如果连续多次没找到，记录警告并增加调试信息
            if (!foundInThisRound && attemptCount % 1000 == 0) {
                log.warn("权重抽样进行中 - 尝试次数：{}，当前已选择：{}个，目标：{}个，随机数:{:.4f}，总权重:{:.4f}",
                        attemptCount, resultList.size(), pointNum, rnd, totalWeight);

                // 每5000次打印当前区间状态
                if (attemptCount % 5000 == 0) {
                    log.info("当前已选择的索引: {}", selectedIndexes);
                    log.info("剩余可选择区间数: {}", rangeMap.size() - selectedIndexes.size());
                }
            }
        }

        // 补充逻辑：如果未达到目标数量
        if (resultList.size() < pointNum) {
            log.warn("权重抽样未完成 - 原因：尝试次数[{}]，耗时[{}ms]，已选择[{}]个，目标[{}]个",
                    attemptCount, System.currentTimeMillis() - startTime, resultList.size(), pointNum);

            // 从未选择的元素中随机选择剩余数量
            List<SimpleKnowledgeAnalysisBO> unselectedItems = new ArrayList<>();
            for (int i = 0; i < simpleKnowledgeAnalysisBOList.size(); i++) {
                if (!selectedIndexes.contains(i)) {
                    unselectedItems.add(simpleKnowledgeAnalysisBOList.get(i));
                }
            }

            Collections.shuffle(unselectedItems, random);

            int needMore = pointNum - resultList.size();
            int canAdd = Math.min(needMore, unselectedItems.size());

            for (int i = 0; i < canAdd; i++) {
                resultList.add(unselectedItems.get(i));
                log.debug("补充选择知识点[{}]", unselectedItems.get(i).getPointName());
            }

            log.info("已补充{}个知识点，最终选择{}个知识点", canAdd, resultList.size());
        }

        log.info("完成加权随机抽取，共抽取 {} 个知识点，总尝试次数：{}", resultList.size(), attemptCount);
        return resultList;
    }

    //TODO ：用户手动定制错误信息生成权重，基于用户权重进行生成练习题
    private List<SimpleKnowledgeAnalysisBO> randomPointByT(List<HavingTPointDTO> havingTPointDTOList) {

        int pointNum = 10; // 目标抽取数量
        double totalWeight = 0.0;
        double step = 0.0;

        // 死循环保护变量
        long startTime = System.currentTimeMillis();
        int maxAttempts = 10000;
        long timeoutMs = 10000;
        int attemptCount = 0;

        Map<TBO, Integer> rangeMap = new ConcurrentHashMap<>();
        List<SimpleKnowledgeAnalysisBO> resultList = new ArrayList<>();

        // 参数校验
        if (havingTPointDTOList == null || havingTPointDTOList.isEmpty()) {
            log.warn("用户自定义权重列表为空");
            return resultList;
        }

        // 获取知识点ID列表，用于查询详细信息
        List<String> pointIds = havingTPointDTOList.stream()
                .map(dto -> String.valueOf(dto.getPointIds()))
                .collect(Collectors.toList());

        // 查询知识点详细信息
        List<SimpleKnowledgeAnalysisBO> allKnowledgeAnalysis;
        try {
            // TODO: 从上下文获取真实学生ID
            String studentId = "1";
            //String studentId = userToTypeUtils.change();
            allKnowledgeAnalysis = knowledgePointMapper.getSimpleKnowledgeAnalysis(pointIds, studentId);
        } catch (Exception e) {
            log.error("查询知识点分析数据失败", e);
            return resultList;
        }

        // 创建知识点ID到分析数据的映射
        Map<Integer, SimpleKnowledgeAnalysisBO> knowledgeMap = allKnowledgeAnalysis.stream()
                .collect(Collectors.toMap(
                        SimpleKnowledgeAnalysisBO::getPointId,
                        Function.identity(),
                        (existing, replacement) -> existing
                ));

        if (havingTPointDTOList.size() <= pointNum) {
            log.info("用户自定义权重数量[{}]不超过目标数量[{}]，返回全部", havingTPointDTOList.size(), pointNum);
            // 返回所有对应的知识点分析数据
            return havingTPointDTOList.stream()
                    .map(dto -> knowledgeMap.get(dto.getPointIds()))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        }

        // 1. 构建权重区间表（基于用户自定义权重T）
        for (int i = 0; i < havingTPointDTOList.size(); i++) {
            HavingTPointDTO dto = havingTPointDTOList.get(i);
            Integer pointId = dto.getPointIds();
            Integer userWeight = dto.getT();

            // 获取对应的知识点分析数据
            SimpleKnowledgeAnalysisBO analysisBO = knowledgeMap.get(pointId);
            if (analysisBO == null) {
                log.warn("知识点ID[{}]未找到对应的分析数据，跳过", pointId);
                continue;
            }

            // 处理用户权重
            if (userWeight == null || userWeight <= 0) {
                userWeight = 3; // 默认权重为3
                log.debug("知识点[{}]权重为空或负数，设置为默认权重3", pointId);
            }

            // 【关键】使用用户自定义权重，进行对数压缩处理
            double originalWeight = userWeight.doubleValue();

            // 对数压缩：保持相对关系但控制数值范围
            double compressedWeight = Math.log1p(originalWeight) * 3.0;
            compressedWeight = Math.max(0.1, compressedWeight);

            // 构建权重区间
            TBO range = new TBO(step, step + compressedWeight);
            rangeMap.put(range, i);
            step += compressedWeight;
            totalWeight += compressedWeight;

            log.debug("知识点[{}] - 用户权重:{}, 压缩权重:{:.4f}, 区间:[{:.4f}, {:.4f}]",
                    analysisBO.getPointName(), userWeight, compressedWeight, range.getMin(), range.getMax());
        }

        log.info("构建用户自定义权重区间成功，总权重：{}，区间表大小：{}", totalWeight, rangeMap.size());

        Set<Integer> selectedIndexes = new HashSet<>();
        Random random = ThreadLocalRandom.current();

        while (resultList.size() < pointNum &&
                attemptCount < maxAttempts &&
                (System.currentTimeMillis() - startTime) < timeoutMs) {

            attemptCount++;
            double rnd = random.nextDouble() * totalWeight;
            boolean foundInThisRound = false;

            for (Map.Entry<TBO, Integer> entry : rangeMap.entrySet()) {
                TBO range = entry.getKey();
                int index = entry.getValue();

                if (rnd >= range.getMin() && rnd < range.getMax() && !selectedIndexes.contains(index)) {
                    HavingTPointDTO selectedDto = havingTPointDTOList.get(index);
                    SimpleKnowledgeAnalysisBO selectedAnalysis = knowledgeMap.get(selectedDto.getPointIds());

                    if (selectedAnalysis != null) {
                        resultList.add(selectedAnalysis);
                        selectedIndexes.add(index);
                        foundInThisRound = true;

                        log.debug("选中知识点[{}] - 用户权重:{}, 随机数:{:.4f}, 区间:[{:.4f}, {:.4f}], 第{}次尝试",
                                selectedAnalysis.getPointName(), selectedDto.getT(),
                                rnd, range.getMin(), range.getMax(), attemptCount);
                    }
                    break;
                }
            }

            // 如果连续多次没找到，记录警告
            if (!foundInThisRound && attemptCount % 1000 == 0) {
                log.warn("用户权重抽样进行中 - 尝试次数：{}，当前已选择：{}个，目标：{}个，随机数:{:.4f}，总权重:{:.4f}",
                        attemptCount, resultList.size(), pointNum, rnd, totalWeight);

                if (attemptCount % 5000 == 0) {
                    log.info("当前已选择的索引: {}", selectedIndexes);
                    log.info("剩余可选择区间数: {}", rangeMap.size() - selectedIndexes.size());
                }
            }
        }

        //如果未达到目标数量
        if (resultList.size() < pointNum) {
            log.warn("用户权重抽样未完成 - 原因：尝试次数[{}]，耗时[{}ms]，已选择[{}]个，目标[{}]个",
                    attemptCount, System.currentTimeMillis() - startTime, resultList.size(), pointNum);

            // 从未选择的元素中随机选择剩余数量
            List<SimpleKnowledgeAnalysisBO> unselectedItems = new ArrayList<>();
            for (int i = 0; i < havingTPointDTOList.size(); i++) {
                if (!selectedIndexes.contains(i)) {
                    HavingTPointDTO unselectedDto = havingTPointDTOList.get(i);
                    SimpleKnowledgeAnalysisBO unselectedAnalysis = knowledgeMap.get(unselectedDto.getPointIds());
                    if (unselectedAnalysis != null) {
                        unselectedItems.add(unselectedAnalysis);
                    }
                }
            }

            Collections.shuffle(unselectedItems, random);

            int needMore = pointNum - resultList.size();
            int canAdd = Math.min(needMore, unselectedItems.size());

            for (int i = 0; i < canAdd; i++) {
                resultList.add(unselectedItems.get(i));
                log.debug("补充选择知识点[{}]", unselectedItems.get(i).getPointName());
            }

            log.info("已补充{}个知识点，最终选择{}个知识点", canAdd, resultList.size());
        }

        log.info("完成用户自定义权重抽取，共抽取 {} 个知识点，总尝试次数：{}", resultList.size(), attemptCount);
        return resultList;
    }


}

