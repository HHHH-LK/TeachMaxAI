package com.aiproject.smartcampus.service.impl;

import com.aiproject.smartcampus.commons.client.Result;
import com.aiproject.smartcampus.commons.redis.QuestionRedisCache;
import com.aiproject.smartcampus.mapper.*;
import com.aiproject.smartcampus.pojo.po.*;
import com.aiproject.smartcampus.pojo.vo.AwardVO;
import com.aiproject.smartcampus.service.FightingService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.formula.functions.T;
import org.aspectj.weaver.patterns.TypePatternQuestions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

import static com.aiproject.smartcampus.service.impl.GameUserServiceImpl.calculateGameUserHP;

/**
 * @program: TeacherMaxAI
 * @description:
 * @author: lk_hhh
 * @create: 2025-08-12 16:15
 **/

@Slf4j
@Service
@RequiredArgsConstructor
public class FightingServiceImpl implements FightingService {

    private final FightingMapper fightingMapper;
    private final BossMapper bossMapper;
    private final QuestionBankMapper questionBankMapper;
    private final ChatLanguageModel chatLanguageModel;
    private final QuestionRedisCache questionRedisCache;
    private final TowerChallengeLogMapper towerChallengeLogMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final GameUserMapper gameUserMapper;
    private final StudentAnswerMapper studentAnswerMapper;
    private final TowerFloorMapper towerFloorMapper;
    private final TaskMapper taskMapper;
    private final ItemMapper itemMapper;


    @Override
    public Result<BattleLog> getCurrentBattleLog(String floorId, String towerChallengeLogId, String studentId) {

        LambdaQueryWrapper<BattleLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BattleLog::getFloorId, floorId);
        wrapper.eq(BattleLog::getUserId, studentId);
        wrapper.eq(BattleLog::getTowerChallengeLogId, towerChallengeLogId);
        wrapper.orderByDesc(BattleLog::getId);
        wrapper.last("limit 1");

        BattleLog battleLog = fightingMapper.selectOne(wrapper);

        if (battleLog == null) {
            log.warn("玩家还开始爬塔");
            throw new RuntimeException("玩家还开始爬塔");
        }

