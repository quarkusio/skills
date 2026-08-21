package com.ibm.websphere.samples.daytrader.config;

import java.util.concurrent.ThreadFactory;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "ManagedExecutorService")
    public AsyncTaskExecutor managedExecutorService() {
        ThreadPoolTaskExecutor threadPoolTaskExecutor = new ThreadPoolTaskExecutor();
        threadPoolTaskExecutor.setThreadNamePrefix("mes-");
        threadPoolTaskExecutor.initialize();
        return threadPoolTaskExecutor;
    }

    @Bean(name = "ManagedScheduledTaskExecutor")
    public ThreadPoolTaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler threadPoolTaskExecutor = new ThreadPoolTaskScheduler();
        threadPoolTaskExecutor.setThreadNamePrefix("mstex-");
        threadPoolTaskExecutor.initialize();
        return threadPoolTaskExecutor;
    }

    @Bean(name = "ManagedThreadFactory")
    public ThreadFactory managedThreadFactoryBean() {
        return Thread
                .ofPlatform()
                .name("mes-", 0) // mes-1, mes-2, ...
                .uncaughtExceptionHandler((t, e) -> {
                    // hook into your logger
                    System.err.println("Uncaught in " + t.getName());
                    e.printStackTrace();
                })
                .factory();
    }
}
