package com.example.limittransactsapi.config;

import com.example.limittransactsapi.services.LimitService;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.beans.factory.annotation.Autowired;

@Data
@Configuration
@EnableScheduling
public class SchedulerConfig {

    @Value("${scheduler.enabled:true}")
    private boolean schedulerEnabled;

    @Value("${scheduler.cron:0 0 1 1 *}") // Запланированное время по умолчанию
    private String cronSchedule;

    @Value("${scheduler.maxAttempts:10}") // Максимальное количество попыток
    private int maxAttempts;

    private LimitService limitService;


    @Autowired
    public SchedulerConfig(LimitService limitService) {
        this.limitService = limitService;
    }

    @Scheduled(cron = "${scheduler.cron}")
    public void scheduleTask() {
        if (schedulerEnabled) {
            int attempts = 0;
            boolean success = false;

            // Цикл, который будет продолжать выполняться до получения положительного ответа
            while (!success && attempts < maxAttempts) {
                success = limitService.insertMonthlyLimit(); // Попытка вставить лимит
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


}
