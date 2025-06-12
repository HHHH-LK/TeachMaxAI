package com.aiproject.smartcampus.commons.client;

import com.aiproject.smartcampus.pojo.bo.TaskAction;
import com.aiproject.smartcampus.pojo.bo.Node;
import com.aiproject.smartcampus.pojo.bo.Side;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @program: SmartCampus
 * @description: 任务处理与任务关系依赖客户穿
 * @author: lk
 * @create: 2025-05-29 16:25
 **/

@Component
@Slf4j
@RequiredArgsConstructor
public class TaskClient {

    private final ReadWriteLock readWriteLock=new ReentrantReadWriteLock();
    //任务列表
    private final List<Node> TaskList=new ArrayList<>();
    //任务关系
    private final List<Side> TaskRelationMap=new ArrayList<>();
    //任务执行状态表
    private final Map<String, TaskAction> TaskStatusMap=new ConcurrentHashMap<>();

    public Map<String, TaskAction> getTaskStatusMap() {
        return TaskStatusMap;
    }

    public void addTask(Node taskName){
        readWriteLock.writeLock().lock();
        try {
            if(!TaskList.contains(taskName)){
                TaskList.add(taskName);
                // 这里给每个任务设置一个新的 TaskAction 对象，状态为 UNDO
                TaskAction action = TaskAction.statusUpdate(taskName.getIntent(), "UNDO");
                TaskStatusMap.put(taskName.getIntent(), action);
            }
            else{
                log.warn("任务已存在");
            }
        }finally {
            readWriteLock.writeLock().unlock();
        }
    }


    public void addTaskRelation( List<Side> relationTask) {
        readWriteLock.writeLock().lock();
        try {
            if(!TaskRelationMap.containsAll(relationTask)){
                TaskRelationMap.addAll(relationTask);
            } else {
                log.warn("任务关系已存在");
            }
        } finally {
            readWriteLock.writeLock().unlock();
        }
    }

    //获取任务关系
    public List<Side> getTaskRelation(){
        readWriteLock.readLock().lock();
        try {
           return TaskRelationMap;
        }finally {
            readWriteLock.readLock().unlock();

        }
    }

    //获取任务列表
    public List<Node> getTaskList(){
        readWriteLock.readLock().lock();
        try {
            return TaskList;
        }finally {
            readWriteLock.readLock().unlock();
        }
    }

    //获取任务状态
    public String getTaskStatus(String intent) {
        Lock lock = readWriteLock.readLock();
        lock.lock();
        try{
            TaskAction action = TaskStatusMap.get(intent);
            return action.getNewStatus();
        }finally {
            lock.unlock();
        }

    }

    //清除所有任务
    public void deleteAllTask() {
        //删除任务
        readWriteLock.writeLock().lock();
        try {
            TaskList.clear();
            TaskStatusMap.clear();
            TaskRelationMap.clear();
        }catch (Exception e){
            log.error("任务清除失败！");
            throw new RuntimeException("任务清除失败！");

        }

    }


}
