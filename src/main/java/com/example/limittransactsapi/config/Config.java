package com.example.limittransactsapi.config;


import com.example.limittransactsapi.services.LimitService;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.Executor;

@Data
@Configuration
@EnableScheduling
@EnableAsync
public class Config {

    @Value("${scheduler.enabled:true}")
    private boolean schedulerEnabled;

    @Value("${scheduler.cron:0 0 1 1 *}") // Запланированное время по умолчанию
    private String cronSchedule;

    @Value("${scheduler.maxAttempts:10}") // Максимальное количество попыток
    private int maxAttempts;

    private LimitService limitService;


    @Autowired
    public Config(LimitService limitService) {
        this.limitService = limitService;
    }

    @Scheduled(cron = "${scheduler.cron}")
    public void scheduleTask() {
        if (schedulerEnabled) {
            int attempts = 0;
            boolean success = false;

            // Цикл, который будет продолжать выполняться до получения положительного ответа
            while (!success && attempts < maxAttempts) {
                success = limitService.setMonthlyLimitByDefault(); // Попытка вставить лимит
                if (!success) {
                    attempts++;
                    System.err.println("Attempt " + attempts + " failed. Retrying...");
                    try {
                        Thread.sleep(2000); // Ждем 2 секунды перед следующей попыткой
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt(); // Восстанавливаем флаг прерывания
                    }
                }
            }
        } else {
            System.out.println("Scheduler is disabled, task will not execute.");
        }
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean(name = "taskExecutor") // Регистрация бина для асинхронного выполнения задач
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // Установка базового размера пула потоков
        executor.setCorePoolSize(5);

        // Установка максимального размера пула потоков
        executor.setMaxPoolSize(10);

        // Установка емкости очереди задач
        executor.setQueueCapacity(25);

        // Установка префикса имен потоков для упрощения отладки
        executor.setThreadNamePrefix("AsyncThread-");

        // Инициализация исполнителя
        executor.initialize();

        return executor;
    }
}
