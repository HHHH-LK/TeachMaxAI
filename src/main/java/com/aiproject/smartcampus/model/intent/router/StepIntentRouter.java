package com.aiproject.smartcampus.model.intent.router;

import com.aiproject.smartcampus.commons.ResultCilent;
import com.aiproject.smartcampus.commons.StatusCilent;
import com.aiproject.smartcampus.commons.easyuse.IntentDispatcher;
import com.aiproject.smartcampus.commons.easyuse.RateLimiterWrapper;
import com.aiproject.smartcampus.commons.easyuse.TaskExecutor;
import com.aiproject.smartcampus.commons.utils.CreateDiagram;
import com.aiproject.smartcampus.commons.utils.TaskStatusChange;
import com.aiproject.smartcampus.handler.contenthandler.ContentCheckClient;
import com.aiproject.smartcampus.model.intent.handler.Handler;
import com.aiproject.smartcampus.model.intent.summer.ModelSummer;
import com.aiproject.smartcampus.pojo.bo.TaskAction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReadWriteLock;

/**
 * @program: SmartCampus
 * @description: 子意图路由
 * @author: lk
 * @create: 2025-05-27 23:35
 **/

@Component
@Slf4j
@RequiredArgsConstructor
public class StepIntentRouter implements StepRouter {

    private final TaskExecutor taskExecutor;
    private final RateLimiterWrapper rateLimiter;
    private final CreateDiagram createDiagram;
    private final ResultCilent resultCilent;
    private final ContentCheckClient contentCheckClient;
    private final ModelSummer modelSummer;
    private final StatusCilent statusCilent;


    @Override
    public String route(List<String> intents) {
        log.info("开始进行意图路由,意图为：{}", intents);
        boolean check = contentCheckClient.check(intents);
        if (!check) {
            log.error("内容检测不通过，包含违规内容");
            return "内容检测不通过,存在非法关键词,请检查输入内容是否符合规范";
        }
        //执行路由
        start();
        //获取总结
        String summer = modelSummer.summer(intents);
        //todo 清理数据

        //返回结果
        return summer;
    }

    //执行限流路由
    public void start() {
        int maxLimit = 100;
        int size = 0;
        while (size < maxLimit) {
            //处理依赖关系获取入度为0且任务状态未执行的的任务进行执行
            //todo 任务处理结果逻辑处理错误
            List<String> readyList = createDiagram.getReadyList();
            if (readyList == null || readyList.isEmpty()) {
                log.info("所有任务执行完毕");
                break;
            }
            //获取任务执行器
            for (String intent : readyList) {
                log.info("获取{}任务前置条件中", intent);
                //获取前置任务
                List<CompletableFuture<String>> result = resultCilent.getPreTask(intent);
                if(result!=null&&!result.isEmpty()){
                    //等待前置任务完成后再进行执行
                    CompletableFuture.allOf(result.toArray(new CompletableFuture[0])).join();
                    log.info("获取{{}}任务前置条件完成,前置条件为{}", intent, result);
                }
                //预估限流处理
                boolean allow = rateLimiter.isAllow(intent);
                if (allow) {
                    //修改任务状态
                    TaskAction action = TaskAction.statusUpdate(intent, "RUNNING");
                    statusCilent.push(action);
                    taskExecutor.executeAsync(intent, result);
                } else {
                    try {
                        //修改任务状态
                        TaskAction action = TaskAction.statusUpdate(intent, "RUNNING");
                        statusCilent.push(action);
                        log.info("任务{}限流,进入到阻塞队列中执行", intent);
                        taskExecutor.enqueueDelayed(intent, result);
                    } catch (Exception e) {
                        log.error("任务{}限流,阻塞队列执行失败", intent, e);
                    }
                }
            }
            readyList.clear();
            size++;
        }
    }


}

