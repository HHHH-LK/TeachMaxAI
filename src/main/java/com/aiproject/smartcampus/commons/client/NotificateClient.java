package com.aiproject.smartcampus.commons.client;

import com.aiproject.smartcampus.commons.utils.BlockingMap;
import com.aiproject.smartcampus.mapper.NotificateMapper;
import com.aiproject.smartcampus.pojo.po.Notificate;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.xiaoymin.knife4j.core.util.StrUtil;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.units.qual.N;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static com.aiproject.smartcampus.contest.CommonContest.NOTIFICATION_KEY;

/**
 * @program: SmartCampus
 * @description: 消息实时通知客户端
 * @author: lk_hhh
 * @create: 2025-06-11 10:37
 **/

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificateClient {

    private final StringRedisTemplate stringRedisTemplate;
    private final NotificateMapper notificateMapper;
    private final RedissonClient redissonClient;
    private final Map<String, BlockingQueue<Notificate>> notificateMap = new ConcurrentHashMap<>(100);
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final ScheduledExecutorService scheduledExecutor = Executors.newSingleThreadScheduledExecutor();
    private volatile Integer code = 0;
    private volatile boolean running = true;


    public void produce(Notificate notificate) {
        // 把 notificate 放到本地内存队列的任务
        Runnable task = () -> {
            try {
                Integer receiverId = notificate.getReceiverId();
                if (receiverId == null) {
                    // 获取或创建队列
                    BlockingQueue<Notificate> blockingQueue = notificateMap.computeIfAbsent("all",
                            k -> new LinkedBlockingQueue<>());
                    blockingQueue.put(notificate);
                } else {
                    // 获取或创建队列
                    BlockingQueue<Notificate> blockingQueue = notificateMap.computeIfAbsent(String.valueOf(receiverId),
                            k -> new LinkedBlockingQueue<>());
                    blockingQueue.put(notificate);
                }
                log.info("Produced notification into queue: {}", notificate);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Interrupted while putting notification into queue", e);
            }
        };
        // 提交给线程池异步执行
        executor.submit(task);
    }

    /**
     * 消息读取器 （使用定时任务）
     */
    @PostConstruct
    public void initNotificationReader() {
        log.info("通知读取器已经启动");
        // 使用定时任务每20分钟读取一次数据库
        scheduledExecutor.scheduleWithFixedDelay(this::readNotifications, 0, 20, TimeUnit.MINUTES);
    }

    //获取通知任务
    //todo 需要去重处理
    private void readNotifications() {
        if (!running) {
            return;
        }
        RLock lock = redissonClient.getLock("smartcampus:notificate:lock");
        try {
            boolean acquired = lock.tryLock(5, 10, TimeUnit.SECONDS);
            if (!acquired) {
                log.warn("无法获取锁，跳过本次通知读取");
                return;
            }
            LambdaQueryWrapper<Notificate> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Notificate::getIsRead, 0);
            List<Notificate> notificates = notificateMapper.selectList(queryWrapper);
            log.info("未读通知数量: {}", notificates.size());
            if (!notificates.isEmpty()) {
                int counter = 0;
                for (Notificate notificate : notificates) {
                    if (notificate == null) {
                        log.warn("通知对象为null，跳过");
                        continue;
                    }
                    Integer receiverId = notificate.getReceiverId();
                    String content = notificate.getContent();
                    if (StrUtil.isBlank(content)) {
                        log.warn("通知内容为空，跳过: {}", notificate.getId());
                        continue;
                    }
                    log.info("处理通知 - receiverId: {}, content: {}", receiverId, content);
                    if (receiverId == null) {
                        //进行判重处理
                        Map<Object, Object> entries = stringRedisTemplate.opsForHash().entries(NOTIFICATION_KEY + "all");
                        Map<String, String> collect = entries.entrySet().stream().collect(
                                Collectors.toMap(
                                        a -> a.toString(),
                                        b -> b.toString()
                                )
                        );
                        if(collect.containsValue(content)) {
                            continue;
                        }
                        // 发送给所有用户
                        stringRedisTemplate.opsForHash().put(
                                NOTIFICATION_KEY + "all",
                                String.valueOf(++counter),
                                content
                        );
                    } else {
                        // 发送给特定用户
                        String existingContent = (String) stringRedisTemplate.opsForHash().get(
                                NOTIFICATION_KEY + "one",
                                String.valueOf(receiverId)
                        );
                        String newContent;
                        if (StrUtil.isNotBlank(existingContent)) {
                            //判重处理
                            String con = (String) stringRedisTemplate.opsForHash().get(NOTIFICATION_KEY + "one", String.valueOf(receiverId));
                            if(con.equals(existingContent)) {
                                continue;
                            }
                            // 组装多条消息
                            newContent = content + "^" + existingContent;
                        } else {
                            newContent = content;
                        }
                        stringRedisTemplate.opsForHash().put(
                                NOTIFICATION_KEY + "one",
                                String.valueOf(receiverId),
                                newContent
                        );
                    }
                    // 添加到阻塞队列
                    produce(notificate);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("读取通知时被中断", e);
        } catch (Exception e) {
            log.error("读取通知时发生异常", e);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 发送信息客户端
     */
    public Notificate getNotificate(String userId) {
        RLock lock = redissonClient.getLock("smartcampus:notificate:send:lock");
        try {
            boolean acquired = lock.tryLock(5, 10, TimeUnit.SECONDS);
            if (!acquired) {
                log.error("获取发送锁失败");
                throw new RuntimeException("系统繁忙，请稍后再试");
            }
            BlockingQueue<Notificate> userQueue = notificateMap.get(userId);
            BlockingQueue<Notificate> allQueue = notificateMap.get("all");
            // 使用轮询算法选择队列
            return chooseNotificationToSend(userQueue, allQueue);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("发送信息时被中断", e);
            throw new RuntimeException("发送信息失败");
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    private Notificate chooseNotificationToSend(BlockingQueue<Notificate> userQueue, BlockingQueue<Notificate> allQueue) {
        synchronized (this) {
            code++;
            // 优先从用户专用队列获取
            if (code % 2 == 0 && userQueue != null && !userQueue.isEmpty()) {
                return userQueue.poll();
            }
            // 从全局队列获取
            else if (allQueue != null && !allQueue.isEmpty()) {
                return allQueue.poll();
            }
            // 如果轮到用户队列但为空，则从全局队列获取
            else if (userQueue != null && !userQueue.isEmpty()) {
                return userQueue.poll();
            }
            return null; // 没有可用的通知
        }
    }

    /**
     * 优雅关闭资源
     */
    @PreDestroy
    public void shutdown() {
        running = false;
        log.info("正在关闭通知客户端...");
        // 关闭定时任务
        scheduledExecutor.shutdown();
        try {
            if (!scheduledExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduledExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduledExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        // 关闭线程池
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        log.info("通知客户端已关闭");
    }
}