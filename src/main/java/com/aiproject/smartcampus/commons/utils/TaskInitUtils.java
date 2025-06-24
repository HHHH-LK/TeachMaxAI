package com.aiproject.smartcampus.commons.utils;

import com.aiproject.smartcampus.commons.client.TaskClient;
import com.aiproject.smartcampus.pojo.bo.Node;
import com.aiproject.smartcampus.pojo.bo.Side;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @program: SmartCampus
 * @description: 任务初始化工具
 * @author: lk_hhh
 * @create: 2025-06-10 10:08
 **/

@RequiredArgsConstructor
@Component
@Slf4j
public class TaskInitUtils {

    private final CreateDiagram createDiagram;
    private final TaskStatusChange taskStatusChange;
    private final TaskClient taskClient;
    private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();

    /**
     * 初始化任务节点和依赖关系
     * @param splitResult 拆分后的任务列表
     * @param intentRelations 任务依赖关系列表
     */
    public void init(List<String> splitResult, List<Side> intentRelations) {
        Lock lock = readWriteLock.writeLock();
        try {
            lock.lock();
            log.info("开始初始化任务节点，任务数量: {}", splitResult.size());

            // 验证输入参数
            if (splitResult == null || splitResult.isEmpty()) {
                throw new RuntimeException("任务拆解结果为空，无法进行初始化");
            }

            // 添加任务节点
            addTaskNodes(splitResult);

            // 处理任务依赖关系
            handleTaskRelations(splitResult, intentRelations);

            // 初始化相关组件
            initializeComponents();

            log.info("任务节点初始化完成");

        } catch (Exception e) {
            log.error("任务初始化失败: {}", e.getMessage(), e);
            throw new RuntimeException("任务初始化失败", e);
        } finally {
            lock.unlock();
        }
    }

    /**
     * 添加任务节点到系统中
     * @param splitResult 拆分后的任务列表
     */
    private void addTaskNodes(List<String> splitResult) {
        try {
            for (String task : splitResult) {
                if (task != null && !task.trim().isEmpty()) {
                    taskClient.addTask(new Node(task.trim(),null));
                    log.debug("添加任务节点: {}", task);
                }
            }
            log.info("成功添加 {} 个任务节点", splitResult.size());
        } catch (Exception e) {
            log.error("添加任务节点失败", e);
            throw new RuntimeException("添加任务节点失败", e);
        }
    }

    /**
     * 处理任务依赖关系
     * @param splitResult 拆分后的任务列表
     * @param intentRelations 任务依赖关系列表
     */
    private void handleTaskRelations(List<String> splitResult, List<Side> intentRelations) {
        try {
            if (intentRelations != null && !intentRelations.isEmpty()) {
                // 有依赖关系，添加到系统中
                taskClient.addTaskRelation(intentRelations);
                log.info("成功添加 {} 个任务依赖关系", intentRelations.size());
            } else {
                // 没有依赖关系，设置所有任务的入度为0
                setZeroInDegreeForAllTasks(splitResult);
                log.info("未发现任务依赖关系，所有任务入度设置为0");
            }
        } catch (Exception e) {
            log.error("处理任务依赖关系失败", e);
            throw new RuntimeException("处理任务依赖关系失败", e);
        }
    }

    /**
     * 为所有任务设置入度为0（无依赖关系时）
     * @param splitResult 拆分后的任务列表
     */
    private void setZeroInDegreeForAllTasks(List<String> splitResult) {
        for (String task : splitResult) {
            if (task != null && !task.trim().isEmpty()) {
                createDiagram.addInDegree(task.trim(), new AtomicInteger(0));
                log.debug("设置任务 {} 入度为0", task);
            }
        }
    }

    /**
     * 初始化相关组件
     */
    private void initializeComponents() {
        try {
            // 初始化图结构
            createDiagram.init();
            log.debug("图结构初始化完成");

            // 初始化任务状态变更组件
            taskStatusChange.init();
            log.debug("任务状态变更组件初始化完成");

        } catch (Exception e) {
            log.error("初始化相关组件失败", e);
            throw new RuntimeException("初始化相关组件失败", e);
        }
    }

    /**
     * 单独初始化单个任务（用于意图拆分失败的情况）
     * @param originalIntent 原始意图
     */
    public void initSingleTask(String originalIntent) {
        Lock lock = readWriteLock.writeLock();
        try {
            lock.lock();
            log.info("初始化单个任务: {}", originalIntent);

            if (originalIntent == null || originalIntent.trim().isEmpty()) {
                throw new RuntimeException("原始意图为空，无法进行初始化");
            }

            // 添加单个任务节点
            taskClient.addTask(new Node(originalIntent.trim(),null));

            // 设置入度为0（无依赖）
            createDiagram.addInDegree(originalIntent.trim(), new AtomicInteger(0));

            // 初始化相关组件
            initializeComponents();

            log.info("单个任务初始化完成: {}", originalIntent);

        } catch (Exception e) {
            log.error("单个任务初始化失败: {}", e.getMessage(), e);
            throw new RuntimeException("单个任务初始化失败", e);
        } finally {
            lock.unlock();
        }
    }
}