        return Result.success(battleLog);
    }

    @Override
    public Result<Long> startFighting(String floorId, String studentId) {
        //查询用户第几次闯该层了
        LambdaQueryWrapper<TowerChallengeLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TowerChallengeLog::getFloorId, floorId);
        wrapper.eq(TowerChallengeLog::getUserId, studentId);
        TowerChallengeLog towerChallengeLog = towerChallengeLogMapper.selectOne(wrapper);

        //设置闯关次数
        Integer challengeCount = towerChallengeLog.getChallengeCount();
        Integer currentChallengeCount = (challengeCount == null || challengeCount <= 0) ? 1 : (challengeCount + 1);

        //加入DB中
        TowerChallengeLog towerChallengeLogToDB = new TowerChallengeLog();
        towerChallengeLogToDB.setChallengeCount(currentChallengeCount);
        towerChallengeLogToDB.setStatus("进行中");
        towerChallengeLogToDB.setFloorId(Long.valueOf(floorId));
        towerChallengeLogToDB.setUserId(Long.valueOf(studentId));
        towerChallengeLogToDB.setLastChallengeTime(LocalDateTime.now());
        int insert = towerChallengeLogMapper.insert(towerChallengeLogToDB);
        if (insert <= 0) {
            log.error("系统错误，开始闯关失败");
            throw new RuntimeException("系统错误，开始闯关失败");
        }

        //查询boss血量和玩家血量并存入redis中
        LambdaQueryWrapper<Monster> monsterWrapper = new LambdaQueryWrapper<>();
        monsterWrapper.eq(Monster::getFloorId, floorId);
        Monster monster = bossMapper.selectOne(monsterWrapper);
        if (monster == null) {
            log.error("怪物不存在");
            throw new RuntimeException("怪物不存在");
        }
        LambdaQueryWrapper<GameUser> gameUserWrapper = new LambdaQueryWrapper<>();
        gameUserWrapper.eq(GameUser::getUserId, studentId);
        GameUser gameUser = gameUserMapper.selectOne(gameUserWrapper);
        Integer GAME_USER_HP = calculateGameUserHP(gameUser.getLevel());
        Integer BOSS_HP = monster.getHp();

        // 设定Redis存用户血量的Key
        String SET_USER_HP_KEY = String.format("game:user:hp:%d:%d:%d", studentId, floorId, currentChallengeCount);
        // 设定Redis存Boss血量的Key
        String SET_BOSS_HP_KEY = String.format("game:boss:hp:%d:%d:%d", floorId, studentId, currentChallengeCount);

        log.info("开始设置当前boss血量以及用户血量的值到redis中");
        stringRedisTemplate.opsForValue().set(SET_BOSS_HP_KEY, BOSS_HP.toString());
        stringRedisTemplate.opsForValue().set(SET_USER_HP_KEY, GAME_USER_HP.toString());

        //返回当前挑战id
        return Result.success(towerChallengeLogToDB.getId());

    }

    @Override
    public Result<Map<String, String>> getgetGameUserHPandbossHP(String studentId, String towerChallengeLogId) {

        //用于映射用户与boss的血量
        Map<String, String> USER_BOSS_HP_MAP = new HashMap<>(2);

        LambdaQueryWrapper<TowerChallengeLog> towerChallengeLogLambdaQueryWrapper = new LambdaQueryWrapper<>();
        towerChallengeLogLambdaQueryWrapper.eq(TowerChallengeLog::getUserId, studentId);
        towerChallengeLogLambdaQueryWrapper.eq(TowerChallengeLog::getId, towerChallengeLogId);
        TowerChallengeLog towerChallengeLog = towerChallengeLogMapper.selectOne(towerChallengeLogLambdaQueryWrapper);
        Long floorId = towerChallengeLog.getFloorId();

        //设置闯关次数
        Integer currentChallengeCount = towerChallengeLog.getChallengeCount();

        //从redis中获取用户与boss的血量
        String SET_USER_HP_KEY = String.format("game:user:hp:%d:%d:%d", studentId, floorId, currentChallengeCount);
        String SET_BOSS_HP_KEY = String.format("game:boss:hp:%d:%d:%d", floorId, studentId, currentChallengeCount);
        String BOSS_HP = stringRedisTemplate.opsForValue().get(SET_BOSS_HP_KEY);
        String GAME_USER_HP = stringRedisTemplate.opsForValue().get(SET_USER_HP_KEY);

        USER_BOSS_HP_MAP.put("GAME_USER_HP", GAME_USER_HP);
        USER_BOSS_HP_MAP.put("BOSS_HP", BOSS_HP);

        return Result.success(USER_BOSS_HP_MAP);

    }

    @Override
    public Result<QuestionBank> userAttack(String studentId, String towerChallengeLogId) {

        LambdaQueryWrapper<TowerChallengeLog> towerChallengeLogLambdaQueryWrapper = new LambdaQueryWrapper<>();
        towerChallengeLogLambdaQueryWrapper.eq(TowerChallengeLog::getId, towerChallengeLogId);
        TowerChallengeLog towerChallengeLog = towerChallengeLogMapper.selectOne(towerChallengeLogLambdaQueryWrapper);
        Long floorId = towerChallengeLog.getFloorId();

        //获取问题id（根据难度进行随机抽取）
        List<String> questionDifficultyListInCacheAndNotPushInCache = questionRedisCache.getQuestionDifficultyListInCacheAndNotPushInCache(Math.toIntExact(floorId));
        LambdaQueryWrapper<QuestionBank> questionBankLambdaQueryWrapper = new LambdaQueryWrapper<>();
        questionBankLambdaQueryWrapper.in(QuestionBank::getQuestionId, questionDifficultyListInCacheAndNotPushInCache);
        List<QuestionBank> questionBanks = questionBankMapper.selectList(questionBankLambdaQueryWrapper);

        //获取难度列表
        List<String> questionDifficultyListInCache = questionRedisCache.getTotalQuestionDifficultyListInCache(Math.toIntExact(floorId));
        String diff = questionDifficultyListInCache.getFirst();

        //进行抽取
        QuestionBank randomQuestion = getRandomQuestion(questionBanks, diff);
        log.info("抽取到的题目为[{}]", randomQuestion);

        return Result.success(randomQuestion);
    }

    public QuestionBank getRandomQuestion(List<QuestionBank> questionBanks, String diff) {
        // 按难度分类
        List<QuestionBank> easyList = questionBanks.stream()
                .filter(q -> q.getDifficultyLevel() == QuestionBank.DifficultyLevel.EASY)
                .toList();

        List<QuestionBank> mediumList = questionBanks.stream()
                .filter(q -> q.getDifficultyLevel() == QuestionBank.DifficultyLevel.MEDIUM)
                .toList();

        List<QuestionBank> hardList = questionBanks.stream()
                .filter(q -> q.getDifficultyLevel() == QuestionBank.DifficultyLevel.HARD)
                .toList();

        switch (diff) {
            case "0": // 简单题
                return getRandomOne(easyList);

            case "1": // 中等题
                return getRandomOne(mediumList);

            case "2": // 困难题
                return getRandomOne(hardList);

            default:
                throw new IllegalArgumentException("未知难度类型: " + diff);
        }
    }

    // 随机抽取工具方法
    private <T> T getRandomOne(List<T> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        Random random = new Random();
        return list.get(random.nextInt(list.size()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Map<String, String>> checkAswerIsTure(String studentId, String questionId, String context, String floorId, String towerChallengeCount) {
        //key 表示扣血的对象 value 表示扣血的血量
        HashMap<String, String> DE_HP_MAP = new HashMap<>(2);
        try {
            LambdaQueryWrapper<QuestionBank> questionBankLambdaQueryWrapper = new LambdaQueryWrapper<>();
            questionBankLambdaQueryWrapper.eq(QuestionBank::getQuestionId, questionId);
            QuestionBank questionBank = questionBankMapper.selectOne(questionBankLambdaQueryWrapper);

            //获取题目内容
            String questionContent = questionBank.getQuestionContent();
            String questionOptions = questionBank.getQuestionOptions();
            String correctAnswer = questionBank.getCorrectAnswer();

            //构建提示词
            String JUDGE_PROMPT = buildJudgePrompt(questionContent, questionOptions, correctAnswer, context);
            log.info("开始进行判题...");
            String judgeResult = chatLanguageModel.chat(UserMessage.userMessage(JUDGE_PROMPT)).aiMessage().text();
            String changeJudgeResult = "正确".equalsIgnoreCase(judgeResult.trim()) ? "1" : "0";

            log.info("评判结果是[{}],解析后的结果是[{}]", judgeResult, changeJudgeResult);

            //进行扣减血量操作
            String difficult = questionBank.getDifficultyLevel().getValue();
            int damage = calculateDamageByDifficult(floorId, difficult, changeJudgeResult);

            // Redis key
            String userHpKey = String.format("game:user:hp:%d:%d:%d", studentId, floorId, towerChallengeCount);
            String bossHpKey = String.format("game:boss:hp:%d:%d:%d", floorId, studentId, towerChallengeCount);

            if ("0".equals(changeJudgeResult)) {
                // 答错 - 扣玩家血
                DE_HP_MAP.put("USER_HP", bossHpKey);
                stringRedisTemplate.opsForValue().decrement(userHpKey, damage);
                //重新加入题目列表中
                questionRedisCache.setQuestionIdsInCache(Integer.valueOf(floorId), questionId);
            } else if ("1".equals(changeJudgeResult)) {
                // 答对 - 扣 Boss 血
                DE_HP_MAP.put("BOSS_HP", bossHpKey);
                stringRedisTemplate.opsForValue().decrement(bossHpKey, damage);
            } else {
                throw new RuntimeException("不存在的状态");
            }

            //将用户答案进行入库处理
            StudentAnswer studentAnswer = new StudentAnswer();
            studentAnswer.setQuestionId(Integer.valueOf(questionId));
            studentAnswer.setStudentId(Integer.valueOf(studentId));
            studentAnswer.setIsCorrect(String.valueOf(changeJudgeResult));
            studentAnswer.setStudentAnswer(context);
            int insert = studentAnswerMapper.insert(studentAnswer);
            if (insert <= 0) {
                log.error("用户回答入库失败");
                throw new RuntimeException("用户答题失败");
            }
            return Result.success(DE_HP_MAP);
        } catch (Exception e) {
            log.error("用户判题异常");
            throw new RuntimeException(e.getMessage());
        }
    }

    @Override
    public Result<Integer> getUserChangeCount(String studentId, String floorId) {

        //查询用户第几次闯该层了
        LambdaQueryWrapper<TowerChallengeLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TowerChallengeLog::getFloorId, floorId);
        wrapper.eq(TowerChallengeLog::getUserId, studentId);
        TowerChallengeLog towerChallengeLog = towerChallengeLogMapper.selectOne(wrapper);

        //设置闯关次数
        Integer currentChallengeCount = towerChallengeLog.getChallengeCount();

        return Result.success(currentChallengeCount);
    }

    @Override
    public Result<Integer> getDamage(String questionId, String floorId) {

        Integer damage = calculateDamageByDifficult(floorId, questionId, "1");

        return Result.success(damage);

    }

    @Override
    public Result<Integer> getResult(String floorId, String studentId, String towerChallengeLogId) {

        //查询用户第几次闯该层了
        LambdaQueryWrapper<TowerChallengeLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TowerChallengeLog::getFloorId, floorId);
        wrapper.eq(TowerChallengeLog::getUserId, studentId);
        TowerChallengeLog towerChallengeLog = towerChallengeLogMapper.selectOne(wrapper);
        //设置闯关次数
        Integer currentChallengeCount = towerChallengeLog.getChallengeCount();
        //key
        String SET_USER_HP_KEY = String.format("game:user:hp:%d:%d:%d", studentId, floorId, currentChallengeCount);
        String SET_BOSS_HP_KEY = String.format("game:boss:hp:%d:%d:%d", floorId, studentId, currentChallengeCount);
        //获取用户血量和boss血量
        String USER_HP = stringRedisTemplate.opsForValue().get(SET_USER_HP_KEY);
        String BOSS_HP = stringRedisTemplate.opsForValue().get(SET_BOSS_HP_KEY);

        //设置本次挑战的状态
        LambdaUpdateWrapper<TowerChallengeLog> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(TowerChallengeLog::getId, towerChallengeLogId);
        //根据血量来进行判断
        if (Integer.valueOf(USER_HP) <= 0) {
            updateWrapper.set(TowerChallengeLog::getStatus, "失败");
            towerChallengeLogMapper.update(null, updateWrapper);
            return Result.success(0);
        } else if (Integer.valueOf(BOSS_HP) <= 0) {
            updateWrapper.set(TowerChallengeLog::getStatus, "成功");
            towerChallengeLogMapper.update(null, updateWrapper);
            //对应成功业务的处理（解锁下一层，设置成功状态）
            LambdaUpdateWrapper<TowerFloor> towerFloorUpdateWrapper = new LambdaUpdateWrapper<>();
            towerFloorUpdateWrapper.eq(TowerFloor::getFloorId, floorId);
            towerFloorUpdateWrapper.set(TowerFloor::getIsPass, 1);
            int update = towerFloorMapper.update(towerFloorUpdateWrapper);
            if (update <= 0) {
                log.error("设置通过状态失效");
                throw new RuntimeException("设置通过状态失效");
            }
            return Result.success(1);

        } else {
            //中间态 表示没有成功也没有失败
            return Result.success(3);
        }
    }

    @Override
    public Result<AwardVO> getAward(String studentId, String floorId) {

        //增加经验值 增加等级 计算道具获取 返回获取到的道具

        LambdaQueryWrapper<Task> taskQueryWrapper = new LambdaQueryWrapper<>();
        taskQueryWrapper.eq(Task::getFloorId, floorId);
        Task task = taskMapper.selectOne(taskQueryWrapper);

        //获取当前任务奖励信息
        Integer rewardExp = task.getRewardExp();
        Integer rewardItemQty = task.getRewardItemQty();
        Integer rewardItemRarity = task.getRewardItemRarity();

        LambdaQueryWrapper<Item> itemQueryWrapper = new LambdaQueryWrapper<>();
        itemQueryWrapper.eq(Item::getRarity, rewardItemRarity);
        List<Item> items = itemMapper.selectList(itemQueryWrapper);

        //获取概率并进行抽奖
        double probability = rewardItemRarity == 0 ? 0.7 : (rewardItemQty == 1 ? 0.3 : 0.1);
        List<Item> randomItems = new ArrayList<>();
        if (items != null && items.size() >= rewardItemQty) {
            // 先判断概率
            if (ThreadLocalRandom.current().nextDouble() < probability) {
                // 命中概率 -> 抽奖
                Collections.shuffle(items); // 打乱顺序
                randomItems = items.subList(0, rewardItemQty);
                log.info("抽取到的物品: {}", randomItems);
            } else {
                log.info("很遗憾，这次没有抽到任何奖励");
            }
        }

        //进行经验的处理
        LambdaQueryWrapper<GameUser> gameUserQueryWrapper = new LambdaQueryWrapper<>();
        gameUserQueryWrapper.eq(GameUser::getUserId, studentId);
        GameUser gameUser = gameUserMapper.selectOne(gameUserQueryWrapper);
        Integer level = gameUser.getLevel();
        //计算升级需要的经验值
        long requireExp = calculateExp(level);
        //获取当前的经验值
        Integer currentExp = gameUser.getExp();
        //进行实现升级扣减经验值业务
        long totalExp = rewardExp + currentExp;
        LambdaUpdateWrapper<GameUser> gameUserUpdateWrapper = new LambdaUpdateWrapper<>();
        gameUserUpdateWrapper.eq(GameUser::getUserId, studentId);
        int update = 0;
        if (totalExp <= requireExp) {
            gameUserUpdateWrapper.set(GameUser::getExp, totalExp);
        } else {
            long afterDecressExp = totalExp - requireExp;
            gameUserUpdateWrapper.set(GameUser::getExp, afterDecressExp);
        }
        update = gameUserMapper.update(gameUserUpdateWrapper);
        //判断是否修改值成功
        if (update <= 0) {
            log.error("修改玩家经验值失败");
            throw new RuntimeException("修改玩家经验值失败");
        }

        AwardVO awardVO = new AwardVO();
        awardVO.setExp(String.valueOf(rewardExp));
        awardVO.setItem(randomItems);
        log.info("用户闯关所获的奖励为: {}", awardVO);

        return Result.success(awardVO);

    }

    /**
     * 计算用户升级需要的经验
     *
     * @param level 当前等级
     * @return 升级到下一级所需经验
     */
    private long calculateExp(int level) {
        // 基础经验
        final int baseExp = 100;
        // 增长系数（整体倍数）
        final double growthRate = 1.5;
        // 等级指数影响（可调节难度）
        final double exponent = 2.0;
        // 计算公式：基础经验 × (等级 ^ 指数) × 增长系数
        double exp = baseExp * Math.pow(level, exponent) * growthRate;
        return Math.round(exp);
    }


    /**
     * 计算技能伤害
     * 难度：EASY、MEDIUM、HARD
     * 正确 → 扣 Boss 血
     * 错误 → 扣 玩家血
     */
    private Integer calculateDamageByDifficult(String floorId, String difficult, String isCorrect) {
        TowerFloor towerFloor = towerFloorMapper.selectOne(
                new LambdaQueryWrapper<TowerFloor>().eq(TowerFloor::getFloorId, floorId)
        );
        Integer floorNo = towerFloor.getFloorNo();

        // 基础伤害随层数增加
        double baseDamage = 30 + floorNo * 5;

        // 难度系数
        double difficultFactor;
        switch (difficult.toUpperCase()) {
            case "HARD":
                difficultFactor = 2.0;
                break;
            case "MEDIUM":
                difficultFactor = 1.5;
                break;
            case "EASY":
            default:
                difficultFactor = 1.0;
                break;
        }

        // 正确与错误的系数
        boolean correct = "1".equals(isCorrect) || "正确".equalsIgnoreCase(isCorrect);
        double resultFactor = correct ? 1.0 : 0.5; // 答错扣玩家血量稍微轻一点

        // 最终伤害
        return (int) Math.round(baseDamage * difficultFactor * resultFactor);
    }

    /**
     * 构建判题提示词
     */
    public String buildJudgePrompt(String questionContent, String questionOptions, String correctAnswer, String studentAnswer) {
        return String.format(
                "请帮我判定学生答案的正确性。\n\n" +
                        "题目：\n%s\n\n" +
                        "选项：\n%s\n\n" +
                        "正确答案：\n%s\n\n" +
                        "学生回答：\n%s\n\n" +
                        "请根据正确答案判断学生回答是否正确，回答时只返回“正确”或“错误”，并简要说明理由。",
                questionContent, questionOptions, correctAnswer, studentAnswer
        );
    }


}