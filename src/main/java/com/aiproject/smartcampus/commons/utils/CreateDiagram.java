package com.aiproject.smartcampus.commons.utils;

import com.aiproject.smartcampus.commons.client.TaskClient;
import com.aiproject.smartcampus.pojo.bo.Node;
import com.aiproject.smartcampus.pojo.bo.Side;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @program: SmartCampus
 * @description: 建图 - 修复版
 * @author: lk
 * @create: 2025-05-28 21:51
 **/

@Order(5)
@Component
@Slf4j
@RequiredArgsConstructor
public class CreateDiagram {

    //注入任务图以及任务关系图
    private final TaskClient taskClient;
    private final TaskStatusChange taskStatusChange;

    private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    //邻接表
    private final Map<String, List<String>> adjList = new ConcurrentHashMap<>();
    //当前入度表 - 动态变化
    private final Map<String, AtomicInteger> intentMap = new ConcurrentHashMap<>();
    //原始入度表 - 保存初始值不变
    private final Map<String, AtomicInteger> reIntentMap = new ConcurrentHashMap<>();
    //使用线程安全的List
    private final List<String> readyList = new CopyOnWriteArrayList<>();

    public Map<String, List<String>> getAdjList() {
        return new ConcurrentHashMap<>(adjList);
    }

    public Map<String, AtomicInteger> getIntentMap() {
        return new ConcurrentHashMap<>(intentMap);
    }

    //最先初始化 - 添加锁保护
    public void init() {
        readWriteLock.writeLock().lock();
        try {
            log.info("开始初始化任务图");

            List<Node> nodeList = taskClient.getTaskList();
            List<Side> sideList = taskClient.getTaskRelation();

            //清空上一次的信息
            adjList.clear();
            intentMap.clear();
            reIntentMap.clear();
            readyList.clear();

            //提前定义相关依赖信息
            for (Node node : nodeList) {
                adjList.put(node.getIntent(), new ArrayList<>());
                intentMap.put(node.getIntent(), new AtomicInteger(0));
            }

            //建立边的关系
            for (Side side : sideList) {
                List<String> children = adjList.get(side.getFrom());
                if (children != null) {
                    children.add(side.getTo());
                    intentMap.computeIfAbsent(side.getTo(), k -> new AtomicInteger(0)).incrementAndGet();
                } else {
                    log.warn("任务 {} 不存在于节点列表中", side.getFrom());
                }
            }

            //复制入度表作为原始入度保存
            reIntentMap.clear();
            for (Map.Entry<String, AtomicInteger> entry : intentMap.entrySet()) {
                reIntentMap.put(entry.getKey(), new AtomicInteger(entry.getValue().get()));
            }

            log.info("任务图初始化完成，节点数: {}, 边数: {}", nodeList.size(), sideList.size());

        } catch (Exception e) {
            log.error("任务图初始化失败", e);
            throw new RuntimeException("任务图初始化失败", e);
        } finally {
            readWriteLock.writeLock().unlock();
        }
    }

    //(基于拓扑)获取就绪列表 - 修复逻辑错误
    public List<String> getReadyList() {
        readWriteLock.readLock().lock();
        try {
            List<String> currentReadyList = new ArrayList<>();

            // 寻找所有入度为0的任务
            for (Map.Entry<String, AtomicInteger> entry : intentMap.entrySet()) {
                String intent = entry.getKey();
                String status = taskStatusChange.getStatus(intent);

                // 修复：扩展可执行状态的范围
                boolean isExecutableStatus = status.equals("UNDO") ||
                        status.equals("ERROR") ||
                        status.equals("DELAYED_READY") ||
                        status.equals("WAITING_RETRY");

                if (entry.getValue().get() == 0 && isExecutableStatus) {
                    currentReadyList.add(intent);
                    log.debug("添加就绪任务: {} (状态: {})", intent, status);
                }
            }

            log.debug("当前就绪任务: {}", currentReadyList);
            return currentReadyList;

        } catch (Exception e) {
            log.error("获取就绪列表失败", e);
            return new ArrayList<>();
        } finally {
            readWriteLock.readLock().unlock();
        }
    }

    //加入元素到就绪队列中
    public void addReadyList(String intent) {
        if (intent != null && !intent.trim().isEmpty()) {
            readyList.add(intent.trim());
            log.debug("添加任务到就绪列表: {}", intent);
        }
    }

    //修改入度 - 修复空指针风险
    public void updateInDegree(String intent) {
        readWriteLock.writeLock().lock();
        try {
            List<String> children = adjList.get(intent);
            if (children != null && !children.isEmpty()) {
                //遍历邻接表中的所有相邻节点
                for (String child : children) {
                    AtomicInteger childInDegree = intentMap.get(child);
                    if (childInDegree != null && childInDegree.get() > 0) {
                        int newInDegree = childInDegree.decrementAndGet();
                        log.debug("任务 {} 的入度减少为: {}", child, newInDegree);
                    }
                }
            }
        } catch (Exception e) {
            log.error("更新入度失败，任务: {}", intent, e);
        } finally {
            readWriteLock.writeLock().unlock();
        }
    }

    //获取节点的原始入度信息 - 修复空指针风险
    public int getInDegree(String intent) {
        readWriteLock.readLock().lock();
        try {
            AtomicInteger inDegree = reIntentMap.get(intent);
            return inDegree != null ? inDegree.get() : 0;
        } catch (Exception e) {
            log.error("获取入度失败，任务: {}", intent, e);
            return 0;
        } finally {
            readWriteLock.readLock().unlock();
        }
    }

