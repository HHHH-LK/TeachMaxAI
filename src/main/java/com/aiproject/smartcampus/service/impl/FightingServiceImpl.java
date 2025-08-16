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
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.aiproject.smartcampus.service.impl.GameUserServiceImpl.calculateGameUserHP;

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

    private static final String REDIS_USER_HP_KEY_TEMPLATE = "game:user:hp:%s:%s:%d";
    private static final String REDIS_BOSS_HP_KEY_TEMPLATE = "game:boss:hp:%s:%s:%d";
    private static final int REDIS_TTL_HOURS = 24;
    private static final int DEFAULT_BASE_DAMAGE = 30;
    private static final int DAMAGE_PER_FLOOR = 5;
    private final UserItemMapper userItemMapper;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    static {
        MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    // Redis 原子扣血 + 钳制非负 + 续期（返回扣后HP）
    private static final DefaultRedisScript<Long> HP_DECR_CLAMP_SCRIPT;

    static {
        HP_DECR_CLAMP_SCRIPT = new DefaultRedisScript<>();
        HP_DECR_CLAMP_SCRIPT.setResultType(Long.class);
        HP_DECR_CLAMP_SCRIPT.setScriptText(
                """
                        local key = KEYS[1]
                        local damage = tonumber(ARGV[1])
                        local ttl = tonumber(ARGV[2])
                        local cur = redis.call('GET', key)
                        if not cur then return nil end
                        local n = tonumber(cur) or 0
                        n = n - damage
                        if n < 0 then n = 0 end
                        redis.call('SETEX', key, ttl, tostring(n))
                        return n
                        """
        );
    }

    private final BattleLogMapper battleLogMapper;


    public enum BattleResult {
        PLAYER_WIN(1, "成功"),
        PLAYER_LOSE(0, "失败"),
        CONTINUE(3, "进行中");
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

        public static Difficulty fromName(String name) {
            for (Difficulty d : values()) if (d.name.equalsIgnoreCase(name)) return d;
            throw new IllegalArgumentException("未知难度名称: " + name);
        }

        public double getFactor() {
            return factor;
        }
    }

    @Override
    public Result<BattleLog> getCurrentBattleLog(String floorId, String towerChallengeLogId, String studentId) {
        if (!validateParams(floorId, towerChallengeLogId, studentId)) return Result.error("参数不能为空");
        try {
            // 查询最新的战斗日志
            BattleLog lastLog = battleLogMapper.selectOne(
                    new LambdaQueryWrapper<BattleLog>()
                            .eq(BattleLog::getFloorId, Long.valueOf(floorId))
                            .eq(BattleLog::getUserId, Long.valueOf(studentId))
                            .eq(BattleLog::getTowerChallengeLogId, Long.valueOf(towerChallengeLogId))
                            .orderByDesc(BattleLog::getId)
                            .last("LIMIT 1")
            );

            // 计算当前回合数
            int currentTurns = Optional.ofNullable(lastLog)
                    .map(log -> log.getTotalTurns() + 1)
                    .orElse(1);

            // 获取怪物信息
            Monster monster = getMonsterByFloorId(floorId);
            if (monster == null) {
                return Result.error("未找到对应层的怪物");
            }

            // 构建并保存战斗日志
            BattleLog battleLog = new BattleLog();
            battleLog.setUserId(Long.valueOf(studentId));
            battleLog.setFloorId(Long.valueOf(floorId));
            battleLog.setMonsterId(monster.getMonsterId());
            battleLog.setTotalTurns(currentTurns);
            battleLog.setTowerChallengeLogId(currentTurns);

            battleLogMapper.insert(battleLog);

            //缓存towerChallengeLogId到redis中
            stringRedisTemplate.opsForValue().set("current:battleLogId:", String.valueOf(battleLog.getId()));

            return Result.success(battleLog);

        } catch (Exception e) {
            log.error("获取战斗日志失败 - floorId: {}, studentId: {}", floorId, studentId, e);
            return Result.error("获取战斗日志失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Long> startFighting(String floorId, String studentId) {
        if (!validateParams(floorId, studentId)) return Result.error("参数不能为空");
        try {
            TowerChallengeLog existingLog = getTowerChallengeLog(floorId, studentId);
            int currentChallengeCount = (existingLog != null && existingLog.getChallengeCount() != null)
                    ? existingLog.getChallengeCount() + 1 : 1;
            TowerChallengeLog newLog = createTowerChallengeLog(floorId, studentId, currentChallengeCount);
            boolean hpInitialized = initializeBattleHP(floorId, studentId, currentChallengeCount);
            if (!hpInitialized) throw new RuntimeException("初始化战斗血量失败");
            log.info("开始战斗 - floorId: {}, studentId: {}, challengeCount: {}, logId: {}",
                    floorId, studentId, currentChallengeCount, newLog.getId());
            return Result.success(newLog.getId());
        } catch (Exception e) {
            log.error("开始战斗失败 - floorId: {}, studentId: {}", floorId, studentId, e);
            return Result.error("开始战斗失败: " + e.getMessage());
        }
    }

    @Override
    public Result<Map<String, String>> getgetGameUserHPandbossHP(String studentId, String towerChallengeLogId) {
        if (!validateParams(studentId, towerChallengeLogId)) return Result.error("参数不能为空");
        try {
            TowerChallengeLog logRow = getTowerChallengeLogById(towerChallengeLogId);
            if (logRow == null) return Result.error("挑战记录不存在");
            String floorId = String.valueOf(logRow.getFloorId());
            int challengeCount = logRow.getChallengeCount();
            Map<String, String> hpMap = getCurrentHP(studentId, floorId, challengeCount);
            if (hpMap.isEmpty()) return Result.error("血量信息不存在");
            return Result.success(hpMap);
        } catch (Exception e) {
            log.error("获取血量失败 - studentId: {}, logId: {}", studentId, towerChallengeLogId, e);
            return Result.error("获取血量信息失败");
        }
    }

    @Override
    public Result<QuestionBank> userAttack(String studentId, String towerChallengeLogId) {
        if (!validateParams(studentId, towerChallengeLogId)) return Result.error("参数不能为空");
        try {
            TowerChallengeLog logRow = getTowerChallengeLogById(towerChallengeLogId);
            if (logRow == null) return Result.error("挑战记录不存在");
            String floorId = String.valueOf(logRow.getFloorId());
            QuestionBank question = getOneQuestionForFloor(floorId);
            if (question == null) return Result.error("暂无可用题目");

            log.info("出题 - studentId: {}, floorId: {}, questionId: {}, difficulty: {}",
                    studentId, floorId, question.getQuestionId(), question.getDifficultyLevel());
            return Result.success(question);
        } catch (Exception e) {
            log.error("获取攻击题目失败 - studentId: {}, logId: {}", studentId, towerChallengeLogId, e);
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
            QuestionBank question = getQuestionById(questionId);
            if (question == null) return Result.error("题目不存在");

            // 先本地判题，不能本地判再调 LLM
            Boolean localResult = judgeAnswerLocal(question, context);
            boolean isCorrect = (localResult != null) ? localResult : judgeAnswerWithAI(question, context);

            //进行回合处理（入库处理）
            String result = isCorrect ? "成功" : "失败";
            String battleLogId = stringRedisTemplate.opsForValue().get("current:battleLogId:");
            String description = question.getDifficultyLevel().getDescription();
            String detail = switch (description) {
                case "简单" -> "普通攻击";
                case "中等" -> "技能";
                case "困难" -> "必杀技";
                default -> throw new IllegalStateException("Unexpected value: " + description);
            };

            LambdaUpdateWrapper<BattleLog> battleLogUpdateWrapper = new LambdaUpdateWrapper<>();
            battleLogUpdateWrapper.eq(BattleLog::getId, battleLogId);
            battleLogUpdateWrapper.set(BattleLog::getQuestionId, questionId);
            battleLogUpdateWrapper.set(BattleLog::getResult, result);
            battleLogUpdateWrapper.set(BattleLog::getDetail, detail);

            battleLogMapper.update(null, battleLogUpdateWrapper);

            log.info("判题 - questionId: {}, studentId: {}, correct: {}", questionId, studentId, isCorrect);

            String diffName = question.getDifficultyLevel() == null ? "MEDIUM" : question.getDifficultyLevel().name();
            int damage = calculateDamage(floorId, diffName, isCorrect);

            Map<String, String> damageResult = updateBattleHP(studentId, floorId, towerChallengeCount, isCorrect, damage);
            saveStudentAnswer(studentId, questionId, context, isCorrect);
            handleQuestionAfterAnswer(floorId, questionId, isCorrect);

            return Result.success(damageResult);
        } catch (Exception e) {
            log.error("判题失败 - studentId: {}, questionId: {}", studentId, questionId, e);
            return Result.error("判题失败: " + e.getMessage());
        }
    }


    @Override
    public Result<Integer> getUserChangeCount(String studentId, String floorId) {
        if (!validateParams(studentId, floorId)) return Result.error("参数不能为空");
        try {
            TowerChallengeLog logRow = getTowerChallengeLog(floorId, studentId);
            int cnt = (logRow != null && logRow.getChallengeCount() != null) ? logRow.getChallengeCount() : 0;
            return Result.success(cnt);
        } catch (Exception e) {
            log.error("获取挑战次数失败 - studentId: {}, floorId: {}", studentId, floorId, e);
            return Result.error("获取挑战次数失败");
        }
    }

    @Override
    public Result<Integer> getDamage(String questionId, String floorId) {
        if (!validateParams(questionId, floorId)) return Result.error("参数不能为空");
        try {
            QuestionBank question = getQuestionById(questionId);
            if (question == null) return Result.error("题目不存在");
            String diffName = question.getDifficultyLevel() == null ? "MEDIUM" : question.getDifficultyLevel().name();
            int damage = calculateDamage(floorId, diffName, true);
            return Result.success(damage);
        } catch (Exception e) {
            log.error("计算伤害失败 - questionId: {}, floorId: {}", questionId, floorId, e);
            return Result.error("计算伤害失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Integer> getResult(String floorId, String studentId, String towerChallengeLogId) {
        if (!validateParams(floorId, studentId, towerChallengeLogId)) return Result.error("参数不能为空");
        try {
            TowerChallengeLog logRow = getTowerChallengeLogById(towerChallengeLogId);
            if (logRow == null) return Result.error("挑战记录不存在");

            int challengeCount = logRow.getChallengeCount();
            Map<String, String> hpMap = getCurrentHP(studentId, floorId, challengeCount);
            if (hpMap.isEmpty()) return Result.error("血量信息不存在");

            int userHP = Integer.parseInt(hpMap.get("GAME_USER_HP"));
            int bossHP = Integer.parseInt(hpMap.get("BOSS_HP"));
            BattleResult result = determineBattleResult(userHP, bossHP);

            updateChallengeStatus(towerChallengeLogId, result);
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
        //设置每个道具获取的数量默认为1;
        final Integer AWARD_NUM = 1;

        if (!validateParams(studentId, floorId)) return Result.error("参数不能为空");
        try {
            Task task = getTaskByFloorId(floorId);
            if (task == null) return Result.error("任务不存在");
            List<Item> rewardItems = calculateItemRewards(task);
            boolean expUpdated = updatePlayerExperience(studentId, task.getRewardExp());
            if (!expUpdated) log.warn("更新经验失败 - studentId: {}, exp: {}", studentId, task.getRewardExp());

            AwardVO award = new AwardVO();
            award.setExp(String.valueOf(task.getRewardExp()));
            award.setItem(rewardItems);
            //将用户获得的奖励存储到到DB中
            List<UserItem> list = rewardItems.stream().map(a -> {
                UserItem userItem = new UserItem();
                userItem.setItemId(a.getItemId());
                userItem.setUserId(Long.valueOf(studentId));
                userItem.setQuantity(AWARD_NUM);
                return userItem;
            }).toList();
            userItemMapper.insert(list);

            log.info("发放奖励 - studentId: {}, floorId: {}, exp: {}, items: {}", studentId, floorId, task.getRewardExp(), rewardItems.size());
            return Result.success(award);
        } catch (Exception e) {
            log.error("获取奖励失败 - studentId: {}, floorId: {}", studentId, floorId, e);
            return Result.error("获取奖励失败");
        }
    }

    @Override
    public Result<Long> getRequireExp(String studentLevel) {
        long requiredExp = calculateRequiredExp(Integer.parseInt(studentLevel));
        log.info("升级需求 - from level[{}] 需要[{}]经验", studentLevel, requiredExp);
        return Result.success(requiredExp);
    }

    @Override
    public Result<Integer> getLevelAdds(String studentLevel, String awardExp) {
        int level = Integer.parseInt(studentLevel);
        long currentExp = Long.parseLong(awardExp);
        int levelUpCount = 0;
        while (true) {
            long requireExp = calculateRequiredExp(level);
            if (currentExp >= requireExp) {
                currentExp -= requireExp;
                level++;
                levelUpCount++;
            } else break;
        }
        return Result.success(levelUpCount);
    }

    // =================== 私有辅助方法 ===================

    private boolean validateParams(String... params) {
        for (String p : params) if (!StringUtils.hasText(p)) return false;
        return true;
    }

    private TowerChallengeLog getTowerChallengeLog(String floorId, String studentId) {
        LambdaQueryWrapper<TowerChallengeLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TowerChallengeLog::getFloorId, Long.valueOf(floorId))
                .eq(TowerChallengeLog::getUserId, Long.valueOf(studentId))
                .orderByDesc(TowerChallengeLog::getId)
                .last("LIMIT 1");
        return towerChallengeLogMapper.selectOne(wrapper);
    }

    private TowerChallengeLog getTowerChallengeLogById(String towerChallengeLogId) {
        return towerChallengeLogMapper.selectById(Long.valueOf(towerChallengeLogId));
    }

    private TowerChallengeLog createTowerChallengeLog(String floorId, String studentId, int challengeCount) {
        TowerChallengeLog challengeLog = new TowerChallengeLog();
        challengeLog.setChallengeCount(challengeCount);
        challengeLog.setStatus(BattleResult.CONTINUE.getDescription());
        challengeLog.setFloorId(Long.valueOf(floorId));
        challengeLog.setUserId(Long.valueOf(studentId));
        challengeLog.setLastChallengeTime(LocalDateTime.now());
        int inserted = towerChallengeLogMapper.insert(challengeLog);
        if (inserted <= 0) throw new RuntimeException("创建挑战记录失败");
        return challengeLog;
    }

    private boolean initializeBattleHP(String floorId, String studentId, int challengeCount) {
        try {
            Monster monster = getMonsterByFloorId(floorId);
            if (monster == null) throw new RuntimeException("怪物不存在");
            GameUser gameUser = getGameUserById(studentId);
            if (gameUser == null) throw new RuntimeException("玩家不存在");

            int userHP = calculateGameUserHP(gameUser.getLevel());
            int bossHP = Optional.ofNullable(monster.getHp()).orElse(200);

            String userHpKey = String.format(REDIS_USER_HP_KEY_TEMPLATE, studentId, floorId, challengeCount);
            String bossHpKey = String.format(REDIS_BOSS_HP_KEY_TEMPLATE, floorId, studentId, challengeCount);
            stringRedisTemplate.opsForValue().set(userHpKey, String.valueOf(userHP), REDIS_TTL_HOURS, TimeUnit.HOURS);
            stringRedisTemplate.opsForValue().set(bossHpKey, String.valueOf(bossHP), REDIS_TTL_HOURS, TimeUnit.HOURS);

            log.info("初始化血量 - studentId: {}, floorId: {}, userHP: {}, bossHP: {}", studentId, floorId, userHP, bossHP);
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

    // 优先从Redis题目队列弹出；为空则按任务pointIds随机从DB兜底
    private QuestionBank getOneQuestionForFloor(String floorId) {
        try {
            Integer fid = Integer.valueOf(floorId);
            String poppedId = questionRedisCache.getQuestionIdInCache(fid);
            if (StringUtils.hasText(poppedId)) {
                QuestionBank q = questionBankMapper.selectById(Integer.valueOf(poppedId));
                if (q != null) return q;
            }
            Task task = getTaskByFloorId(floorId);
            if (task == null || !StringUtils.hasText(task.getPointIds())) {
                log.warn("无任务或无知识点，无法兜底取题 - floorId: {}", floorId);
                return null;
            }
            List<Integer> pointIds = parsePointIds(task.getPointIds());
            if (pointIds.isEmpty()) return null;
            LambdaQueryWrapper<QuestionBank> wrapper = new LambdaQueryWrapper<>();
            wrapper.in(QuestionBank::getPointId, pointIds);
            List<QuestionBank> candidates = questionBankMapper.selectList(wrapper);
            if (candidates == null || candidates.isEmpty()) return null;
            return candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
        } catch (Exception e) {
            log.error("获取题目失败 - floorId: {}", floorId, e);
            return null;
        }
    }

    private List<Integer> parsePointIds(String raw) {
        if (!StringUtils.hasText(raw)) return List.of();
        List<Integer> ids = new ArrayList<>();
        Matcher m = Pattern.compile("\\d+").matcher(raw);
        while (m.find()) {
            try {
                ids.add(Integer.parseInt(m.group()));
            } catch (NumberFormatException ignored) {
            }
        }
        return ids;
    }

    // 本地判题（能判就返回true/false；无法判题返回null）
    private Boolean judgeAnswerLocal(QuestionBank question, String studentAnswer) {
        try {
            if (question.getQuestionType() == null) return null;
            String type = question.getQuestionType().getValue();
            String correct = question.getCorrectAnswer();
            if (!StringUtils.hasText(correct)) return null;

            switch (type.toLowerCase()) {
                case "single_choice": {
                    String std = normalizeLabel(extractFirstLabel(correct));
                    String stu = normalizeLabel(extractFirstLabel(studentAnswer));
                    if (!StringUtils.hasText(std) || !StringUtils.hasText(stu)) return null;
                    return std.equals(stu);
                }
                case "multiple_choice": {
                    Set<String> std = parseLabelSet(correct);
                    Set<String> stu = parseLabelSet(studentAnswer);
                    if (std.isEmpty() || stu.isEmpty()) return null;
                    return std.equals(stu);
                }
                case "true_false": {
                    Boolean std = parseBooleanAnswer(correct);
                    Boolean stu = parseBooleanAnswer(studentAnswer);
                    if (std == null || stu == null) return null;
                    return Objects.equals(std, stu);
                }
                case "fill_blank": {
                    List<String> std = parseTextList(correct);
                    List<String> stu = parseTextList(studentAnswer);
                    if (std.isEmpty() || stu.isEmpty()) return null;
                    // 长度不同直接错误；长度相同则逐项宽松匹配（忽略大小写与空白）
                    if (std.size() != stu.size()) return false;
                    for (int i = 0; i < std.size(); i++) {
                        String a = normalizeText(std.get(i));
                        String b = normalizeText(stu.get(i));
                        if (!Objects.equals(a, b)) return false;
                    }
                    return true;
                }
                case "short_answer":
                default:
                    return null; // 交给 LLM 判
            }
        } catch (Exception e) {
            log.warn("本地判题异常，切换LLM - qid: {}", question.getQuestionId(), e);
            return null;
        }
    }

    private boolean judgeAnswerWithAI(QuestionBank question, String studentAnswer) {
        try {
            String sys = "你是严格的判题器。只输出JSON：{\"correct\":true/false}。不要输出其它内容。";
            String prompt = buildJudgePrompt(
                    question.getQuestionContent(),
                    question.getQuestionOptions(),
                    question.getCorrectAnswer(),
                    studentAnswer
            );
            var resp = chatLanguageModel.chat(Arrays.asList(
                    SystemMessage.systemMessage(sys),
                    UserMessage.userMessage(prompt)
            ));
            String text = (resp == null || resp.aiMessage() == null) ? null : resp.aiMessage().text();
            if (text != null) {
                String t = text.trim().toLowerCase();
                if (t.contains("\"correct\":true")) return true;
                if (t.contains("\"correct\":false")) return false;
            }
            // 最后兜底（极端情况下）
            return text != null && text.contains("正确");
        } catch (Exception e) {
            log.error("LLM判题失败 - questionId: {}", question.getQuestionId(), e);
            return false;
        }
    }

    private Map<String, String> updateBattleHP(String studentId, String floorId,
                                               String challengeCount, boolean isCorrect, int damage) {
        Map<String, String> result = new HashMap<>(3);
        try {
            int cnt = Integer.parseInt(challengeCount);
            String userHpKey = String.format(REDIS_USER_HP_KEY_TEMPLATE, studentId, floorId, cnt);
            String bossHpKey = String.format(REDIS_BOSS_HP_KEY_TEMPLATE, floorId, studentId, cnt);
            int ttlSeconds = REDIS_TTL_HOURS * 3600;

            if (isCorrect) {
                Long after = stringRedisTemplate.execute(
                        HP_DECR_CLAMP_SCRIPT,
                        Collections.singletonList(bossHpKey),
                        String.valueOf(damage),
                        String.valueOf(ttlSeconds)
                );
                result.put("target", "BOSS_HP");
                result.put("damage", String.valueOf(damage));
                result.put("after", String.valueOf(after != null ? after : -1));
            } else {
                Long after = stringRedisTemplate.execute(
                        HP_DECR_CLAMP_SCRIPT,
                        Collections.singletonList(userHpKey),
                        String.valueOf(damage),
                        String.valueOf(ttlSeconds)
                );
                result.put("target", "USER_HP");
                result.put("damage", String.valueOf(damage));
                result.put("after", String.valueOf(after != null ? after : -1));
            }
        } catch (Exception e) {
            log.error("原子扣血失败 - studentId: {}, floorId: {}", studentId, floorId, e);
        }
        return result;
    }

    private void saveStudentAnswer(String studentId, String questionId, String answer, boolean isCorrect) {
        try {
            StudentAnswer sa = new StudentAnswer();
            sa.setStudentId(Integer.valueOf(studentId));
            sa.setQuestionId(Integer.valueOf(questionId));
            sa.setStudentAnswer(answer);
            sa.setIsCorrect(isCorrect ? "1" : "0");
            int inserted = studentAnswerMapper.insert(sa);
            if (inserted <= 0) log.warn("保存答案失败 - studentId: {}, questionId: {}", studentId, questionId);
        } catch (Exception e) {
            log.error("保存答案异常 - studentId: {}, questionId: {}", studentId, questionId, e);
        }
    }

    private void handleQuestionAfterAnswer(String floorId, String questionId, boolean isCorrect) {
        try {
            if (!isCorrect) {
                questionRedisCache.setQuestionIdsInCache(Integer.valueOf(floorId), String.valueOf(questionId));
            }
        } catch (Exception e) {
            log.error("处理题目队列失败 - floorId: {}, questionId: {}", floorId, questionId, e);
        }
    }

    private BattleResult determineBattleResult(int userHP, int bossHP) {
        if (userHP <= 0) return BattleResult.PLAYER_LOSE;
        if (bossHP <= 0) return BattleResult.PLAYER_WIN;
        return BattleResult.CONTINUE;
    }

    private void updateChallengeStatus(String towerChallengeLogId, BattleResult result) {
        try {
            LambdaUpdateWrapper<TowerChallengeLog> wrapper = new LambdaUpdateWrapper<>();
            wrapper.eq(TowerChallengeLog::getId, Long.valueOf(towerChallengeLogId))
                    .set(TowerChallengeLog::getStatus, result.getDescription())
                    .set(TowerChallengeLog::getLastChallengeTime, LocalDateTime.now());
            towerChallengeLogMapper.update(null, wrapper);
        } catch (Exception e) {
            log.error("更新挑战状态失败 - logId: {}, result: {}", towerChallengeLogId, result, e);
        }
    }

    private void handlePlayerVictory(String floorId, String studentId) {
        try {
            LambdaUpdateWrapper<TowerFloor> wrapper = new LambdaUpdateWrapper<>();
            wrapper.eq(TowerFloor::getFloorId, Long.valueOf(floorId))
                    .set(TowerFloor::getIsPass, 1);
            int updated = towerFloorMapper.update(null, wrapper);
            if (updated <= 0) log.warn("设置通过状态失败 - floorId: {}", floorId);

            TowerFloor tf = getTowerFloorById(floorId);
            if (tf != null) {
                String towerId = String.valueOf(tf.getTowerId());
                redisSort.incrementStudentScore(towerId, studentId, "1");
                redisSort.incrementTotalScore(studentId, "1");
            }
        } catch (Exception e) {
            log.error("胜利处理失败 - floorId: {}, studentId: {}", floorId, studentId, e);
        }
    }

    private List<Item> calculateItemRewards(Task task) {
        List<Item> rewardItems = new ArrayList<>();
        try {
            Integer qty = task.getRewardItemQty();
            Integer rarity = task.getRewardItemRarity();
            if (qty == null || qty <= 0 || rarity == null) return rewardItems;

            LambdaQueryWrapper<Item> w = new LambdaQueryWrapper<>();
            w.eq(Item::getRarity, rarity);
            List<Item> available = itemMapper.selectList(w);
            if (available == null || available.size() < qty) {
                log.warn("可用物品不足 - rarity: {}, required: {}, available: {}", rarity, qty, available == null ? 0 : available.size());
                return rewardItems;
            }

            double prob = calculateItemProbability(rarity, qty);
            if (ThreadLocalRandom.current().nextDouble() < prob) {
                Collections.shuffle(available);
                rewardItems = available.subList(0, qty);
                log.info("获得物品奖励 - count: {}, rarity: {}", qty, rarity);
            } else {
                log.info("未命中奖励概率 - probability: {}", prob);
            }
        } catch (Exception e) {
            log.error("计算物品奖励失败", e);
        }
        return rewardItems;
    }

    private double calculateItemProbability(int rarity, int quantity) {
        double base = switch (rarity) {
            case 0 -> 0.7;
            case 1 -> 0.4;
            case 2 -> 0.2;
            case 3 -> 0.05;
            default -> 0.1;
        };
        double factor = Math.max(0.1, 1.0 - (quantity - 1) * 0.1);
        return base * factor;
    }

    private boolean updatePlayerExperience(String studentId, int rewardExp) {
        if (rewardExp <= 0) return true;
        try {
            GameUser gu = getGameUserById(studentId);
            if (gu == null) return false;

            int currentLevel = gu.getLevel();
            int currentExp = gu.getExp() == null ? 0 : gu.getExp();
            long requiredExp = calculateRequiredExp(currentLevel);

            long totalExp = currentExp + rewardExp;
            int newLevel = currentLevel;
            long remaining = totalExp;

            while (remaining >= requiredExp && newLevel < 100) {
                remaining -= requiredExp;
                newLevel++;
                requiredExp = calculateRequiredExp(newLevel);
            }

            LambdaUpdateWrapper<GameUser> w = new LambdaUpdateWrapper<>();
            w.eq(GameUser::getStudentId, Long.valueOf(studentId))
                    .set(GameUser::getExp, (int) remaining)
                    .set(GameUser::getLevel, newLevel);
            int updated = gameUserMapper.update(null, w);

            if (newLevel > currentLevel) {
                log.info("玩家升级 - studentId: {}, level: {} -> {}, exp: {}", studentId, currentLevel, newLevel, remaining);
            }
            return updated > 0;
        } catch (Exception e) {
            log.error("更新玩家经验失败 - studentId: {}, rewardExp: {}", studentId, rewardExp, e);
            return false;
        }
    }

    public long calculateRequiredExp(int level) {
        final int baseExp = 100;
        final double growthRate = 1.2;
        final double exponent = 1.8;
        double exp = baseExp * Math.pow(level, exponent) * growthRate;
        return Math.max(100, Math.round(exp));
    }

    private int calculateDamage(String floorId, String difficultyName, boolean isCorrect) {
        try {
            TowerFloor tf = getTowerFloorById(floorId);
            int floorNo = tf == null ? 1 : tf.getFloorNo();
            double base = DEFAULT_BASE_DAMAGE + floorNo * DAMAGE_PER_FLOOR;

            double diffFactor = 1.0;
            try {
                diffFactor = Difficulty.fromName(difficultyName.toUpperCase()).getFactor();
            } catch (Exception e) {
                log.warn("未知难度等级: {}", difficultyName);
            }
            double correctFactor = isCorrect ? 1.0 : 0.6;
            int dmg = (int) Math.round(base * diffFactor * correctFactor);
            return Math.max(1, dmg);
        } catch (Exception e) {
            log.error("计算伤害失败 - floorId: {}, difficulty: {}", floorId, difficultyName, e);
            return DEFAULT_BASE_DAMAGE;
        }
    }

    // =================== 数据查询 ===================

    private QuestionBank getQuestionById(String questionId) {
        try {
            return questionBankMapper.selectById(Integer.valueOf(questionId));
        } catch (Exception e) {
            log.error("查询题目失败 - questionId: {}", questionId, e);
            return null;
        }
    }

    private Monster getMonsterByFloorId(String floorId) {
        try {
            LambdaQueryWrapper<Monster> w = new LambdaQueryWrapper<>();
            w.eq(Monster::getFloorId, Long.valueOf(floorId));
            return bossMapper.selectOne(w);
        } catch (Exception e) {
            log.error("查询怪物失败 - floorId: {}", floorId, e);
            return null;
        }
    }

    private GameUser getGameUserById(String studentId) {
        try {
            LambdaQueryWrapper<GameUser> w = new LambdaQueryWrapper<>();
            w.eq(GameUser::getStudentId, Long.valueOf(studentId));
            return gameUserMapper.selectOne(w);
        } catch (Exception e) {
            log.error("查询游戏用户失败 - studentId: {}", studentId, e);
            return null;
        }
    }

    private TowerFloor getTowerFloorById(String floorId) {
        try {
            LambdaQueryWrapper<TowerFloor> w = new LambdaQueryWrapper<>();
            w.eq(TowerFloor::getFloorId, Long.valueOf(floorId));
            return towerFloorMapper.selectOne(w);
        } catch (Exception e) {
            log.error("查询塔层失败 - floorId: {}", floorId, e);
            return null;
        }
    }

    private Task getTaskByFloorId(String floorId) {
        try {
            LambdaQueryWrapper<Task> w = new LambdaQueryWrapper<>();
            w.eq(Task::getFloorId, Long.valueOf(floorId));
            return taskMapper.selectOne(w);
        } catch (Exception e) {
            log.error("查询任务失败 - floorId: {}", floorId, e);
            return null;
        }
    }

    private String buildJudgePrompt(String questionContent, String questionOptions,
                                    String correctAnswer, String studentAnswer) {
        StringBuilder sb = new StringBuilder();
        sb.append("请判断学生答案是否正确，并只返回 JSON：{\"correct\":true/false}。\n\n");
        sb.append("题目内容：\n").append(questionContent).append("\n\n");
        if (StringUtils.hasText(questionOptions)) {
            sb.append("选项（JSON）：\n").append(questionOptions).append("\n\n");
        }
        sb.append("标准答案（JSON或文本）：\n").append(correctAnswer).append("\n\n");
        sb.append("学生答案（JSON或文本）：\n").append(studentAnswer).append("\n\n");
        sb.append("注意：只返回 JSON，不要返回任何解释。");
        return sb.toString();
    }

    // =================== 本地判题解析辅助 ===================

    private String normalizeLabel(String s) {
        if (!StringUtils.hasText(s)) return null;
        String t = s.trim().toUpperCase();
        // 兼容 "A" / "\"A\"" / ["A"] / {"label":"A"} / "答案:A"
        if (t.length() == 1 && t.charAt(0) >= 'A' && t.charAt(0) <= 'Z') return t;
        // 抽取第一个字母
        Matcher m = Pattern.compile("([A-Z])").matcher(t);
        return m.find() ? String.valueOf(m.group(1).charAt(0)) : null;
    }

    private String extractFirstLabel(String raw) {
        if (!StringUtils.hasText(raw)) return null;
        try {
            // 如果是 JSON 数组 ["A","B"]，取第一个
            List<String> arr = MAPPER.readValue(raw, new TypeReference<List<String>>() {
            });
            if (!arr.isEmpty()) return arr.getFirst();
        } catch (Exception ignore) {
        }
        return raw; // 非 JSON 时直接返回原文供 normalize 处理
    }

    private Set<String> parseLabelSet(String raw) {
        Set<String> set = new TreeSet<>();
        if (!StringUtils.hasText(raw)) return set;
        // 尝试 JSON 数组
        try {
            List<String> arr = MAPPER.readValue(raw, new TypeReference<List<String>>() {
            });
            for (String s : arr) {
                String lab = normalizeLabel(s);
                if (lab != null) set.add(lab);
            }
            if (!set.isEmpty()) return set;
        } catch (Exception ignore) {
        }
        // 尝试用 ,;/ 空白 分隔
        String[] parts = raw.split("[,;/，；\\s]+");
        for (String p : parts) {
            String lab = normalizeLabel(p);
            if (lab != null) set.add(lab);
        }
        return set;
    }

    private Boolean parseBooleanAnswer(String raw) {
        if (!StringUtils.hasText(raw)) return null;
        String t = raw.trim().toLowerCase();
        if (t.equals("true") || t.equals("正确") || t.equals("对") || t.equals("是")) return true;
        if (t.equals("false") || t.equals("错误") || t.equals("错") || t.equals("否")) return false;
        try {
            // 尝试 JSON 布尔
            JsonNode node = MAPPER.readTree(raw);
            if (node.isBoolean()) return node.booleanValue();
        } catch (Exception ignore) {
        }
        return null;
    }

    private List<String> parseTextList(String raw) {
        if (!StringUtils.hasText(raw)) return List.of();
        try {
            // 优先 JSON 数组
            List<String> arr = MAPPER.readValue(raw, new TypeReference<List<String>>() {
            });
            List<String> norm = new ArrayList<>(arr.size());
            for (String s : arr) norm.add(normalizeText(s));
            return norm;
        } catch (Exception ignore) {
        }
        // 非 JSON，当成单个填空
        return List.of(normalizeText(raw));
    }

    private String normalizeText(String s) {
        if (s == null) return "";
        String t = s.trim();
        // 数字近似可扩展，这里仅做完全匹配（如需容差可在此加数值比较逻辑）
        return t.replaceAll("\\s+", "").toLowerCase();
    }
}