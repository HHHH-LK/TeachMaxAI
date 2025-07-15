package com.aiproject.smartcampus.model.store;

import cn.hutool.core.util.StrUtil;
import com.aiproject.smartcampus.pojo.bo.CacheEntry;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static com.aiproject.smartcampus.contest.CommonContest.REDIS_KEY_PREFIX;

/**
 * @program: lecture-langchain-20250525
 * @description: 本地数据库存储消息（带Redis降级机制+快速同步）
 * @author: lk
 * @create: 2025-05-11 10:15
 **/

@Component
@Slf4j
@RequiredArgsConstructor
public class LocalStore implements ChatMemoryStore {

    private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    private final RedisTemplate<String, CacheEntry> chatMemoryRedisTemplate;
    // 本地缓存：提高读取性能
    private final Map<String, CacheEntry> localCache = new ConcurrentHashMap<>();
    private final Integer MAX_LEN = 200;
    private static final Duration REDIS_TTL = Duration.ofDays(7);
    private static final int MAX_MESSAGES_PER_SESSION = 200;
    private static final int MAX_CACHE_SIZE = 1000;
    private final Executor memoryStoreTaskExecutor;

    // Redis连接状态管理
    private final AtomicBoolean redisAvailable = new AtomicBoolean(true);
    private volatile long lastRedisCheck = 0;
    private static final long REDIS_CHECK_INTERVAL = 30000; // 30秒检查一次Redis状态

    // 快速同步配置
    private static final int IMMEDIATE_SYNC_THRESHOLD = 5; // 超过5条消息立即同步
    private static final long FORCE_SYNC_INTERVAL = 10000; // 10秒强制同步一次

    @Override
    public List<ChatMessage> getMessages(Object id) {
        readWriteLock.readLock().lock();
        try {
            String key = String.valueOf(id);

            // 首先检查本地缓存
            CacheEntry localEntry = localCache.get(key);
            if (localEntry != null) {
                log.debug("缓存命中，key: {}, 消息数量: {}", key, localEntry.getMessages().size());
                return new ArrayList<>(localEntry.getMessages());
            }

            // 本地缓存未命中，尝试从Redis加载
            CacheEntry redisEntry = loadFromRedis(key);
            if (redisEntry != null) {
                localCache.put(key, redisEntry);
                log.info("从Redis加载数据成功，key: {}, 消息数量: {}", key, redisEntry.getMessages().size());
                return new ArrayList<>(redisEntry.getMessages());
            }

            // Redis也没有数据，创建新的空条目
            log.info("未找到任何数据，创建新的会话，key: {}", key);
            CacheEntry newEntry = new CacheEntry(new LinkedList<>());
            localCache.put(key, newEntry);
            return new ArrayList<>(newEntry.getMessages());

        } catch (Exception e) {
            log.error("获取会话信息失败，id: {}，使用空消息列表降级", id, e);
            // 降级：返回空消息列表
            return new ArrayList<>();
        } finally {
            readWriteLock.readLock().unlock();
        }
    }

    @Override
    public void updateMessages(Object id, List<ChatMessage> newMessages) {
        if (newMessages == null || newMessages.isEmpty()) {
            log.debug("新消息为空，跳过更新，id: {}", id);
            return;
        }

        readWriteLock.writeLock().lock();
        try {
            String key = String.valueOf(id);
            CacheEntry entry = localCache.get(key);

            if (entry == null) {
                // 本地缓存不存在，尝试从Redis加载后合并
                CacheEntry redisEntry = loadFromRedis(key);
                List<ChatMessage> combined = new ArrayList<>();

                if (redisEntry != null) {
                    combined.addAll(redisEntry.getMessages());
                }
                combined.addAll(newMessages);
                List<ChatMessage> limited = limitMessages(combined);

                entry = new CacheEntry(limited);
                localCache.put(key, entry);
                log.info("初始化缓存，key: {}，消息数量: {}", key, limited.size());
            } else {
                // 本地缓存存在，直接合并
                List<ChatMessage> merged = new ArrayList<>(entry.getMessages());
                merged.addAll(newMessages);
                List<ChatMessage> limited = limitMessages(merged);
                entry.setMessages(limited);
                entry.markDirty();

                log.debug("更新缓存，key: {}，新增消息: {}，总消息数: {}",
                        key, newMessages.size(), limited.size());
            }

            // 快速同步策略
            if (shouldImmediateSync(entry)) {
                log.debug("触发立即同步，key: {}", key);
                immediateSync(key, entry.getMessages());
            } else {
                // 异步同步到Redis
                asyncWriteToRedis(key, entry.getMessages());
            }

            // 缓存清理
            if (localCache.size() >= MAX_LEN) {
                log.info("本地缓存数量达到阈值 {}, 开始清理", MAX_LEN);
                evictOldestEntries();
            }

        } catch (Exception e) {
            log.error("更新聊天缓存失败，id: {}，但本地缓存已更新", id, e);
            // 不抛出异常，确保本地缓存更新成功
        } finally {
            readWriteLock.writeLock().unlock();
        }
    }

