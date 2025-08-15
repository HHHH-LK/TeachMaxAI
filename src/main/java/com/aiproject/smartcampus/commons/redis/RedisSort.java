package com.aiproject.smartcampus.commons.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.Set;

/**
 * @program: TeacherMaxAI
 * @description: 优化后的实时排序服务 - 线程安全且逻辑清晰
 * @author: lk_hhh
 * @create: 2025-08-08 22:36
 **/

@Component
@RequiredArgsConstructor
@Slf4j
public class RedisSort {

    private final StringRedisTemplate stringRedisTemplate;

    private static final String LEADERBOARD_KEY_PREFIX = "game:sort:towerId:";
    private static final String TOTAL_SORT_KEY = "game:sort:total";
    private static final int TOP_RANK_COUNT = 10;

    /**
     * 设置或更新课程排行榜中学生的最高塔层数（绝对值设置）
     *
     * @param towerId        塔ID
     * @param studentId      学生ID
     * @param maxFloorNumber 最高塔层数
     * @return 操作是否成功
     */
    public boolean setStudentMaxFloor(String towerId, String studentId, String maxFloorNumber) {
        if (!validateParams(towerId, studentId, maxFloorNumber)) {
            return false;
        }

        try {
            Double score = parseScore(maxFloorNumber);
            if (score == null) {
                return false;
            }

            String key = LEADERBOARD_KEY_PREFIX + towerId;
            Boolean result = stringRedisTemplate.opsForZSet().add(key, studentId, score);

            log.debug("设置课程排行榜 - courseId: {}, studentId: {}, score: {}, result: {}",
                    towerId, studentId, score, result);
            return true;

        } catch (Exception e) {
            log.error("设置课程排行榜失败 - courseId: {}, studentId: {}, maxFloorNumber: {}",
                    towerId, studentId, maxFloorNumber, e);
            return false;
        }
    }

    /**
     * 增量更新课程排行榜分数（相对值增加）
     *
     * @param towerId        塔id
     * @param studentId      学生ID
     * @param incrementScore 增量分数
     * @return 操作是否成功
     */
    public boolean incrementStudentScore(String towerId, String studentId, String incrementScore) {
        if (!validateParams(towerId, studentId, incrementScore)) {
            return false;
        }

        try {
            Double score = parseScore(incrementScore);
            if (score == null) {
                return false;
            }

            String key = LEADERBOARD_KEY_PREFIX + towerId;
            Double newScore = stringRedisTemplate.opsForZSet().incrementScore(key, studentId, score);

            log.debug("增量更新课程排行榜 - courseId: {}, studentId: {}, increment: {}, newScore: {}",
                    towerId, studentId, score, newScore);
            return newScore != null;

        } catch (Exception e) {
            log.error("增量更新课程排行榜失败 - courseId: {}, studentId: {}, incrementScore: {}",
                    towerId, studentId, incrementScore, e);
            return false;
        }
    }

    /**
     * 设置或更新总排行榜中学生的分数（绝对值设置）
     *
     * @param studentId  学生ID
     * @param totalScore 总分数
     * @return 操作是否成功
     */
    public boolean setStudentTotalScore(String studentId, String totalScore) {
        if (!validateParams("total", studentId, totalScore)) {
            return false;
        }

        try {
            Double score = parseScore(totalScore);
            if (score == null) {
                return false;
            }

            Boolean result = stringRedisTemplate.opsForZSet().add(TOTAL_SORT_KEY, studentId, score);

            log.debug("设置总排行榜 - studentId: {}, score: {}, result: {}",
                    studentId, score, result);
            return true;

        } catch (Exception e) {
            log.error("设置总排行榜失败 - studentId: {}, totalScore: {}",
                    studentId, totalScore, e);
            return false;
        }
    }

