package com.example.limittransactsapi.config;

import com.example.limittransactsapi.services.ShedullerService;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.service.spi.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.OffsetDateTime;

@Slf4j
@Configuration
@EnableScheduling
public class ShedullerConfig {

    @Value("${scheduler.enabled:true}")
    private boolean schedulerEnabled;

    @Value("${scheduler.cron:0 0 1 1 *}")
    private String cronSchedule;

    @Value("${scheduler.maxAttempts:10}")
    private int maxAttempts;

    @Value("${scheduler.retryInterval:2000}")
    private long retryInterval;

    @Value("${scheduler.expectedIntervalMinutes:44640}") // Пример в минутах для 31 дней
    private long expectedIntervalMinutes;


    @EventListener
    public void onApplicationEvent(ContextRefreshedEvent event) {
        checkTasksOnStartup();
    }

    private final ShedullerService shedullerService;

    @Autowired
    public ShedullerConfig(ShedullerService shedullerService) {
        this.shedullerService = shedullerService;
    }

    private void checkTasksOnStartup() {
        String taskName = "Monthly Limit Update";
        OffsetDateTime now = OffsetDateTime.now();

        // Проверка пропущенных заданий только при старте приложения
        log.info("started check for missed tasks");
        if (shedullerService.checkMissedExecution(taskName, now, expectedIntervalMinutes)) {
            log.warn("Detected a missed execution for task: " + taskName);
            handleMissedExecution(taskName, now);
        }else {
            log.info("no missed executions, status ok.");
        }
    }

    private void handleMissedExecution(String taskName, OffsetDateTime now) {
        executeScheduledTasks();
        log.warn("Handling missed execution for task: " + taskName);
    }

      @Scheduled(cron = "${scheduler.cron}")
    public void scheduleTask() {
        if (schedulerEnabled) {
            executeScheduledTasks();
        } else {
            log.warn("Scheduler is disabled, task will not execute.");
        }
    }

    public void executeScheduledTasks() {
        String taskName = "Monthly Limit Update";
        OffsetDateTime now = OffsetDateTime.now();
        int attempts = 0;
        boolean success = false;
        shedullerService.updateLimitStatusIsActive();

        while (!success && attempts < maxAttempts) {
            success = shedullerService.setMonthlyLimitByDefault();
            if (success) {
                shedullerService.logExecution(taskName, now, "SUCCESS");
                log.info("Scheduler task successfully executed.");
            } else {
                attempts++;
                log.error("Attempt " + attempts + " failed. Retrying...");
                try {
                    Thread.sleep(retryInterval);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    log.error("Thread was interrupted", ie);
                }
            }
        }
        // Log failure if no attempt was successful
        if (!success) {
            shedullerService.logExecution(taskName, now, "FAILED");
            throw new ServiceException(taskName + " execution failed");
        }
    }
}
