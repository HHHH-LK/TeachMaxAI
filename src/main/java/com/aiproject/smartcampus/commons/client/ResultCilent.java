package com.aiproject.smartcampus.commons.client;

import com.aiproject.smartcampus.commons.utils.CreateDiagram;
import com.aiproject.smartcampus.pojo.bo.TaskAction;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

@Component
@Slf4j
@RequiredArgsConstructor
public class ResultCilent {

    //当前任务所依赖的任务
    private Map<String, List<CompletableFuture<String>>> resultMap = new ConcurrentHashMap<>();
    private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    private final CreateDiagram createDiagram;
    private final StatusCilent statusCilent;
    private final TaskClient taskClient;

    @PostConstruct
    public void init() {
        createDiagram.getAdjList().keySet().forEach(intent ->
                resultMap.put(intent, new CopyOnWriteArrayList<>()));
        log.info("任务结果客户端初始化成功");
    }

    /**
     * 添加结果
     */
    public void addResult(String intent, List<CompletableFuture<String>> completableFutures) {
        readWriteLock.writeLock().lock();
        try {
            log.info("为任务{}添加结果到resultMap中", intent);
            resultMap.put(intent, completableFutures);
            TaskAction action = TaskAction.statusUpdate(intent, "SUCCESS");
            statusCilent.push(action);
        } finally {
            readWriteLock.writeLock().unlock();
        }
    }

    /**
     * 获取结果
     */
    public List<CompletableFuture<String>> getResult(String key) {
        readWriteLock.readLock().lock();
        try {
            List<CompletableFuture<String>> completableFutures = resultMap.get(key);

            if (completableFutures == null || completableFutures.isEmpty()) {
                log.warn("resultMap中不存在{}的依赖结果", key);
                return new ArrayList<>(); // 返回空列表而不是null
            }
            log.info("从resultMap中获取任务{}的结果", key);
            return completableFutures;
        } finally {
            readWriteLock.readLock().unlock();
        }
    }
    /**
     * 移除结果
     */
    public void removeResult(String key) {
        readWriteLock.writeLock().lock();
        try {
            if (!resultMap.containsKey(key)) {
                log.error("从resultMap中移除{}失败，key不存在", key);
                throw new RuntimeException("resultMap中不存在该key: " + key);
            }
            resultMap.remove(key);
            log.info("成功从resultMap中移除任务{}的结果", key);
        } finally {
            readWriteLock.writeLock().unlock();
        }
    }

    public void clearAllFutures(List<String> intents) {
        //清除所有任务
        intents.forEach(intent -> {
           resultMap.put(intent, new CopyOnWriteArrayList<>());
        });
    }

    //获取依赖任务的结果
    public List<CompletableFuture<String>> getPreTask(String intent) {
        try {
            // 获取当前任务的依赖任务
            List<String> preTask = createDiagram.getParetents(intent);

            // 判断是否需要依赖
            if (preTask == null || preTask.isEmpty()) {
                log.info("任务{}不需要依赖其他任务", intent);
                return new ArrayList<>(); // 🔥 返回空列表而不是null
            }
            // 过滤已经执行完成的任务(幂等性)
            List<String> preTasks = preTask.stream()
                    .filter(task -> {
                        String status = taskClient.getTaskStatus(task);
                        return !"SUCCESS".equals(status) && !"RUNNING".equals(status);
                    })
                    .collect(Collectors.toList());

            if (preTasks.isEmpty()) {
                log.info("任务{}的所有前置任务都已完成", intent);
                return new ArrayList<>();
            }

            // 获取依赖任务的结果
            List<CompletableFuture<String>> futures = new ArrayList<>();
            for (String preTaskName : preTasks) {
                List<CompletableFuture<String>> taskResults = getResult(preTaskName);
                // 🔥 由于getResult现在总是返回非null，这里更安全
                if (!taskResults.isEmpty()) {
                    futures.addAll(taskResults);
                } else {
                    log.warn("前置任务{}的结果为空", preTaskName);
                }
            }
            return futures;

        } catch (Exception e) {
            log.error("获取任务{}的前置任务失败", intent, e);
            return new ArrayList<>(); // 🔥 异常时也返回空列表
        }
    }

}