    /**
     * 增量更新总排行榜分数（相对值增加）
     *
     * @param studentId      学生ID
     * @param incrementScore 增量分数
     * @return 操作是否成功
     */
    public boolean incrementTotalScore(String studentId, String incrementScore) {
        if (!validateParams("total", studentId, incrementScore)) {
            return false;
        }

        try {
            Double score = parseScore(incrementScore);
            if (score == null) {
                return false;
            }

            Double newScore = stringRedisTemplate.opsForZSet().incrementScore(TOTAL_SORT_KEY, studentId, score);

            log.debug("增量更新总排行榜 - studentId: {}, increment: {}, newScore: {}",
                    studentId, score, newScore);
            return newScore != null;

        } catch (Exception e) {
            log.error("增量更新总排行榜失败 - studentId: {}, incrementScore: {}",
                    studentId, incrementScore, e);
            return false;
        }
    }

    /**
     * 获取课程排行榜前N名
     *
     * @param towerId 塔ID
     * @return 排行榜数据，失败时返回空集合
     */
    public Set<ZSetOperations.TypedTuple<String>> getCourseLeaderboard(String towerId) {
        return getCourseLeaderboard(towerId, TOP_RANK_COUNT);
    }

    /**
     * 获取课程排行榜前N名
     *
     * @param towerId  塔ID
     * @param topCount 获取前几名
     * @return 排行榜数据，失败时返回空集合
     */
    public Set<ZSetOperations.TypedTuple<String>> getCourseLeaderboard(String towerId, int topCount) {
        if (!StringUtils.hasText(towerId)) {
            log.warn("获取课程排行榜失败：towerId为空");
            return Collections.emptySet();
        }

        if (topCount <= 0) {
            log.warn("获取课程排行榜失败：topCount必须大于0");
            return Collections.emptySet();
        }

        try {
            String key = LEADERBOARD_KEY_PREFIX + towerId;
            Set<ZSetOperations.TypedTuple<String>> result = stringRedisTemplate.opsForZSet()
                    .reverseRangeWithScores(key, 0, topCount - 1);

            log.debug("获取塔排行榜成功 - towerId: {}, topCount: {}, resultSize: {}",
                    towerId, topCount, result != null ? result.size() : 0);

            return result != null ? result : Collections.emptySet();

        } catch (Exception e) {
            log.error("获取塔排行榜失败 - towerId: {}, topCount: {}", towerId, topCount, e);
            return Collections.emptySet();
        }
    }

    /**
     * 获取总排行榜前N名
     *
     * @return 总排行榜数据，失败时返回空集合
     */
    public Set<ZSetOperations.TypedTuple<String>> getTotalLeaderboard() {
        return getTotalLeaderboard(TOP_RANK_COUNT);
    }

    /**
     * 获取总排行榜前N名
     *
     * @param topCount 获取前几名
     * @return 总排行榜数据，失败时返回空集合
     */
    public Set<ZSetOperations.TypedTuple<String>> getTotalLeaderboard(int topCount) {
        if (topCount <= 0) {
            log.warn("获取总排行榜失败：topCount必须大于0");
            return Collections.emptySet();
        }

        try {
            Set<ZSetOperations.TypedTuple<String>> result = stringRedisTemplate.opsForZSet()
                    .reverseRangeWithScores(TOTAL_SORT_KEY, 0, topCount - 1);

            log.debug("获取总排行榜成功 - topCount: {}, resultSize: {}",
                    topCount, result != null ? result.size() : 0);

            return result != null ? result : Collections.emptySet();

        } catch (Exception e) {
            log.error("获取总排行榜失败 - topCount: {}", topCount, e);
            return Collections.emptySet();
        }
    }

