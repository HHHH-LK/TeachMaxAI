package com.aiproject.smartcampus.store;

import cn.hutool.core.util.StrUtil;
import com.aiproject.smartcampus.exception.MemoryExpection;
import com.aiproject.smartcampus.handler.memorystorehandler.handlerImpl.MemoryDataBaseMemoryStoreHandler;
import com.aiproject.smartcampus.handler.memorystorehandler.handlerImpl.MemoryIdMemoryStoreHandler;
import com.aiproject.smartcampus.mapper.AIMapper;
import com.aiproject.smartcampus.pojo.bo.CacheEntry;
import com.aiproject.smartcampus.pojo.bo.handlerentity.MemoryStoreHandlerResponse;
import com.aiproject.smartcampus.pojo.bo.handlerentity.MemoryStoreHandlerquery;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.memory.ChatMemory;
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
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static com.aiproject.smartcampus.contest.CommonContest.REDIS_KEY_PREFIX;

/**
 * @program: lecture-langchain-20250525
 * @description: 本地数据库存储消息
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

    @Override
    public List<ChatMessage> getMessages(Object id) {
        readWriteLock.readLock().lock();
        try {
            String key = String.valueOf(id);
            boolean isHaveKey = localCache.containsKey(key);
            if (!isHaveKey) {
                String chatMemoryStoreKey = createChatMemoryStoreKey(key);
                CacheEntry cacheEntry = chatMemoryRedisTemplate.opsForValue().get(chatMemoryStoreKey);
                if (cacheEntry == null || StrUtil.isBlank(cacheEntry.toString())) {
                    log.info("缓存未命中且Redis中无数据，key: {}", chatMemoryStoreKey);
                    CacheEntry newEntry = new CacheEntry(new LinkedList<>());
                    localCache.put(key, newEntry);
                    return newEntry.getMessages();
                }
                log.info("缓存未命中，从Redis加载数据成功，key: {}, 消息数量: {}", chatMemoryStoreKey, cacheEntry.getMessages().size());
                localCache.put(key, cacheEntry);
                return cacheEntry.getMessages();
            }
            log.debug("缓存命中，key: {}, 消息数量: {}", key, localCache.get(key).getMessages().size());
            return localCache.get(key).getMessages();
        } catch (Exception e) {
            log.error("获取会话信息失败，id: {}", id, e);
            throw new RuntimeException("获取会话信息失败", e);
        } finally {
            readWriteLock.readLock().unlock();
        }
    }

    @Override
    public void updateMessages(Object id, List<ChatMessage> newMessages) {
        readWriteLock.writeLock().lock();
        try {
            String key = String.valueOf(id);
            CacheEntry entry = localCache.get(key);

            if (entry == null) {
                String redisKey = createChatMemoryStoreKey(key);
                CacheEntry redisEntry = chatMemoryRedisTemplate.opsForValue().get(redisKey);

                List<ChatMessage> combined = new ArrayList<>();
                if (redisEntry != null && !StrUtil.isBlank(redisEntry.toString())) {
                    combined.addAll(redisEntry.getMessages());
                }
                combined.addAll(newMessages);
                List<ChatMessage> limited = limitMessages(combined);

                entry = new CacheEntry(limited);
                localCache.put(key, entry);

                log.info("初始化缓存，key: {}，消息数量（Redis数据+新增）: {}", key, limited.size());
            } else {
                List<ChatMessage> merged = new ArrayList<>(entry.getMessages());
                merged.addAll(newMessages);
                List<ChatMessage> limited = limitMessages(merged);
                entry.setMessages(limited);

                log.debug("更新缓存，key: {}，更新后消息数量: {}", key, limited.size());
            }

            asyncWriteToRedis(key, entry.getMessages());

            if (localCache.size() >= MAX_LEN) {
                log.info("本地缓存数量达到阈值 {}, 开始驱逐旧缓存", MAX_LEN);
                evictOldestEntries();
            }

        } catch (Exception e) {
            log.error("更新聊天缓存失败，id: {}", id, e);
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
            String redisKey = createChatMemoryStoreKey(key);
            chatMemoryRedisTemplate.delete(redisKey);
            log.info("删除会话缓存，内存ID: {}，Redis键: {}", id, redisKey);
        } catch (Exception e) {
            log.error("删除会话缓存失败，内存ID: {}", id, e);
            throw new RuntimeException("删除会话缓存失败", e);
        } finally {
            readWriteLock.writeLock().unlock();
        }
    }

    private void evictOldestEntries() {
        log.info("开始驱逐旧缓存，当前缓存大小: {}", localCache.size());

        localCache.entrySet().removeIf(entry -> {
            if (entry.getValue().isExpired()) {
                if (entry.getValue().isDirty()) {
                    log.debug("驱逐过期且脏数据，key: {}", entry.getKey());
                    asyncWriteToRedis(entry.getKey(), entry.getValue().getMessages());
                }
                return true;
            }
            return false;
        });

        if (localCache.size() >= MAX_CACHE_SIZE) {
            localCache.entrySet().stream()
                    .sorted(Map.Entry.comparingByValue(
                            (a, b) -> Long.compare(a.timestamp, b.timestamp)))
                    .limit(localCache.size() - MAX_CACHE_SIZE + 100)
                    .forEach(entry -> {
                        if (entry.getValue().isDirty()) {
                            log.debug("驱逐旧脏数据，key: {}", entry.getKey());
                            asyncWriteToRedis(entry.getKey(), entry.getValue().getMessages());
                        }
                        localCache.remove(entry.getKey());
                    });
        }
    }


    //限制长度
    private List<ChatMessage> limitMessages(List<ChatMessage> merged) {

        if (merged.size() <= MAX_MESSAGES_PER_SESSION) {
            return new ArrayList<>(merged);
        }
        return new ArrayList<>(merged.subList(
                merged.size() - MAX_MESSAGES_PER_SESSION,
                merged.size()
        ));
    }

    /**
     * 定期清理过期缓存（可以通过@Scheduled注解定期执行）
     */
    public void cleanupExpiredCache() {
        readWriteLock.writeLock().lock();
        try {
            localCache.entrySet().removeIf(entry -> {
                if (entry.getValue().isExpired()) {
                    // 写回脏数据
                    if (entry.getValue().isDirty()) {
                        asyncWriteToRedis(entry.getKey(), entry.getValue().getMessages());
                    }
                    return true;
                }
                return false;
            });

            log.debug("Cleaned up expired cache entries, remaining: {}", localCache.size());

        } finally {
            readWriteLock.writeLock().unlock();
        }
    }

    //异步处理缓存
    private void asyncWriteToRedis(String key, List<ChatMessage> messages) {
        try {
            CompletableFuture.runAsync(() -> {
                CacheEntry cacheEntry = new CacheEntry(messages);
                chatMemoryRedisTemplate.opsForValue().set(key, cacheEntry, REDIS_TTL);
                //避免反复加载
                CacheEntry entry = localCache.get(key);
                if (entry != null) {
                    entry.markClean();
                }
            }, memoryStoreTaskExecutor);
        } catch (Exception e) {
            log.error("异步写入memory错误");
        }

    }

    /**
     * 强制写回所有脏数据到Redis
     */
    public void flushDirtyData() {
        readWriteLock.readLock().lock();
        try {
            localCache.entrySet().parallelStream()
                    .filter(entry -> entry.getValue().isDirty())
                    .forEach(entry -> {
                        asyncWriteToRedis(entry.getKey(), entry.getValue().getMessages());
                    });

            log.info("数据已经写回到redis中");

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
            stats.put("dirtyEntries", localCache.values().stream()
                    .mapToInt(entry -> entry.isDirty() ? 1 : 0).sum());
            stats.put("expiredEntries", localCache.values().stream()
                    .mapToInt(entry -> entry.isExpired() ? 1 : 0).sum());
            return stats;
        } finally {
            readWriteLock.readLock().unlock();
        }
    }

    public  String createChatMemoryStoreKey(String key){

        return REDIS_KEY_PREFIX+key;


    }


}

