package com.aiproject.smartcampus.commons.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * @program: TeacherMaxAI
 * @description: 塔层题目数量缓存
 * @author: lk_hhh
 * @create: 2025-08-10 10:16
 **/

@Component
@RequiredArgsConstructor
@Slf4j
public class QuestionRedisCache {

    private final String REDIS_QUESTION_CACHE = "questionCache:towerFloor:";
    private final String REDIS_QUESTION_NUM_CACHE = "questionCache:towerFloor:";
    private final String REDIS_QUESTION_NUM_CACHE_NEXT = ":total";
    private final String REDIS_IS_CREATE_TAG_CACHE = "isCreateTagCache:towerFloor:";
    private final String REDIS_QUESTION_DIFFICULTY_LIST_CACHE = "difficultyListCache:towerFloor:";
    private final String REDIS_TOTAL_QUESTION_DIFFICULTY_LIST_CACHE = "difficultyListCache:total:towerFloor:";

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

    /**
     * 设置题目数量
     */
    public void setQuestionNumToCache(Integer towerFloorId, Integer questionNum) {
        String key = REDIS_QUESTION_NUM_CACHE + towerFloorId + REDIS_QUESTION_NUM_CACHE_NEXT;
        stringRedisTemplate.opsForValue().set(key, String.valueOf(questionNum));
    }

    /**
     * 变化题目数量
     */
    public void deCressQuestionNumInCache(Integer towerFloorId, Integer deCressCount) {
        String key = REDIS_QUESTION_NUM_CACHE + towerFloorId + REDIS_QUESTION_NUM_CACHE_NEXT;
        stringRedisTemplate.opsForValue().increment(key, deCressCount);

    }

    /**
     * 设置题目集合
     */
    public void setQuestionToCache(Integer towerFloorId, List<Integer> questionIds) {
        String key = REDIS_QUESTION_CACHE + towerFloorId;
        List<String> list = questionIds.stream().map(Object::toString).toList();
        stringRedisTemplate.opsForList().rightPushAll(key, list);

    }

    /**
     * 获取题目
     */
    public String getQuestionIdInCache(Integer towerFloorId) {
        String key = REDIS_QUESTION_CACHE + towerFloorId;
        return stringRedisTemplate.opsForList().leftPop(key);

    }

    /**
     * 新增单个题目
     */
    public void setQuestionIdsInCache(Integer towerFloorId, String questionId) {

        String key = REDIS_QUESTION_CACHE + towerFloorId;
        stringRedisTemplate.opsForList().rightPush(key, questionId);

    }

    /**
     * 获取题目数量
     */
    public String getQuestionNumInCache(Integer towerFloorId) {
        String key = REDIS_QUESTION_NUM_CACHE + towerFloorId + REDIS_QUESTION_NUM_CACHE_NEXT;
        return stringRedisTemplate.opsForValue().get(key);
    }

    /**
     * 获取当前塔层的初始化状态(0/1)
     */
    public String isCreateTagInCache(Integer towerFloorId) {
        String key = REDIS_IS_CREATE_TAG_CACHE + towerFloorId;
        return stringRedisTemplate.opsForValue().get(key);
    }

    /**
     * 设置当前塔层的初始化状态
     */
    public void setCreateTagToCache(Integer towerFloorId, String createTag) {
        String key = REDIS_IS_CREATE_TAG_CACHE + towerFloorId;
        //如果状态是一样的就跳过
        if (stringRedisTemplate.opsForValue().get(key).equals(createTag)) {
            return;
        }
        stringRedisTemplate.opsForValue().set(key, createTag);

    }

    /**
     * 缓存当前塔层题目分配难度(0/1/2)
     */
    public void setQuestionDifficultyListInCache(Integer towerFloorId, List<String> questionDifficultyList) {

        String key = REDIS_QUESTION_DIFFICULTY_LIST_CACHE + towerFloorId;

        stringRedisTemplate.opsForList().rightPushAll(key, questionDifficultyList);

    }

    /**
     * 从 Redis 队列中批量弹出最多 count 个元素，实际不足返回剩余所有
     */
    public List<String> getQuestionDifficultyListInCache(Integer towerFloorId, Integer count) {
        int popCount = (count == null || count <= 0) ? 1 : count;
        String key = "question_difficulty_list:" + towerFloorId;

        // 执行Lua脚本
        List<String> result = stringRedisTemplate.execute(
                BATCH_LPOP_SCRIPT,
                Collections.singletonList(key),
                String.valueOf(popCount)
        );

        return result == null ? Collections.emptyList() : result;
    }

    /**
     * 存入总难度列表便于用户随机选取题目
     */
    public void setTotalQuestionDifficultyInCache(Integer towerFloorId, List<String> totalQuestionDifficulty) {

        String key = REDIS_TOTAL_QUESTION_DIFFICULTY_LIST_CACHE + towerFloorId;

        stringRedisTemplate.opsForList().rightPushAll(key, totalQuestionDifficulty);

    }

    /**
     * 获取总难度列表
     */
    public List<String> getTotalQuestionDifficultyListInCache(Integer towerFloorId) {

        String key = REDIS_TOTAL_QUESTION_DIFFICULTY_LIST_CACHE + towerFloorId;
        List<String> range = stringRedisTemplate.opsForList().leftPop(key, 1);

        return range == null ? Collections.emptyList() : range;
    }

    /**
     * 查看所有题目并不取出
     */
    public List<String> getQuestionDifficultyListInCacheAndNotPushInCache(Integer towerFloorId) {

        String key = REDIS_QUESTION_CACHE + towerFloorId;
        List<String> range = stringRedisTemplate.opsForList().range(key, 0, -1);

        return range == null ? Collections.emptyList() : range;
    }


}