    /**
     * 获取学生在课程排行榜中的排名和分数
     *
     * @param towerId   塔ID
     * @param studentId 学生ID
     * @return 学生排名信息
     */
    public StudentRankInfo getStudentCourseRank(String towerId, String studentId) {
        if (!validateParams(towerId, studentId, "1")) {
            return StudentRankInfo.empty();
        }

        try {
            String key = LEADERBOARD_KEY_PREFIX + towerId;
            Double score = stringRedisTemplate.opsForZSet().score(key, studentId);
            if (score == null) {
                return StudentRankInfo.notFound();
            }

            Long rank = stringRedisTemplate.opsForZSet().reverseRank(key, studentId);
            if (rank == null) {
                return StudentRankInfo.notFound();
            }

            return new StudentRankInfo(rank + 1, score, true); // rank从0开始，所以+1

        } catch (Exception e) {
            log.error("获取学生塔排名失败 - courseId: {}, studentId: {}", towerId, studentId, e);
            return StudentRankInfo.empty();
        }
    }

    /**
     * 获取学生在总排行榜中的排名和分数
     *
     * @param studentId 学生ID
     * @return 学生排名信息
     */
    public StudentRankInfo getStudentTotalRank(String studentId) {
        if (!StringUtils.hasText(studentId)) {
            log.warn("获取学生总排名失败：studentId为空");
            return StudentRankInfo.empty();
        }

        try {
            Double score = stringRedisTemplate.opsForZSet().score(TOTAL_SORT_KEY, studentId);
            if (score == null) {
                return StudentRankInfo.notFound();
            }

            Long rank = stringRedisTemplate.opsForZSet().reverseRank(TOTAL_SORT_KEY, studentId);
            if (rank == null) {
                return StudentRankInfo.notFound();
            }

            return new StudentRankInfo(rank + 1, score, true); // rank从0开始，所以+1

        } catch (Exception e) {
            log.error("获取学生总排名失败 - studentId: {}", studentId, e);
            return StudentRankInfo.empty();
        }
    }

    /**
     * 删除学生的课程排行榜记录
     *
     * @param towerId   塔ID
     * @param studentId 学生ID
     * @return 操作是否成功
     */
    public boolean removeStudentFromCourse(String towerId, String studentId) {
        if (!validateParams(towerId, studentId, "1")) {
            return false;
        }

        try {
            String key = LEADERBOARD_KEY_PREFIX + towerId;
            Long removed = stringRedisTemplate.opsForZSet().remove(key, studentId);

            log.debug("删除学生塔排行榜记录 - courseId: {}, studentId: {}, removed: {}",
                    towerId, studentId, removed);
            return removed != null && removed > 0;

        } catch (Exception e) {
            log.error("删除学生塔排行榜记录失败 - courseId: {}, studentId: {}", towerId, studentId, e);
            return false;
        }
    }

    /**
     * 删除学生的总排行榜记录
     *
     * @param studentId 学生ID
     * @return 操作是否成功
     */
    public boolean removeStudentFromTotal(String studentId) {
        if (!StringUtils.hasText(studentId)) {
            log.warn("删除学生总排行榜记录失败：studentId为空");
            return false;
        }

        try {
            Long removed = stringRedisTemplate.opsForZSet().remove(TOTAL_SORT_KEY, studentId);

            log.debug("删除学生总排行榜记录 - studentId: {}, removed: {}", studentId, removed);
            return removed != null && removed > 0;

        } catch (Exception e) {
            log.error("删除学生总排行榜记录失败 - studentId: {}", studentId, e);
            return false;
        }
    }

    /**
     * 参数校验
     */
    private boolean validateParams(String param1, String param2, String param3) {
        if (!StringUtils.hasText(param1)) {
            log.warn("参数校验失败：第一个参数为空");
            return false;
        }
        if (!StringUtils.hasText(param2)) {
            log.warn("参数校验失败：第二个参数为空");
            return false;
        }
        if (!StringUtils.hasText(param3)) {
            log.warn("参数校验失败：第三个参数为空");
            return false;
        }
        return true;
    }

    /**
     * 分数解析
     */
    private Double parseScore(String scoreStr) {
        try {
            double score = Double.parseDouble(scoreStr);
            if (Double.isNaN(score) || Double.isInfinite(score)) {
                log.warn("分数值无效：{}", scoreStr);
                return null;
            }
            return score;
        } catch (NumberFormatException e) {
            log.warn("分数解析失败：{}", scoreStr);
            return null;
        }
    }

