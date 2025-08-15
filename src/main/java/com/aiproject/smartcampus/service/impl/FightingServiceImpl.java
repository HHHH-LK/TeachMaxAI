package com.aiproject.smartcampus.service.impl;

import com.aiproject.smartcampus.commons.client.Result;
import com.aiproject.smartcampus.commons.redis.QuestionRedisCache;
import com.aiproject.smartcampus.commons.redis.RedisSort;
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
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import static com.aiproject.smartcampus.service.impl.GameUserServiceImpl.calculateGameUserHP;

/**
 * @program: TeacherMaxAI
 * @description: 优化后的战斗服务实现 - 增强健壮性和性能
 * @author: lk_hhh
 * @create: 2025-08-15
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
    private final RedisSort redisSort;

    // 常量定义
    private static final String REDIS_USER_HP_KEY_TEMPLATE = "game:user:hp:%s:%s:%d";
    private static final String REDIS_BOSS_HP_KEY_TEMPLATE = "game:boss:hp:%s:%s:%d";
    private static final int REDIS_TTL_HOURS = 24; // Redis键过期时间
    private static final int DEFAULT_BASE_DAMAGE = 30;
    private static final int DAMAGE_PER_FLOOR = 5;

    /**
     * 战斗结果枚举
     */
    public enum BattleResult {
        PLAYER_WIN(1, "胜利"),
        PLAYER_LOSE(0, "失败"),
        CONTINUE(3, "继续");

        private final int code;
        private final String description;

        BattleResult(int code, String description) {
            this.code = code;
            this.description = description;
        }

        public int getCode() {
            return code;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 难度枚举
     */
    public enum Difficulty {
        EASY("0", "EASY", 1.0),
        MEDIUM("1", "MEDIUM", 1.5),
        HARD("2", "HARD", 2.0);

        private final String code;
        private final String name;
        private final double factor;

        Difficulty(String code, String name, double factor) {
            this.code = code;
            this.name = name;
            this.factor = factor;
        }

        public static Difficulty fromCode(String code) {
            for (Difficulty d : values()) {
                if (d.code.equals(code)) {
                    return d;
                }
            }
            throw new IllegalArgumentException("未知难度代码: " + code);
        }

        public static Difficulty fromName(String name) {
            for (Difficulty d : values()) {
                if (d.name.equalsIgnoreCase(name)) {
                    return d;
                }
            }
            throw new IllegalArgumentException("未知难度名称: " + name);
        }

        public String getCode() {
            return code;
        }

        public String getName() {
            return name;
        }

        public double getFactor() {
            return factor;
        }
    }

    @Override
    public Result<BattleLog> getCurrentBattleLog(String floorId, String towerChallengeLogId, String studentId) {
        if (!validateParams(floorId, towerChallengeLogId, studentId)) {
            return Result.error("参数不能为空");
        }

        try {
            LambdaQueryWrapper<BattleLog> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(BattleLog::getFloorId, floorId)
                    .eq(BattleLog::getUserId, studentId)
                    .eq(BattleLog::getTowerChallengeLogId, towerChallengeLogId)
                    .orderByDesc(BattleLog::getId)
                    .last("LIMIT 1");

            BattleLog battleLog = fightingMapper.selectOne(wrapper);

            if (battleLog == null) {
                log.warn("未找到战斗日志 - floorId: {}, studentId: {}, towerChallengeLogId: {}",
                        floorId, studentId, towerChallengeLogId);
                return Result.error("玩家尚未开始爬塔");
            }

            return Result.success(battleLog);

        } catch (Exception e) {
            log.error("获取战斗日志失败 - floorId: {}, studentId: {}", floorId, studentId, e);
            return Result.error("获取战斗日志失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Long> startFighting(String floorId, String studentId) {
        if (!validateParams(floorId, studentId)) {
            return Result.error("参数不能为空");
        }

        try {
            // 获取或创建挑战记录
            TowerChallengeLog existingLog = getTowerChallengeLog(floorId, studentId);
            int currentChallengeCount = (existingLog != null && existingLog.getChallengeCount() != null)
                    ? existingLog.getChallengeCount() + 1
                    : 1;

            // 创建新的挑战记录
            TowerChallengeLog newChallengeLog = createTowerChallengeLog(floorId, studentId, currentChallengeCount);

            // 初始化血量
            boolean hpInitialized = initializeBattleHP(floorId, studentId, currentChallengeCount);
            if (!hpInitialized) {
                throw new RuntimeException("初始化战斗血量失败");
            }

            log.info("成功开始战斗 - floorId: {}, studentId: {}, challengeCount: {}, logId: {}",
                    floorId, studentId, currentChallengeCount, newChallengeLog.getId());

            return Result.success(newChallengeLog.getId());

        } catch (Exception e) {
            log.error("开始战斗失败 - floorId: {}, studentId: {}", floorId, studentId, e);
            return Result.error("开始战斗失败: " + e.getMessage());
        }
    }

    @Override
    public Result<Map<String, String>> getgetGameUserHPandbossHP(String studentId, String towerChallengeLogId) {
        if (!validateParams(studentId, towerChallengeLogId)) {
            return Result.error("参数不能为空");
        }

        try {
            TowerChallengeLog challengeLog = getTowerChallengeLogById(towerChallengeLogId);
            if (challengeLog == null) {
                return Result.error("挑战记录不存在");
            }

            String floorId = String.valueOf(challengeLog.getFloorId());
            int challengeCount = challengeLog.getChallengeCount();

            // 获取血量信息
            Map<String, String> hpMap = getCurrentHP(studentId, floorId, challengeCount);

            if (hpMap.isEmpty()) {
                log.warn("未找到血量信息 - studentId: {}, floorId: {}, challengeCount: {}",
                        studentId, floorId, challengeCount);
                return Result.error("血量信息不存在");
            }

            return Result.success(hpMap);

        } catch (Exception e) {
            log.error("获取血量信息失败 - studentId: {}, towerChallengeLogId: {}",
                    studentId, towerChallengeLogId, e);
            return Result.error("获取血量信息失败");
        }
    }

    @Override
    public Result<QuestionBank> userAttack(String studentId, String towerChallengeLogId) {
        if (!validateParams(studentId, towerChallengeLogId)) {
            return Result.error("参数不能为空");
        }

        try {
            TowerChallengeLog challengeLog = getTowerChallengeLogById(towerChallengeLogId);
            if (challengeLog == null) {
                return Result.error("挑战记录不存在");
            }

            String floorId = String.valueOf(challengeLog.getFloorId());

            // 获取题目
            QuestionBank question = getRandomQuestionForFloor(floorId);
            if (question == null) {
                return Result.error("暂无可用题目");
            }

            log.info("为玩家提供题目 - studentId: {}, floorId: {}, questionId: {}, difficulty: {}",
                    studentId, floorId, question.getQuestionId(), question.getDifficultyLevel());

            return Result.success(question);

        } catch (Exception e) {
            log.error("获取攻击题目失败 - studentId: {}, towerChallengeLogId: {}",
                    studentId, towerChallengeLogId, e);
            return Result.error("获取题目失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Map<String, String>> checkAswerIsTure(String studentId, String questionId,
                                                        String context, String floorId, String towerChallengeCount) {

        if (!validateParams(studentId, questionId, context, floorId, towerChallengeCount)) {
            return Result.error("参数不能为空");
        }

        try {
            // 获取题目信息
            QuestionBank question = getQuestionById(questionId);
            if (question == null) {
                return Result.error("题目不存在");
            }

            // AI判题
            boolean isCorrect = judgeAnswerWithAI(question, context);
            log.info("判题结果 - questionId: {}, studentId: {}, isCorrect: {}",
                    questionId, studentId, isCorrect);

            // 计算伤害
            int damage = calculateDamage(floorId, question.getDifficultyLevel().getValue(), isCorrect);

            // 更新血量
            Map<String, String> damageResult = updateBattleHP(studentId, floorId,
                    towerChallengeCount, isCorrect, damage);

            // 保存答题记录
            saveStudentAnswer(studentId, questionId, context, isCorrect);

            // 处理题目状态
            handleQuestionAfterAnswer(floorId, questionId, isCorrect);

            return Result.success(damageResult);

        } catch (Exception e) {
            log.error("判题失败 - studentId: {}, questionId: {}", studentId, questionId, e);
            return Result.error("判题失败: " + e.getMessage());
        }
    }

    @Override
    public Result<Integer> getUserChangeCount(String studentId, String floorId) {
        if (!validateParams(studentId, floorId)) {
            return Result.error("参数不能为空");
        }

        try {
            TowerChallengeLog challengeLog = getTowerChallengeLog(floorId, studentId);
            int challengeCount = (challengeLog != null && challengeLog.getChallengeCount() != null)
                    ? challengeLog.getChallengeCount()
                    : 0;

            return Result.success(challengeCount);

        } catch (Exception e) {
            log.error("获取挑战次数失败 - studentId: {}, floorId: {}", studentId, floorId, e);
            return Result.error("获取挑战次数失败");
        }
    }

    @Override
    public Result<Integer> getDamage(String questionId, String floorId) {
        if (!validateParams(questionId, floorId)) {
            return Result.error("参数不能为空");
        }

        try {
            QuestionBank question = getQuestionById(questionId);
            if (question == null) {
                return Result.error("题目不存在");
            }

            int damage = calculateDamage(floorId, question.getDifficultyLevel().getValue(), true);
            return Result.success(damage);

        } catch (Exception e) {
            log.error("计算伤害失败 - questionId: {}, floorId: {}", questionId, floorId, e);
            return Result.error("计算伤害失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Integer> getResult(String floorId, String studentId, String towerChallengeLogId) {
        if (!validateParams(floorId, studentId, towerChallengeLogId)) {
            return Result.error("参数不能为空");
        }

        try {
            TowerChallengeLog challengeLog = getTowerChallengeLogById(towerChallengeLogId);
            if (challengeLog == null) {
                return Result.error("挑战记录不存在");
            }

            int challengeCount = challengeLog.getChallengeCount();

            // 获取当前血量
            Map<String, String> hpMap = getCurrentHP(studentId, floorId, challengeCount);
            if (hpMap.isEmpty()) {
                return Result.error("血量信息不存在");
            }

            int userHP = Integer.parseInt(hpMap.get("GAME_USER_HP"));
            int bossHP = Integer.parseInt(hpMap.get("BOSS_HP"));

            BattleResult result = determineBattleResult(userHP, bossHP);

            // 更新挑战状态
            updateChallengeStatus(towerChallengeLogId, result);

            // 处理胜利逻辑
            if (result == BattleResult.PLAYER_WIN) {
                handlePlayerVictory(floorId, studentId);
            }

            log.info("战斗结果 - floorId: {}, studentId: {}, result: {}, userHP: {}, bossHP: {}",
                    floorId, studentId, result.getDescription(), userHP, bossHP);

            return Result.success(result.getCode());

        } catch (Exception e) {
            log.error("获取战斗结果失败 - floorId: {}, studentId: {}", floorId, studentId, e);
            return Result.error("获取战斗结果失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<AwardVO> getAward(String studentId, String floorId) {
        if (!validateParams(studentId, floorId)) {
            return Result.error("参数不能为空");
        }

        try {
            // 获取任务奖励配置
            Task task = getTaskByFloorId(floorId);
            if (task == null) {
                return Result.error("任务不存在");
            }

            // 处理物品奖励
            List<Item> rewardItems = calculateItemRewards(task);

            // 处理经验奖励
            boolean expUpdated = updatePlayerExperience(studentId, task.getRewardExp());
            if (!expUpdated) {
                log.warn("更新经验失败 - studentId: {}, exp: {}", studentId, task.getRewardExp());
            }

            // 构建奖励结果
            AwardVO award = new AwardVO();
            award.setExp(String.valueOf(task.getRewardExp()));
            award.setItem(rewardItems);

            log.info("发放奖励成功 - studentId: {}, floorId: {}, exp: {}, items: {}",
                    studentId, floorId, task.getRewardExp(), rewardItems.size());

            return Result.success(award);

        } catch (Exception e) {
            log.error("获取奖励失败 - studentId: {}, floorId: {}", studentId, floorId, e);
            return Result.error("获取奖励失败");
        }
    }

    // =================== 私有辅助方法 ===================

    private boolean validateParams(String... params) {
        for (String param : params) {
            if (!StringUtils.hasText(param)) {
                return false;
            }
        }
        return true;
    }

    private TowerChallengeLog getTowerChallengeLog(String floorId, String studentId) {
        LambdaQueryWrapper<TowerChallengeLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TowerChallengeLog::getFloorId, floorId)
                .eq(TowerChallengeLog::getUserId, studentId)
                .orderByDesc(TowerChallengeLog::getId)
                .last("LIMIT 1");
        return towerChallengeLogMapper.selectOne(wrapper);
    }

    private TowerChallengeLog getTowerChallengeLogById(String towerChallengeLogId) {
        return towerChallengeLogMapper.selectById(towerChallengeLogId);
    }

    private TowerChallengeLog createTowerChallengeLog(String floorId, String studentId, int challengeCount) {
        TowerChallengeLog challengeLog = new TowerChallengeLog();
        challengeLog.setChallengeCount(challengeCount);
        challengeLog.setStatus("进行中");
        challengeLog.setFloorId(Long.valueOf(floorId));
        challengeLog.setUserId(Long.valueOf(studentId));
        challengeLog.setLastChallengeTime(LocalDateTime.now());

        int inserted = towerChallengeLogMapper.insert(challengeLog);
        if (inserted <= 0) {
            throw new RuntimeException("创建挑战记录失败");
        }

        return challengeLog;
    }

    private boolean initializeBattleHP(String floorId, String studentId, int challengeCount) {
        try {
            // 获取怪物信息
            Monster monster = getMonsterByFloorId(floorId);
            if (monster == null) {
                throw new RuntimeException("怪物不存在");
            }

            // 获取玩家信息
            GameUser gameUser = getGameUserById(studentId);
            if (gameUser == null) {
                throw new RuntimeException("玩家不存在");
            }

            int userHP = calculateGameUserHP(gameUser.getLevel());
            int bossHP = monster.getHp();

            // 设置Redis键
            String userHpKey = String.format(REDIS_USER_HP_KEY_TEMPLATE, studentId, floorId, challengeCount);
            String bossHpKey = String.format(REDIS_BOSS_HP_KEY_TEMPLATE, floorId, studentId, challengeCount);

            // 保存到Redis并设置过期时间
            stringRedisTemplate.opsForValue().set(userHpKey, String.valueOf(userHP), REDIS_TTL_HOURS, TimeUnit.HOURS);
            stringRedisTemplate.opsForValue().set(bossHpKey, String.valueOf(bossHP), REDIS_TTL_HOURS, TimeUnit.HOURS);

            log.info("初始化血量成功 - studentId: {}, floorId: {}, userHP: {}, bossHP: {}",
                    studentId, floorId, userHP, bossHP);

            return true;

        } catch (Exception e) {
            log.error("初始化血量失败 - studentId: {}, floorId: {}", studentId, floorId, e);
            return false;
        }
    }

    private Map<String, String> getCurrentHP(String studentId, String floorId, int challengeCount) {
        Map<String, String> hpMap = new HashMap<>(2);

        try {
            String userHpKey = String.format(REDIS_USER_HP_KEY_TEMPLATE, studentId, floorId, challengeCount);
            String bossHpKey = String.format(REDIS_BOSS_HP_KEY_TEMPLATE, floorId, studentId, challengeCount);

            String userHP = stringRedisTemplate.opsForValue().get(userHpKey);
            String bossHP = stringRedisTemplate.opsForValue().get(bossHpKey);

            if (StringUtils.hasText(userHP) && StringUtils.hasText(bossHP)) {
                hpMap.put("GAME_USER_HP", userHP);
                hpMap.put("BOSS_HP", bossHP);
            }

        } catch (Exception e) {
            log.error("获取血量失败 - studentId: {}, floorId: {}", studentId, floorId, e);
        }

        return hpMap;
    }

    private QuestionBank getRandomQuestionForFloor(String floorId) {
        try {
            // 获取题目ID列表
            List<String> questionIds = questionRedisCache.getQuestionDifficultyListInCacheAndNotPushInCache(
                    Integer.valueOf(floorId));

            if (questionIds.isEmpty()) {
                log.warn("塔层{}无可用题目", floorId);
                return null;
            }

            // 获取难度分布
            List<String> difficultyList = questionRedisCache.getTotalQuestionDifficultyListInCache(
                    Integer.valueOf(floorId));

            if (difficultyList.isEmpty()) {
                log.warn("塔层{}无难度配置", floorId);
                return null;
            }

            String targetDifficulty = difficultyList.get(0);

            // 查询题目
            LambdaQueryWrapper<QuestionBank> wrapper = new LambdaQueryWrapper<>();
            wrapper.in(QuestionBank::getQuestionId, questionIds);
            List<QuestionBank> questions = questionBankMapper.selectList(wrapper);

            return selectQuestionByDifficulty(questions, targetDifficulty);

        } catch (Exception e) {
            log.error("获取随机题目失败 - floorId: {}", floorId, e);
            return null;
        }
    }

    private QuestionBank selectQuestionByDifficulty(List<QuestionBank> questions, String difficultyCode) {
        if (questions == null || questions.isEmpty()) {
            return null;
        }

        try {
            Difficulty targetDifficulty = Difficulty.fromCode(difficultyCode);

            List<QuestionBank> filteredQuestions = questions.stream()
                    .filter(q -> q.getDifficultyLevel().name().equals(targetDifficulty.getName()))
                    .toList();

            if (filteredQuestions.isEmpty()) {
                // 如果没有对应难度的题目，随机选择一道
                return questions.get(ThreadLocalRandom.current().nextInt(questions.size()));
            }

            return filteredQuestions.get(ThreadLocalRandom.current().nextInt(filteredQuestions.size()));

        } catch (Exception e) {
            log.warn("按难度筛选题目失败，随机选择 - difficultyCode: {}", difficultyCode, e);
            return questions.get(ThreadLocalRandom.current().nextInt(questions.size()));
        }
    }

    private boolean judgeAnswerWithAI(QuestionBank question, String studentAnswer) {
        try {
            String prompt = buildJudgePrompt(
                    question.getQuestionContent(),
                    question.getQuestionOptions(),
                    question.getCorrectAnswer(),
                    studentAnswer
            );

            String aiResponse = chatLanguageModel.chat(UserMessage.userMessage(prompt)).aiMessage().text();
            boolean isCorrect = aiResponse != null && aiResponse.trim().contains("正确");

            log.debug("AI判题 - questionId: {}, studentAnswer: {}, aiResponse: {}, result: {}",
                    question.getQuestionId(), studentAnswer, aiResponse, isCorrect);

            return isCorrect;

        } catch (Exception e) {
            log.error("AI判题失败 - questionId: {}", question.getQuestionId(), e);
            // 默认为错误，避免系统异常导致的不公平
            return false;
        }
    }

    private Map<String, String> updateBattleHP(String studentId, String floorId,
                                               String challengeCount, boolean isCorrect, int damage) {

        Map<String, String> result = new HashMap<>(2);

        try {
            int count = Integer.parseInt(challengeCount);
            String userHpKey = String.format(REDIS_USER_HP_KEY_TEMPLATE, studentId, floorId, count);
            String bossHpKey = String.format(REDIS_BOSS_HP_KEY_TEMPLATE, floorId, studentId, count);

            if (isCorrect) {
                // 答对 - 扣Boss血
                stringRedisTemplate.opsForValue().decrement(bossHpKey, damage);
                result.put("target", "BOSS_HP");
                result.put("damage", String.valueOf(damage));
            } else {
                // 答错 - 扣玩家血
                stringRedisTemplate.opsForValue().decrement(userHpKey, damage);
                result.put("target", "USER_HP");
                result.put("damage", String.valueOf(damage));
            }

        } catch (Exception e) {
            log.error("更新血量失败 - studentId: {}, floorId: {}", studentId, floorId, e);
        }

        return result;
    }

    private void saveStudentAnswer(String studentId, String questionId, String answer, boolean isCorrect) {
        try {
            StudentAnswer studentAnswer = new StudentAnswer();
            studentAnswer.setStudentId(Integer.valueOf(studentId));
            studentAnswer.setQuestionId(Integer.valueOf(questionId));
            studentAnswer.setStudentAnswer(answer);
            studentAnswer.setIsCorrect(isCorrect ? "1" : "0");

            int inserted = studentAnswerMapper.insert(studentAnswer);
            if (inserted <= 0) {
                log.warn("保存学生答案失败 - studentId: {}, questionId: {}", studentId, questionId);
            }

        } catch (Exception e) {
            log.error("保存学生答案异常 - studentId: {}, questionId: {}", studentId, questionId, e);
        }
    }

    private void handleQuestionAfterAnswer(String floorId, String questionId, boolean isCorrect) {
        try {
            if (!isCorrect) {
                // 答错了，重新放回题目池
                questionRedisCache.setQuestionIdsInCache(Integer.valueOf(floorId), questionId);
            }
            // 答对了，题目从池中移除，不做额外处理
        } catch (Exception e) {
            log.error("处理答题后题目状态失败 - floorId: {}, questionId: {}", floorId, questionId, e);
        }
    }

    private BattleResult determineBattleResult(int userHP, int bossHP) {
        if (userHP <= 0) {
            return BattleResult.PLAYER_LOSE;
        } else if (bossHP <= 0) {
            return BattleResult.PLAYER_WIN;
        } else {
            return BattleResult.CONTINUE;
        }
    }

    private void updateChallengeStatus(String towerChallengeLogId, BattleResult result) {
        try {
            LambdaUpdateWrapper<TowerChallengeLog> wrapper = new LambdaUpdateWrapper<>();
            wrapper.eq(TowerChallengeLog::getId, towerChallengeLogId)
                    .set(TowerChallengeLog::getStatus, result.getDescription())
                    .set(TowerChallengeLog::getLastChallengeTime, LocalDateTime.now());

            towerChallengeLogMapper.update(wrapper);

        } catch (Exception e) {
            log.error("更新挑战状态失败 - logId: {}, result: {}", towerChallengeLogId, result, e);
        }
    }

    private void handlePlayerVictory(String floorId, String studentId) {
        try {
            // 设置塔层为通过状态
            LambdaUpdateWrapper<TowerFloor> wrapper = new LambdaUpdateWrapper<>();
            wrapper.eq(TowerFloor::getFloorId, floorId)
                    .set(TowerFloor::getIsPass, 1);

            int updated = towerFloorMapper.update(wrapper);
            if (updated <= 0) {
                log.warn("设置塔层通过状态失败 - floorId: {}", floorId);
            }

            // 更新排行榜 - 使用修复后的RedisSort方法
            TowerFloor towerFloor = getTowerFloorById(floorId);
            if (towerFloor != null) {
                String towerId = String.valueOf(towerFloor.getTowerId());
                // 使用增量更新而不是绝对设置
                redisSort.incrementStudentScore(towerId, studentId, "1");
                redisSort.incrementTotalScore(studentId, "1");
            }

        } catch (Exception e) {
            log.error("处理玩家胜利逻辑失败 - floorId: {}, studentId: {}", floorId, studentId, e);
        }
    }

    private List<Item> calculateItemRewards(Task task) {
        List<Item> rewardItems = new ArrayList<>();

        try {
            Integer rewardItemQty = task.getRewardItemQty();
            Integer rewardItemRarity = task.getRewardItemRarity();

            if (rewardItemQty == null || rewardItemQty <= 0 || rewardItemRarity == null) {
                return rewardItems;
            }

            // 获取对应稀有度的物品
            LambdaQueryWrapper<Item> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Item::getRarity, rewardItemRarity);
            List<Item> availableItems = itemMapper.selectList(wrapper);

            if (availableItems == null || availableItems.size() < rewardItemQty) {
                log.warn("可用物品不足 - rarity: {}, required: {}, available: {}",
                        rewardItemRarity, rewardItemQty, availableItems != null ? availableItems.size() : 0);
                return rewardItems;
            }

            // 计算获得物品的概率
            double probability = calculateItemProbability(rewardItemRarity, rewardItemQty);

            if (ThreadLocalRandom.current().nextDouble() < probability) {
                // 命中概率，随机选择物品
                Collections.shuffle(availableItems);
                rewardItems = availableItems.subList(0, rewardItemQty);
                log.info("获得物品奖励 - count: {}, rarity: {}", rewardItemQty, rewardItemRarity);
            } else {
                log.info("未命中物品奖励概率 - probability: {}", probability);
            }

        } catch (Exception e) {
            log.error("计算物品奖励失败", e);
        }

        return rewardItems;
    }

    private double calculateItemProbability(int rarity, int quantity) {
        // 基于稀有度和数量计算概率
        double baseProbability = switch (rarity) {
            case 0 -> 0.7;  // 普通物品70%
            case 1 -> 0.4;  // 稀有物品40%
            case 2 -> 0.2;  // 史诗物品20%
            case 3 -> 0.05; // 传说物品5%
            default -> 0.1;
        };

        // 数量越多，概率适当降低
        double quantityFactor = Math.max(0.1, 1.0 - (quantity - 1) * 0.1);

        return baseProbability * quantityFactor;
    }

    private boolean updatePlayerExperience(String studentId, int rewardExp) {
        if (rewardExp <= 0) {
            return true;
        }

        try {
            GameUser gameUser = getGameUserById(studentId);
            if (gameUser == null) {
                return false;
            }

            int currentLevel = gameUser.getLevel();
            int currentExp = gameUser.getExp() != null ? gameUser.getExp() : 0;
            long requiredExp = calculateRequiredExp(currentLevel);

            long totalExp = currentExp + rewardExp;
            int newLevel = currentLevel;
            long remainingExp = totalExp;

            // 处理升级
            while (remainingExp >= requiredExp && newLevel < 999) { // 设置最大等级限制
                remainingExp -= requiredExp;
                newLevel++;
                requiredExp = calculateRequiredExp(newLevel);
            }

            // 更新玩家数据
            LambdaUpdateWrapper<GameUser> wrapper = new LambdaUpdateWrapper<>();
            wrapper.eq(GameUser::getUserId, studentId)
                    .set(GameUser::getExp, (int) remainingExp)
                    .set(GameUser::getLevel, newLevel);

            int updated = gameUserMapper.update(wrapper);

            if (newLevel > currentLevel) {
                log.info("玩家升级 - studentId: {}, level: {} -> {}, exp: {}",
                        studentId, currentLevel, newLevel, remainingExp);
            }

            return updated > 0;

        } catch (Exception e) {
            log.error("更新玩家经验失败 - studentId: {}, rewardExp: {}", studentId, rewardExp, e);
            return false;
        }
    }

    private long calculateRequiredExp(int level) {
        // 优化后的经验计算公式
        final int baseExp = 100;
        final double growthRate = 1.2;
        final double exponent = 1.8;

        double exp = baseExp * Math.pow(level, exponent) * growthRate;
        return Math.max(100, Math.round(exp)); // 最少需要100经验
    }

    private int calculateDamage(String floorId, String difficultyLevel, boolean isCorrect) {
        try {
            TowerFloor towerFloor = getTowerFloorById(floorId);
            if (towerFloor == null) {
                log.warn("塔层不存在 - floorId: {}", floorId);
                return DEFAULT_BASE_DAMAGE;
            }

            int floorNo = towerFloor.getFloorNo();
            double baseDamage = DEFAULT_BASE_DAMAGE + floorNo * DAMAGE_PER_FLOOR;

            // 获取难度系数
            double difficultyFactor = 1.0;
            try {
                Difficulty difficulty = Difficulty.fromName(difficultyLevel.toUpperCase());
                difficultyFactor = difficulty.getFactor();
            } catch (IllegalArgumentException e) {
                log.warn("未知难度等级: {}", difficultyLevel);
            }

            // 正确与错误的系数
            double correctnessFactor = isCorrect ? 1.0 : 0.6; // 答错时伤害稍低

            int finalDamage = (int) Math.round(baseDamage * difficultyFactor * correctnessFactor);
            return Math.max(1, finalDamage); // 最小伤害为1

        } catch (Exception e) {
            log.error("计算伤害失败 - floorId: {}, difficulty: {}", floorId, difficultyLevel, e);
            return DEFAULT_BASE_DAMAGE;
        }
    }

    // =================== 数据查询辅助方法 ===================

    private QuestionBank getQuestionById(String questionId) {
        try {
            return questionBankMapper.selectById(questionId);
        } catch (Exception e) {
            log.error("查询题目失败 - questionId: {}", questionId, e);
            return null;
        }
    }

    private Monster getMonsterByFloorId(String floorId) {
        try {
            LambdaQueryWrapper<Monster> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Monster::getFloorId, floorId);
            return bossMapper.selectOne(wrapper);
        } catch (Exception e) {
            log.error("查询怪物失败 - floorId: {}", floorId, e);
            return null;
        }
    }

    private GameUser getGameUserById(String studentId) {
        try {
            LambdaQueryWrapper<GameUser> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(GameUser::getUserId, studentId);
            return gameUserMapper.selectOne(wrapper);
        } catch (Exception e) {
            log.error("查询游戏用户失败 - studentId: {}", studentId, e);
            return null;
        }
    }

    private TowerFloor getTowerFloorById(String floorId) {
        try {
            LambdaQueryWrapper<TowerFloor> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(TowerFloor::getFloorId, floorId);
            return towerFloorMapper.selectOne(wrapper);
        } catch (Exception e) {
            log.error("查询塔层失败 - floorId: {}", floorId, e);
            return null;
        }
    }

    private Task getTaskByFloorId(String floorId) {
        try {
            LambdaQueryWrapper<Task> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Task::getFloorId, floorId);
            return taskMapper.selectOne(wrapper);
        } catch (Exception e) {
            log.error("查询任务失败 - floorId: {}", floorId, e);
            return null;
        }
    }

    /**
     * 构建AI判题提示词
     *
     * @param questionContent 题目内容
     * @param questionOptions 题目选项
     * @param correctAnswer   正确答案
     * @param studentAnswer   学生答案
     * @return 判题提示词
     */
    private String buildJudgePrompt(String questionContent, String questionOptions,
                                    String correctAnswer, String studentAnswer) {

        StringBuilder prompt = new StringBuilder();
        prompt.append("请判断学生答案的正确性。\n\n");
        prompt.append("题目内容：\n").append(questionContent).append("\n\n");

        if (StringUtils.hasText(questionOptions)) {
            prompt.append("选项：\n").append(questionOptions).append("\n\n");
        }

        prompt.append("标准答案：\n").append(correctAnswer).append("\n\n");
        prompt.append("学生答案：\n").append(studentAnswer).append("\n\n");
        prompt.append("请仅回答正确或错误，不要包含其他内容。");

        return prompt.toString();
    }

    /**
     * 清理过期的战斗数据（定时任务调用）
     *
     * @param hours 保留小时数
     * @return 清理的键数量
     */
    public int cleanupExpiredBattleData(int hours) {
        // 这里可以实现清理逻辑，删除过期的Redis键
        // 由于Redis已经设置了TTL，这个方法主要用于数据库清理
        log.info("执行战斗数据清理任务 - 保留{}小时内的数据", hours);
        // TODO: 实现具体的清理逻辑
        return 0;
    }

    /**
     * 获取玩家战斗统计信息
     *
     * @param studentId 学生ID
     * @return 战斗统计
     */
    public Map<String, Object> getBattleStatistics(String studentId) {
        Map<String, Object> stats = new HashMap<>();

        try {
            // 统计挑战次数
            LambdaQueryWrapper<TowerChallengeLog> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(TowerChallengeLog::getUserId, studentId);

            Long totalChallenges = towerChallengeLogMapper.selectCount(wrapper);

            wrapper.eq(TowerChallengeLog::getStatus, "成功");
            Long successfulChallenges = towerChallengeLogMapper.selectCount(wrapper);

            stats.put("totalChallenges", totalChallenges);
            stats.put("successfulChallenges", successfulChallenges);
            stats.put("successRate", totalChallenges > 0 ?
                    (double) successfulChallenges / totalChallenges : 0.0);

        } catch (Exception e) {
            log.error("获取战斗统计失败 - studentId: {}", studentId, e);
        }

        return stats;
    }
}