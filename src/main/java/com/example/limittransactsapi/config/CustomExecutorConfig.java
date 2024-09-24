package com.example.limittransactsapi.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class CustomExecutorConfig {


    @Bean(name = "customExecutor") // Регистрация бина для асинхронного выполнения задач
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        executor.setCorePoolSize(5); // Установка базового размера пула потоков
        executor.setMaxPoolSize(10); // Установка максимального размера пула потоков
        executor.setQueueCapacity(25); // Установка емкости очереди задач
        executor.setThreadNamePrefix("AsyncThread-"); // Установка префикса имен потоков

        executor.initialize(); // Инициализация исполнителя

        return executor;
    }
}
