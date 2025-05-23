package com.aiproject.smartcampus.commons.utils;

import cn.hutool.core.lang.Assert;
import com.aiproject.smartcampus.exception.UserExpection;
import org.redisson.api.RLock;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import static com.aiproject.smartcampus.contest.CommonContest.REDO_SUM;

/**
 * @program: SmartCampus
 * @description: 锁工具
 * @author: lk
 * @create: 2025-05-23 19:21
 **/

public class LockUtils {

    public static boolean Redo(RLock lock) throws InterruptedException {
        for (int i=0;i<REDO_SUM;i++){
            boolean isok = lock.tryLock(5, 30, TimeUnit.SECONDS);
            if (isok){
                return true;
            }

        }
        return false;
    }

    /**
     * 尝试获取锁并执行幂等检查，如果失败则按照重试策略继续尝试。
     *
     * @param lock           RedissonClient.getLock(...) 获取的 RLock 实例
     * @param maxAttempts    最大重试次数（包含第一次尝试）
     * @param initialDelayMs 初始等待时长（毫秒）
     * @param maxDelayMs     最大等待时长（毫秒）
     * @param work           真正要执行的业务操作（传入一个 Lambda，返回结果）
     * @param <T>            返回类型
     * @return 业务操作返回值
     */
    public static <T> T doWithLockAndRetry(
            RLock lock,
            int maxAttempts,
            long initialDelayMs,
            long maxDelayMs,
            LockWork<T> work) {

        Assert.isTrue(maxAttempts > 0, "maxAttempts must be >= 1");
        long delay = initialDelayMs;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            boolean acquired = false;
            try {
                // 尝试获取锁（超时 5s，自动过期 30s）
                acquired = lock.tryLock(5, 30, TimeUnit.SECONDS);
                if (acquired) {
                    // 拿到锁后，调用业务逻辑
                    return work.run();
                } else {
                    throw new UserExpection("锁获取超时");
                }
            } catch (UserExpection ue) {
                // 如果是业务幂等抛出，也直接往外抛，不重试
                throw ue;
            } catch (Exception e) {
                // 第 attempt 次尝试失败
                if (attempt == maxAttempts) {
                    // 最后一次，抛出异常
                    throw new UserExpection("系统繁忙，请稍后重试");
                }
                // 计算下一次等待时间：指数回退 + 随机抖动
                long jitter = ThreadLocalRandom.current().nextLong(delay / 2);
                long sleepTime = Math.min(delay + jitter, maxDelayMs);
                try {
                    TimeUnit.MILLISECONDS.sleep(sleepTime);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new UserExpection("线程被中断，请重试");
                }
                // 指数回退：每次乘 2
                delay = Math.min(delay * 2, maxDelayMs);
            } finally {
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            }
        }
        // 理论不会走到这里
        throw new UserExpection("锁重试失败");
    }

    /**
     * 业务执行接口
     */
    @FunctionalInterface
    public interface LockWork<T> {
        T run() throws Exception;
    }


}
