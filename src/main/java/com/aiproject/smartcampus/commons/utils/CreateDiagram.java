package com.aiproject.smartcampus.commons.utils;

import com.aiproject.smartcampus.commons.TaskClient;
import com.aiproject.smartcampus.pojo.bo.TaskAction;
import com.aiproject.smartcampus.pojo.po.Node;
import com.aiproject.smartcampus.pojo.po.Side;
import com.github.xiaoymin.knife4j.core.util.StrUtil;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @program: SmartCampus
 * @description: 建图
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

    private ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    //邻接表
    private Map<String, List<String>> adjList = new ConcurrentHashMap<>();
    //入度表
    private Map<String, AtomicInteger> intentMap = new ConcurrentHashMap<>();
    private Map<String, AtomicInteger> reIntentMap = new ConcurrentHashMap<>();
    private List<String> readyList = new ArrayList<>();

    public Map<String, List<String>> getAdjList() {
        return adjList;
    }

    public Map<String, AtomicInteger> getIntentMap() {
        return intentMap;
    }

    //最先初始化
    public void init() {
        List<Node> nodeList = taskClient.getTaskList();
        List<Side> sideList = taskClient.getTaskRelation();
        //清空上一次的信息
        if (!adjList.isEmpty()) {
            adjList.clear();
        }
        if (!intentMap.isEmpty()) {
            intentMap.clear();
        }
        //提前定义相关依赖信息
        for (Node node : nodeList) {
            adjList.put(node.getIntent(), new ArrayList<>());
            intentMap.put(node.getIntent(), new AtomicInteger(0));
        }
        for (Side side : sideList) {
            adjList.get(side.getFrom()).add(side.getTo());
            intentMap.computeIfAbsent(side.getTo(), k -> new AtomicInteger(0)).incrementAndGet();
        }
        //复制入度表（）
        reIntentMap = new ConcurrentHashMap<>();
        for (Map.Entry<String, AtomicInteger> entry : intentMap.entrySet()) {
            reIntentMap.put(entry.getKey(), new AtomicInteger(entry.getValue().get()));
        }
    }

    //(基于拓扑)获取就绪列表 加上写锁保证原子性
    public List<String> getReadyList() {
        readWriteLock.writeLock().lock();
        try {
            //释放就绪队列
            readyList.clear();
            //寻找所有入度为0的任务
            for (Map.Entry<String, AtomicInteger> entry : intentMap.entrySet()) {
                //获取任务并判断任务是否执行完毕
                String intent = entry.getKey();
                String status = taskStatusChange.getStatus(intent);
                if (entry.getValue().get() == 0 && status.equals("UNDO") || status.equals("ERROR")) {
                    readyList.add(entry.getKey());
                }
            }
            return readyList;

        } finally {
            readWriteLock.writeLock().unlock();

        }

    }

    //加入元素到就绪队列中
    public void addReadyList(String intent) {
        readWriteLock.writeLock().lock();
        try {
            readyList.add(intent);
        } finally {
            readWriteLock.writeLock().unlock();
        }
    }

    //修改入度
    public void updateInDegree(String intent) {
        readWriteLock.writeLock().lock();
        try {
            //遍历临街表中的所有相邻节点
            for (String adj : adjList.get(intent)) {
                intentMap.computeIfAbsent(adj, k -> new AtomicInteger(0)).decrementAndGet();
            }
        } finally {
            readWriteLock.writeLock().unlock();
        }
    }

    //获取节点的入度信息
    public int getInDegree(String intent) {
        readWriteLock.readLock().lock();
        try {
            return reIntentMap.get(intent).get();
        } finally {
            readWriteLock.readLock().unlock();
        }

    }

    //加入信息到入度表中
    public void addInDegree(String intent, AtomicInteger inDegree) {
        readWriteLock.writeLock().lock();
        try {
            reIntentMap.put(intent, inDegree);
        } finally {
            readWriteLock.writeLock().unlock();
        }

    }
    //获取父节点
    public List<String> getParetents(String intent) {
        Lock lock = readWriteLock.readLock();
        lock.lock();
        try{
            ArrayList<String> paretrntsList = new ArrayList<>();
            //遍历邻接表，如果包含该节点就说明是父节点
            for (Map.Entry<String, List<String>> entry : adjList.entrySet()) {
                if (entry.getValue().contains(intent)) {
                    paretrntsList.add(entry.getKey());
                }
            }

            return paretrntsList;
        }finally {
            lock.unlock();
        }
    }

}
