package com.example.limittransactsapi.services;

import com.example.limittransactsapi.Models.Entity.Limit;
import com.example.limittransactsapi.Models.Entity.TaskExecutionLog;
import com.example.limittransactsapi.repository.LimitRepository;
import com.example.limittransactsapi.repository.TaskExecutionLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;

import static java.time.Duration.between;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShedullerService {

    private static final String USD = "USD";

    private final LimitRepository limitRepository;
    private final TaskExecutionLogRepository logRepository;

    public boolean checkMissedExecution(String taskName, OffsetDateTime now, long expectedIntervalMinutes) {
        Optional<TaskExecutionLog> logRecord = logRepository.findByTaskName(taskName);

        // Если запись отсутствует, считаем, что задача пропущена
        OffsetDateTime lastExecutionTime = logRecord.map(taskExecutionLog -> taskExecutionLog.getLastExecutionTime())
                .orElse(OffsetDateTime.MIN); //если данных нет то присваиваем OffsetDateTime.MIN
                                            // это очень малая временная отметка что даст на при проверке то что дата пропущена и мы не получим exception на null.

        long actualInterval = between(lastExecutionTime, now).toMinutes();

        // Если время последнего выполнения слишком далеко в прошлом или вообще отсутствовало, начинается запуск
        return actualInterval > expectedIntervalMinutes;

    }

    public void logExecution(String taskName, OffsetDateTime executionTime, String status) {
        TaskExecutionLog logEntry = new TaskExecutionLog(taskName, executionTime, status);
        logRepository.save(logEntry);
    }

    @Transactional
    public boolean setMonthlyLimitByDefault() {
        try {
            Limit limit = new Limit();
            limit.setLimitSum(BigDecimal.valueOf(1000));
            limit.setCurrency(USD);
            limit.setBaseLimit(true);
            limit.setActive(true);

            Optional<Limit> savedLimit = limitRepository.saveWithOptional(limit);
            if (savedLimit.isPresent()) {
                log.info("Default limit saved successfully: {}", savedLimit);
                return true;
            } else {
                log.warn("Limit was not saved: {}", limit);
                return false;
            }
        } catch (Exception e) {
            log.error("Unexpected error in method insertMonthlyLimit: {}", e.getMessage(), e);
            return false;
        }
    }

    @Transactional
    public void updateLimitStatusIsActive() {
        limitRepository.updateStatusIsActive();
    }


    //methods for controller можно вынести в отельный сервис но не хочу увеличивать количество файлов из за двух методов.
    private volatile boolean schedulerEnabled = true;

    public boolean isSchedulerEnabled() {
        return schedulerEnabled;
    }

    public void setSchedulerEnabled(boolean enabled) {
        this.schedulerEnabled = enabled;
    }
}
