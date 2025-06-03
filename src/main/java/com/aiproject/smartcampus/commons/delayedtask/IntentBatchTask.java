package com.aiproject.smartcampus.commons.delayedtask;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.*;

/**
 * @program: SmartCampus
 * @description: 延迟队列任务
 * @author: lk
 * @create: 2025-05-28 14:57
 **/

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IntentBatchTask implements Delayed {
    //意图
    private String intents;
    //前置条件
    private List<CompletableFuture<String>> futureList;
    //到期时间
    private long delayTime;

    @Override
    public long getDelay(@NotNull TimeUnit unit) {
        long remainingTime = delayTime - System.currentTimeMillis();
        return unit.convert(remainingTime, TimeUnit.MILLISECONDS);
    }

    @Override
    public int compareTo(@NotNull Delayed o) {
        if(o instanceof IntentBatchTask){
            IntentBatchTask task = (IntentBatchTask) o;
            return Long.compare(this.delayTime, task.delayTime);
        }
        return 0;
    }



}