    //获取节点的当前入度信息
    public int getCurrentInDegree(String intent) {
        readWriteLock.readLock().lock();
        try {
            AtomicInteger inDegree = intentMap.get(intent);
            return inDegree != null ? inDegree.get() : 0;
        } catch (Exception e) {
            log.error("获取当前入度失败，任务: {}", intent, e);
            return 0;
        } finally {
            readWriteLock.readLock().unlock();
        }
    }

    //加入信息到入度表中 - 添加参数验证
    public void addInDegree(String intent, AtomicInteger inDegree) {
        if (intent == null || intent.trim().isEmpty() || inDegree == null) {
            log.warn("无效的参数，跳过添加入度: intent={}, inDegree={}", intent, inDegree);
            return;
        }

        readWriteLock.writeLock().lock();
        try {
            reIntentMap.put(intent.trim(), inDegree);
            intentMap.put(intent.trim(), new AtomicInteger(inDegree.get()));
            log.debug("添加任务入度: {} -> {}", intent, inDegree.get());
        } finally {
            readWriteLock.writeLock().unlock();
        }
    }

    //获取父节点 - 添加错误处理
    public List<String> getParetents(String intent) {
        readWriteLock.readLock().lock();
        try {
            List<String> parentsList = new ArrayList<>();

            if (intent == null || intent.trim().isEmpty()) {
                log.warn("任务名称为空，无法获取父节点");
                return parentsList;
            }

            //遍历邻接表，如果包含该节点就说明是父节点
            for (Map.Entry<String, List<String>> entry : adjList.entrySet()) {
                List<String> children = entry.getValue();
                if (children != null && children.contains(intent.trim())) {
                    parentsList.add(entry.getKey());
                }
            }

            log.debug("任务 {} 的父节点: {}", intent, parentsList);
            return parentsList;

        } catch (Exception e) {
            log.error("获取父节点失败，任务: {}", intent, e);
            return new ArrayList<>();
        } finally {
            readWriteLock.readLock().unlock();
        }
    }

    //获取子节点
    public List<String> getChildren(String intent) {
        readWriteLock.readLock().lock();
        try {
            if (intent == null || intent.trim().isEmpty()) {
                return new ArrayList<>();
            }

            List<String> children = adjList.get(intent.trim());
            return children != null ? new ArrayList<>(children) : new ArrayList<>();

        } catch (Exception e) {
            log.error("获取子节点失败，任务: {}", intent, e);
            return new ArrayList<>();
        } finally {
            readWriteLock.readLock().unlock();
        }
    }

    //获取所有任务
    public List<String> getAllTasks() {
        readWriteLock.readLock().lock();
        try {
            return new ArrayList<>(adjList.keySet());
        } finally {
            readWriteLock.readLock().unlock();
        }
    }

    //检查是否还有未完成的任务
    public boolean hasUnfinishedTasks() {
        readWriteLock.readLock().lock();
        try {
            for (String task : adjList.keySet()) {
                String status = taskStatusChange.getStatus(task);

                // 扩展未完成状态的定义
                boolean isUnfinished = "UNDO".equals(status) ||
                        "RUNNING".equals(status) ||
                        "ERROR".equals(status) ||
                        "DELAYED_PROCESSING".equals(status) ||
                        "WAITING_DEPENDENCIES".equals(status) ||
                        "WAITING_RETRY".equals(status) ||
                        "RATE_LIMITED".equals(status);

                if (isUnfinished) {
                    log.debug("发现未完成任务: {} (状态: {})", task, status);
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            log.error("检查未完成任务失败", e);
            return true; // 出错时保守返回true
        } finally {
            readWriteLock.readLock().unlock();
        }
    }

    //清除所有节点 - 修复重复清理
    public void clear() {
        readWriteLock.writeLock().lock();
        try {
            readyList.clear();
            intentMap.clear();
            reIntentMap.clear();
            adjList.clear();
            log.info("图节点清除成功");
        } catch (Exception e) {
            log.error("图节点清除失败", e);
            try {
                readyList.clear();
                intentMap.clear();
                reIntentMap.clear();
                adjList.clear();
            } catch (Exception clearException) {
                log.error("强制清理也失败", clearException);
            }
        } finally {
            readWriteLock.writeLock().unlock();
        }
    }

    //添加调试信息方法
    public void printDebugInfo() {
        readWriteLock.readLock().lock();
        try {
            log.info("=== 图调试信息 ===");
            log.info("邻接表: {}", adjList);
            log.info("当前入度: {}", intentMap);
            log.info("原始入度: {}", reIntentMap);
            log.info("就绪列表: {}", readyList);
            log.info("===============");
        } finally {
            readWriteLock.readLock().unlock();
        }
    }

    //验证图的完整性
    public boolean validateGraph() {
        readWriteLock.readLock().lock();
        try {
            // 检查是否有孤立节点或无效边
            for (Map.Entry<String, List<String>> entry : adjList.entrySet()) {
                String from = entry.getKey();
                List<String> children = entry.getValue();

                if (!intentMap.containsKey(from)) {
                    log.error("邻接表中的节点 {} 不存在于入度表中", from);
                    return false;
                }
                if (children != null) {
                    for (String child : children) {
                        if (!intentMap.containsKey(child)) {
                            log.error("子节点 {} 不存在于入度表中", child);
                            return false;
                        }
                    }
                }
            }

            log.info("图结构验证通过");
            return true;

        } catch (Exception e) {
            log.error("图结构验证失败", e);
            return false;
        } finally {
            readWriteLock.readLock().unlock();
        }
    }




}