    /**
     * 学生排名信息
     */
    /**
     * 学生排名信息类
     * 用于封装学生在排行榜中的排名、分数和查询状态信息
     *
     * @author lk_hhh
     * @since 2025-08-15
     */
    public static class StudentRankInfo {

        /**
         * 学生在排行榜中的排名位置
         * <p>
         * 说明：
         * - 排名从1开始计算（第1名、第2名...）
         * - 值为0表示未找到排名或查询失败
         * - 分数相同的学生会有相同的排名
         * - 例如：两个学生都是100分，他们都是第1名，下一个学生是第3名
         */
        private final long rank;

        /**
         * 学生在排行榜中的分数
         * <p>
         * 说明：
         * - 在课程排行榜中，通常表示学生在该课程中的最高塔层数
         * - 在总排行榜中，通常表示学生所有课程的总分数或总塔层数
         * - 分数越高排名越靠前（降序排列）
         * - 值为0.0表示无分数或查询失败
         */
        private final double score;

        /**
         * 查询结果状态标识
         * <p>
         * 说明：
         * - true：成功找到学生的排名信息，rank和score字段有效
         * - false：未找到学生的排名信息，可能原因：
         * 1. 学生不在该排行榜中（从未参与或已被移除）
         * 2. 查询参数错误（如courseId不存在、studentId无效）
         * 3. Redis操作异常或网络问题
         * 4. 排行榜数据为空
         */
        private final boolean found;

        /**
         * 构造函数
         *
         * @param rank  排名位置，从1开始，0表示无效
         * @param score 分数，通常为塔层数或总分
         * @param found 是否找到有效的排名信息
         */
        public StudentRankInfo(long rank, double score, boolean found) {
            this.rank = rank;
            this.score = score;
            this.found = found;
        }

        /**
         * 创建空的排名信息
         * 用于参数校验失败或异常情况
         *
         * @return 表示查询失败的StudentRankInfo对象
         */
        public static StudentRankInfo empty() {
            return new StudentRankInfo(0, 0.0, false);
        }

        /**
         * 创建"未找到"的排名信息
         * 用于学生不在排行榜中的情况
         *
         * @return 表示学生不在排行榜中的StudentRankInfo对象
         */
        public static StudentRankInfo notFound() {
            return new StudentRankInfo(0, 0.0, false);
        }

        /**
         * 获取学生排名
         *
         * @return 排名位置，1表示第一名，0表示无效排名
         */
        public long getRank() {
            return rank;
        }

        /**
         * 获取学生分数
         *
         * @return 分数值，0.0表示无分数
         */
        public double getScore() {
            return score;
        }

        /**
         * 检查是否找到有效的排名信息
         *
         * @return true表示找到有效信息，false表示未找到或查询失败
         */
        public boolean isFound() {
            return found;
        }

        /**
         * 格式化输出排名信息
         * 便于日志记录和调试
         *
         * @return 格式化的字符串，包含排名、分数和查找状态
         */
        @Override
        public String toString() {
            return String.format("StudentRankInfo{rank=%d, score=%.2f, found=%s}", rank, score, found);
        }


        /**
         * 检查是否为有效的排名（排名大于0且找到了数据）
         *
         * @return true表示有效排名
         */
        public boolean hasValidRank() {
            return found && rank > 0;
        }

        /**
         * 检查是否为有效的分数（分数大于0且找到了数据）
         *
         * @return true表示有效分数
         */
        public boolean hasValidScore() {
            return found && score > 0.0;
        }

        /**
         * 获取排名描述文本
         *
         * @return 排名的文字描述
         */
        public String getRankDescription() {
            if (!found) {
                return "未上榜";
            }
            if (rank == 0) {
                return "排名异常";
            }
            return "第" + rank + "名";
        }
    }
}