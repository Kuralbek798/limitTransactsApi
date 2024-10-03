package com.example.limittransactsapi.repository;

import com.example.limittransactsapi.models.entity.TaskExecutionLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TaskExecutionLogRepository extends JpaRepository<TaskExecutionLog, UUID> {
    Optional<TaskExecutionLog> findByTaskName(String taskName);
}
