package com.aiproject.smartcampus.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.concurrent.Executor;

@Configuration
public class AsyncExecutorConfig {

    @Bean("transactionAwareExecutor")
    public Executor transactionAwareExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("AsyncTxExecutor-");
        executor.setTaskDecorator(new ContextCopyingDecorator()); // 关键
        executor.initialize();
        return executor;
    }

    /**
     * 异步任务执行器配置
     */
    @Bean(name = "memoryStoreTaskExecutor")
    public Executor memoryStoreTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("MemoryStore-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }

    /**
     * 保证事务上下文在异步线程中可用
     */
    public static class ContextCopyingDecorator implements TaskDecorator {
        @Override
        public Runnable decorate(Runnable runnable) {
            // 将主线程的上下文（包括事务）复制一份给子线程
            var context = TransactionSynchronizationManager.getResourceMap();
            return () -> {
                var old = TransactionSynchronizationManager.getResourceMap();
                try {
                    context.forEach(TransactionSynchronizationManager::bindResource);
                    runnable.run();
                } finally {
                    // 清除，避免线程池复用带来污染
                    context.keySet().forEach(TransactionSynchronizationManager::unbindResource);
                    old.forEach(TransactionSynchronizationManager::bindResource);
                }
            };
        }
    }
}
