package com.aiproject.smartcampus.commons.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * @program: TeacherMaxAI
 * @description: 塔层题目与难度的缓存
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class QuestionRedisCache {

    // 题目列表（存放题目ID队列）
    private static final String KEY_QUESTION_LIST_PREFIX = "questionCache:towerFloor:";      // + floorId
    // 题目数量
    private static final String KEY_QUESTION_NUM_PREFIX = "questionCache:towerFloor:";       // + floorId + ":total"
    private static final String KEY_QUESTION_NUM_SUFFIX = ":total";
    // 初始化标记
    private static final String KEY_INIT_TAG_PREFIX = "isCreateTagCache:towerFloor:";        // + floorId
    // 难度消费队列（用于按顺序弹出下一批难度）
    private static final String KEY_DIFF_QUEUE_PREFIX = "difficultyListCache:towerFloor:";   // + floorId
    // 难度总列表（仅用于查看/调试）
    private static final String KEY_DIFF_TOTAL_PREFIX = "difficultyListCache:total:towerFloor:"; // + floorId

    private static final DefaultRedisScript<List> BATCH_LPOP_SCRIPT;

    static {
        BATCH_LPOP_SCRIPT = new DefaultRedisScript<>();
        BATCH_LPOP_SCRIPT.setScriptText(
                "local key = KEYS[1]\n" +
                        "local count = tonumber(ARGV[1])\n" +
                        "local result = {}\n" +
                        "local len = redis.call('LLEN', key)\n" +
                        "if len == 0 then\n" +
                        "    return result\n" +
                        "end\n" +
                        "local n = math.min(count, len)\n" +
                        "for i = 1, n do\n" +
                        "    local val = redis.call('LPOP', key)\n" +
                        "    table.insert(result, val)\n" +
                        "end\n" +
                        "return result"
        );
        BATCH_LPOP_SCRIPT.setResultType(List.class);
    }

    private final StringRedisTemplate stringRedisTemplate;

    private String keyQuestionList(Integer floorId) {
        return KEY_QUESTION_LIST_PREFIX + floorId;
    }

    private String keyQuestionNum(Integer floorId) {
        return KEY_QUESTION_NUM_PREFIX + floorId + KEY_QUESTION_NUM_SUFFIX;
    }

    private String keyInitTag(Integer floorId) {
        return KEY_INIT_TAG_PREFIX + floorId;
    }

    private String keyDiffQueue(Integer floorId) {
        return KEY_DIFF_QUEUE_PREFIX + floorId;
    }

    private String keyDiffTotal(Integer floorId) {
        return KEY_DIFF_TOTAL_PREFIX + floorId;
    }

    // ========== 题目数量 ==========
    public void setQuestionNumToCache(Integer floorId, Integer questionNum) {
        stringRedisTemplate.opsForValue().set(keyQuestionNum(floorId), String.valueOf(questionNum));
    }

    // 注意：此处是增加数量（新增了多少题目）
    public void incrQuestionNumInCache(Integer floorId, Integer incrCount) {
        stringRedisTemplate.opsForValue().increment(keyQuestionNum(floorId), incrCount);
    }

    public String getQuestionNumInCache(Integer floorId) {
        return stringRedisTemplate.opsForValue().get(keyQuestionNum(floorId));
    }

    // ========== 题目列表 ==========
    public void setQuestionToCache(Integer floorId, List<Integer> questionIds) {
        if (questionIds == null || questionIds.isEmpty()) return;
        List<String> list = questionIds.stream().map(Object::toString).toList();
        stringRedisTemplate.opsForList().rightPushAll(keyQuestionList(floorId), list);
    }

    public String getQuestionIdInCache(Integer floorId) {
        return stringRedisTemplate.opsForList().leftPop(keyQuestionList(floorId));
    }

    public void setQuestionIdsInCache(Integer floorId, String questionId) {
        stringRedisTemplate.opsForList().rightPush(keyQuestionList(floorId), questionId);
    }

    public List<String> peekAllQuestionIds(Integer floorId) {
        List<String> range = stringRedisTemplate.opsForList().range(keyQuestionList(floorId), 0, -1);
        return range == null ? List.of() : range;
    }

    // ========== 初始化标记 ==========
    public String isCreateTagInCache(Integer floorId) {
        return stringRedisTemplate.opsForValue().get(keyInitTag(floorId));
    }

    public void setCreateTagToCache(Integer floorId, String createTag) {
        String key = keyInitTag(floorId);
        String old = stringRedisTemplate.opsForValue().get(key);
        if (Objects.equals(old, createTag)) {
            return;
        }
        stringRedisTemplate.opsForValue().set(key, createTag);
    }

    // ========== 难度列表 ==========
    // 消费队列：用于按序弹出下一批难度
    public void setQuestionDifficultyListInCache(Integer floorId, List<String> difficultyList) {
        if (difficultyList == null || difficultyList.isEmpty()) return;
        stringRedisTemplate.opsForList().rightPushAll(keyDiffQueue(floorId), difficultyList);
    }

    // 从“消费队列”里批量弹出
    public List<String> popDifficultyListInCache(Integer floorId, Integer count) {
        int popCount = (count == null || count <= 0) ? 1 : count;
        List<String> result = stringRedisTemplate.execute(
                BATCH_LPOP_SCRIPT,
                Collections.singletonList(keyDiffQueue(floorId)),
                String.valueOf(popCount)
        );
        return result == null ? List.of() : result;
    }

    // 保存“总难度列表”（仅查看/调试）
    public void setTotalQuestionDifficultyInCache(Integer floorId, List<String> totalDifficultyList) {
        if (totalDifficultyList == null || totalDifficultyList.isEmpty()) return;
        stringRedisTemplate.opsForList().rightPushAll(keyDiffTotal(floorId), totalDifficultyList);
    }

    public List<String> getTotalQuestionDifficultyListInCache(Integer floorId) {
        List<String> range = stringRedisTemplate.opsForList().range(keyDiffTotal(floorId), 0, -1);
        return range == null ? List.of() : range;
    }
}