    @Override
    public void deleteMessages(Object id) {
        String key = String.valueOf(id);
        readWriteLock.writeLock().lock();
        try {
            localCache.remove(key);

            // 尝试从Redis删除
            if (isRedisAvailable()) {
                try {
                    String redisKey = createChatMemoryStoreKey(key);
                    chatMemoryRedisTemplate.delete(redisKey);
                    log.info("删除会话缓存成功，内存ID: {}，Redis键: {}", id, redisKey);
                } catch (Exception e) {
                    log.warn("从Redis删除会话缓存失败，但本地缓存已删除，id: {}, error: {}", id, e.getMessage());
                    markRedisUnavailable();
                }
            } else {
                log.info("Redis不可用，仅删除本地缓存，id: {}", id);
            }

        } catch (Exception e) {
            log.error("删除会话缓存失败，id: {}", id, e);
            // 不抛出异常，确保不影响上层业务
        } finally {
            readWriteLock.writeLock().unlock();
        }
    }

    /**
     * 安全地从Redis加载数据
     */
    private CacheEntry loadFromRedis(String key) {
        if (!isRedisAvailable()) {
            return null;
        }

        try {
            String redisKey = createChatMemoryStoreKey(key);
            CacheEntry cacheEntry = chatMemoryRedisTemplate.opsForValue().get(redisKey);

            if (cacheEntry != null && !StrUtil.isBlank(cacheEntry.toString())) {
                return cacheEntry;
            }
            return null;

        } catch (Exception e) {
            log.warn("从Redis加载数据失败，key: {}, error: {}", key, e.getMessage());
            markRedisUnavailable();
            return null;
        }
    }

    /**
     * 判断是否需要立即同步
     */
    private boolean shouldImmediateSync(CacheEntry entry) {
        if (!isRedisAvailable()) {
            return false;
        }

        // 消息数量超过阈值
        if (entry.getMessages().size() >= IMMEDIATE_SYNC_THRESHOLD) {
            return true;
        }

        // 距离上次同步时间过长
        if (System.currentTimeMillis() - entry.getTimestamp() > FORCE_SYNC_INTERVAL) {
            return true;
        }

        return false;
    }

    /**
     * 立即同步到Redis
     */
    private void immediateSync(String key, List<ChatMessage> messages) {
        try {
            String redisKey = createChatMemoryStoreKey(key);
            CacheEntry cacheEntry = new CacheEntry(messages);
            chatMemoryRedisTemplate.opsForValue().set(redisKey, cacheEntry, REDIS_TTL);

            // 标记为已同步
            CacheEntry localEntry = localCache.get(key);
            if (localEntry != null) {
                localEntry.markClean();
            }

            log.debug("立即同步成功，key: {}", key);

        } catch (Exception e) {
            log.warn("立即同步失败，key: {}, error: {}", key, e.getMessage());
            markRedisUnavailable();
        }
    }

    /**
     * 检查Redis是否可用
     */
    private boolean isRedisAvailable() {
        long now = System.currentTimeMillis();

        // 如果最近检查过且Redis不可用，且还没到重新检查时间
        if (!redisAvailable.get() && (now - lastRedisCheck) < REDIS_CHECK_INTERVAL) {
            return false;
        }

        // 如果到了重新检查时间，尝试检查Redis状态
        if ((now - lastRedisCheck) > REDIS_CHECK_INTERVAL) {
            checkRedisHealth();
        }

        return redisAvailable.get();
    }

    /**
     * 检查Redis健康状态
     */
    private void checkRedisHealth() {
        try {

            if (!redisAvailable.get()) {
                log.info("Redis连接已恢复");
                redisAvailable.set(true);
            }

        } catch (Exception e) {
            if (redisAvailable.get()) {
                log.warn("Redis连接不可用: {}", e.getMessage());
                redisAvailable.set(false);
            }
        } finally {
            lastRedisCheck = System.currentTimeMillis();
        }
    }

    /**
     * 标记Redis不可用
     */
    private void markRedisUnavailable() {
        if (redisAvailable.get()) {
            log.warn("标记Redis为不可用状态");
            redisAvailable.set(false);
            lastRedisCheck = System.currentTimeMillis();
        }
    }

    private void evictOldestEntries() {
        log.info("开始清理缓存，当前大小: {}", localCache.size());

        // 首先清理过期的条目
        int expiredCount = 0;
        Iterator<Map.Entry<String, CacheEntry>> iterator = localCache.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, CacheEntry> entry = iterator.next();
            if (entry.getValue().isExpired()) {
                if (entry.getValue().isDirty()) {
                    log.debug("清理过期脏数据，key: {}", entry.getKey());
                    asyncWriteToRedis(entry.getKey(), entry.getValue().getMessages());
                }
                iterator.remove();
                expiredCount++;
            }
        }

        log.info("清理了{}个过期缓存条目", expiredCount);

