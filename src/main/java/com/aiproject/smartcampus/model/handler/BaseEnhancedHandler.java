// BaseEnhancedHandler.java
package com.aiproject.smartcampus.model.handler;

import com.aiproject.smartcampus.commons.client.StatusCilent;
import com.aiproject.smartcampus.commons.utils.CreateDiagram;
import com.aiproject.smartcampus.pojo.bo.TaskAction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * @program: SmartCampus
 * @description: Handler基类，统一处理状态更新和图状态管理
 * @author: lk
 * @create: 2025-07-05
 **/
@Slf4j
@Component
public abstract class BaseEnhancedHandler extends EnhancedAutoRegisterHandler {

    @Autowired
    protected StatusCilent statusCilent;
    
    @Autowired
    protected CreateDiagram createDiagram;

    @Override
    public final String run(String intent, List<CompletableFuture<String>> result) {
        long startTime = System.currentTimeMillis();
        
        try {
            log.info("任务[{}]开始执行，类型: {}", intent, this.getClass().getSimpleName());
            
            // 1. 更新状态为运行中
            updateTaskStatus(intent, "RUNNING");
            
            // 2. 执行具体业务逻辑
            String businessResult = executeBusinessLogic(intent, result);
            
            // 3. 任务成功完成后的状态更新
            onTaskSuccess(intent, businessResult);
            
            long duration = System.currentTimeMillis() - startTime;
            log.info("任务[{}]执行成功，耗时: {}ms", intent, duration);
            
            return businessResult;
            
        } catch (Exception e) {
            // 4. 任务失败的状态更新
            long duration = System.currentTimeMillis() - startTime;
            log.error("任务[{}]执行失败，耗时: {}ms", intent, duration, e);
            
            onTaskFailure(intent, e);
            throw new RuntimeException("任务执行失败: " + intent, e);
        }
    }

    /**
     * 子类实现具体的业务逻辑
     */
    protected abstract String executeBusinessLogic(String intent, List<CompletableFuture<String>> result);

    /**
     * 任务成功完成时的处理
     */
    protected void onTaskSuccess(String intent, String result) {
        try {
            log.info("任务[{}]执行成功，开始更新状态，结果长度: {}", intent, 
                    result != null ? result.length() : 0);
            
            // 1. 更新任务状态为成功
            updateTaskStatus(intent, "SUCCESS");
            
            // 2. 更新图状态：减少依赖任务的入度
            createDiagram.updateInDegree(intent);
            
            // 3. 发送入度减少动作
            TaskAction decreaseAction = TaskAction.indegreeDecrease(intent);
            statusCilent.push(decreaseAction);
            
            log.info("任务[{}]状态更新完成", intent);
            
        } catch (Exception e) {
            log.error("任务成功后状态更新失败: {}", intent, e);
        }
    }

    /**
     * 任务失败时的处理
     */
    protected void onTaskFailure(String intent, Exception e) {
        try {
            log.error("任务[{}]执行失败", intent, e);
            
            // 更新任务状态为失败
            updateTaskStatus(intent, "FAILED");
            
            // 可选：根据业务需求决定是否在失败时也减少入度
            // 这里选择减少入度，避免阻塞后续任务
            if (shouldUpdateGraphOnFailure()) {
                createDiagram.updateInDegree(intent);
                TaskAction decreaseAction = TaskAction.indegreeDecrease(intent);
                statusCilent.push(decreaseAction);
                log.info("任务[{}]失败后已更新图状态", intent);
            }
            
        } catch (Exception updateException) {
            log.error("任务失败后状态更新失败: {}", intent, updateException);
        }
    }

    /**
     * 子类可以重写此方法来决定失败时是否更新图状态
     * 默认返回true，即失败时也减少入度，避免阻塞
     */
    protected boolean shouldUpdateGraphOnFailure() {
        return true;
    }

    /**
     * 更新任务状态
     */
    protected void updateTaskStatus(String intent, String status) {
        try {
            TaskAction action = TaskAction.statusUpdate(intent, status);
            statusCilent.push(action);
            log.debug("任务[{}]状态更新为: {}", intent, status);
        } catch (Exception e) {
            log.error("更新任务状态失败: {} -> {}", intent, status, e);
        }
    }

    /**
     * 获取当前Handler类型
     */
    protected String getHandlerType() {
        return this.getClass().getSimpleName();
    }
}