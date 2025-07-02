package com.aiproject.smartcampus.service.impl;

import com.aiproject.smartcampus.commons.client.Result;
import com.aiproject.smartcampus.commons.utils.UserToTypeUtils;
import com.aiproject.smartcampus.exception.StudentExpection;
import com.aiproject.smartcampus.exception.UserExpection;
import com.aiproject.smartcampus.functioncalling.NotMasterTestCreateTool;
import com.aiproject.smartcampus.mapper.KnowledgePointMapper;
import com.aiproject.smartcampus.mapper.StudentKnowledgeMasteryMapper;
import com.aiproject.smartcampus.mapper.UserMapper;
import com.aiproject.smartcampus.pojo.bo.SimpleKnowledgeAnalysisBO;
import com.aiproject.smartcampus.pojo.bo.StudentWrongKnowledgeBO;
import com.aiproject.smartcampus.pojo.bo.TBO;
import com.aiproject.smartcampus.pojo.bo.TestTaskBO;
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
    private final String KnledgeRedisKey = "system:knoledge:imformation:";
    private final UserToTypeUtils getUserToTypeUtils;
    private final Duration duration = Duration.ofHours(3);

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
    public Result createListTestByagent(List<String> pointIds) {

        //String studentId = getUserToTypeUtils.change();
        //todo进行测试
        String studentId = "1";

        if (studentId == null) {
            log.error("该用户未登录[{}]", studentId);
            throw new UserExpection("用户未登录");
        }

        List<SimpleKnowledgeAnalysisBO> simpleKnowledgeAnalysisBOList = knowledgePointMapper.getSimpleKnowledgeAnalysis(pointIds, studentId);
        //进行权值算法抽取
        List<SimpleKnowledgeAnalysisBO> agentCreatepoint = randomChancepoint(simpleKnowledgeAnalysisBOList);

        //进行智能生成
        notMasterTestCreateTool.setTestTaskBO(new TestTaskBO(Integer.valueOf(studentId), null));
        notMasterTestCreateTool.setSimpleKnowledgeAnalysisBOList(agentCreatepoint);
        notMasterTestCreateTool.run();
        String result = notMasterTestCreateTool.getResult();

        return Result.success(result);
    }

    // TODO: 后续改成策略模式 - 修复死循环版本（利用对数压缩进行修改处理）
    private List<SimpleKnowledgeAnalysisBO> randomChancepoint(List<SimpleKnowledgeAnalysisBO> simpleKnowledgeAnalysisBOList) {
        int pointNum = 10;
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
}