        // 如果还是太多，按时间戳清理最老的条目
        if (localCache.size() >= MAX_CACHE_SIZE) {
            int toRemove = localCache.size() - MAX_CACHE_SIZE + 100;

            localCache.entrySet().stream()
                    .sorted(Map.Entry.comparingByValue(
                            Comparator.comparingLong(CacheEntry::getTimestamp)))
                    .limit(toRemove)
                    .forEach(entry -> {
                        if (entry.getValue().isDirty()) {
                            log.debug("清理旧脏数据，key: {}", entry.getKey());
                            asyncWriteToRedis(entry.getKey(), entry.getValue().getMessages());
                        }
                        localCache.remove(entry.getKey());
                    });

            log.info("清理了{}个最旧的缓存条目", toRemove);
        }
    }

    /**
     * 限制消息长度
     */
    private List<ChatMessage> limitMessages(List<ChatMessage> messages) {
        if (messages.size() <= MAX_MESSAGES_PER_SESSION) {
            return new ArrayList<>(messages);
        }

        List<ChatMessage> limited = new ArrayList<>(messages.subList(
                messages.size() - MAX_MESSAGES_PER_SESSION,
                messages.size()
        ));

        log.debug("消息数量超限，从{}条截取为{}条", messages.size(), limited.size());
        return limited;
    }

    /**
     * 清理过期缓存
     */
    public void cleanupExpiredCache() {
        readWriteLock.writeLock().lock();
        try {
            int cleaned = 0;
            Iterator<Map.Entry<String, CacheEntry>> iterator = localCache.entrySet().iterator();

            while (iterator.hasNext()) {
                Map.Entry<String, CacheEntry> entry = iterator.next();
                if (entry.getValue().isExpired()) {
                    // 写回脏数据
                    if (entry.getValue().isDirty()) {
                        asyncWriteToRedis(entry.getKey(), entry.getValue().getMessages());
                    }
                    iterator.remove();
                    cleaned++;
                }
            }

            if (cleaned > 0) {
                log.info("清理了{}个过期缓存条目，剩余: {}", cleaned, localCache.size());
            }

        } catch (Exception e) {
            log.error("清理过期缓存失败", e);
        } finally {
            readWriteLock.writeLock().unlock();
        }
    }

    /**
     * 异步写入Redis
     */
    private void asyncWriteToRedis(String key, List<ChatMessage> messages) {
        if (!isRedisAvailable()) {
            log.debug("Redis不可用，跳过异步写入，key: {}", key);
            return;
        }

        try {
            CompletableFuture.runAsync(() -> {
                try {
                    String redisKey = createChatMemoryStoreKey(key);
                    CacheEntry cacheEntry = new CacheEntry(messages);
                    chatMemoryRedisTemplate.opsForValue().set(redisKey, cacheEntry, REDIS_TTL);

                    // 标记为已同步
                    CacheEntry entry = localCache.get(key);
                    if (entry != null) {
                        entry.markClean();
                    }

                    log.debug("异步写入Redis成功，key: {}", key);

                } catch (Exception e) {
                    log.warn("异步写入Redis失败，key: {}, error: {}", key, e.getMessage());
                    markRedisUnavailable();
                }
            }, memoryStoreTaskExecutor);

        } catch (Exception e) {
            log.error("提交异步写入任务失败，key: {}", key, e);
        }
    }

    /**
     * 强制写回所有脏数据到Redis
     */
    public void flushDirtyData() {
        if (!isRedisAvailable()) {
           /* log.warn("Redis不可用，跳过脏数据写回");*/
            return;
        }

        readWriteLock.readLock().lock();
        try {
            int dirtyCount = 0;

            for (Map.Entry<String, CacheEntry> entry : localCache.entrySet()) {
                if (entry.getValue().isDirty()) {
                    immediateSync(entry.getKey(), entry.getValue().getMessages());
                    dirtyCount++;
                }
            }

        } catch (Exception e) {
            log.error("强制写回脏数据失败", e);
        } finally {
            readWriteLock.readLock().unlock();
        }
    }

    /**
     * 获取缓存统计信息
     */
    public Map<String, Object> getCacheStats() {
        readWriteLock.readLock().lock();
        try {
            Map<String, Object> stats = new HashMap<>();
            stats.put("cacheSize", localCache.size());
            stats.put("maxCacheSize", MAX_CACHE_SIZE);
            stats.put("redisAvailable", redisAvailable.get());
            stats.put("dirtyEntries", localCache.values().stream()
                    .mapToInt(entry -> entry.isDirty() ? 1 : 0).sum());
            stats.put("expiredEntries", localCache.values().stream()
                    .mapToInt(entry -> entry.isExpired() ? 1 : 0).sum());
            return stats;
        } finally {
            readWriteLock.readLock().unlock();
        }
    }

    public String createChatMemoryStoreKey(String key) {
        return REDIS_KEY_PREFIX + key;
    }

    /**
     * 获取Redis可用状态
     */
    public boolean getRedisStatus() {
        return redisAvailable.get();
    }

    /**
     * 手动触发Redis健康检查
     */
    public void forceRedisHealthCheck() {
        checkRedisHealth();
    }
}