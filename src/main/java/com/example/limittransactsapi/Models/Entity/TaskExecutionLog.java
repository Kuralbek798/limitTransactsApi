package com.example.limittransactsapi.Models.Entity;

import jakarta.persistence.*;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "task_execution_log")
public class TaskExecutionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    @Column(name = "task_name", nullable = false)
    private String taskName;

    @Column(name = "last_execution_time", nullable = false)
    private OffsetDateTime lastExecutionTime = OffsetDateTime.now();

    @Column(name = "status", nullable = false)
    private String status;

    public TaskExecutionLog(String taskName, OffsetDateTime executionTime, String status) {
        this.taskName = taskName;
        this.lastExecutionTime = executionTime;
        this.status = status;
    }
}


