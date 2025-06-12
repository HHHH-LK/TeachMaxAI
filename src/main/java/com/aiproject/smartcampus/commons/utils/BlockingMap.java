package com.aiproject.smartcampus.commons.utils;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingQueue;

public class BlockingMap<K, V> {
    // 每个 key 对应一个内部的 BlockingQueue
    private final ConcurrentMap<K, BlockingQueue<V>> map = new ConcurrentHashMap<>();
    private final int queueCapacity;

    public BlockingMap(int queueCapacity) {
        this.queueCapacity = queueCapacity;
    }

    /**
     * 往指定 key 的队列里放入一个元素，容量满时阻塞
     */
    public void put(K key, V value) throws InterruptedException {
        // 如果不存在对应队列，则创建一个新的
        map.computeIfAbsent(key, k -> new LinkedBlockingQueue<>(queueCapacity))
                .put(value);
    }

    /**
     * 从指定 key 的队列里取出一个元素，队列空时阻塞
     */
    public V take(K key) throws InterruptedException {
        BlockingQueue<V> queue = map.computeIfAbsent(key, k -> new LinkedBlockingQueue<>(queueCapacity));
        return queue.take();
    }

    /**
     * 非阻塞地尝试取元素，空时返回 null
     */
    public V poll(K key) {
        BlockingQueue<V> queue = map.get(key);
        return queue != null ? queue.poll() : null;
    }

    /**
     * 查看某个 key 是否已有队列（不阻塞）
     */
    public boolean containsKey(K key) {
        return map.containsKey(key);
    }

    /**
     * 清理空队列（可选）
     */
    public void removeEmptyQueues() {
        map.entrySet().removeIf(e -> e.getValue().isEmpty());
    }
}
