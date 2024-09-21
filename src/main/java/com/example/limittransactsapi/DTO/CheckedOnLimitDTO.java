package com.example.limittransactsapi.DTO;

import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.OffsetDateTime;
import java.util.UUID;

@NoArgsConstructor
@ToString
public class CheckedOnLimitDTO {
    private UUID id;
    private UUID transactionId;
    private UUID limitId;
    private boolean limitExceeded;
    private OffsetDateTime dateTime;

    // Синхронизированный геттер для id
    public synchronized UUID getId() {
        return id;
    }

    // Синхронизированный сеттер для id
    public synchronized void setId(UUID id) {
        this.id = id;
    }

    // Синхронизированный геттер для transactionId
    public synchronized UUID getTransactionId() {
        return transactionId;
    }

    // Синхронизированный сеттер для transactionId
    public synchronized void setTransactionId(UUID transactionId) {
        this.transactionId = transactionId;
    }

    // Синхронизированный геттер для limitId
    public synchronized UUID getLimitId() {
        return limitId;
    }

    // Синхронизированный сеттер для limitId
    public synchronized void setLimitId(UUID limitId) {
        this.limitId = limitId;
    }

    // Синхронизированный геттер для limitExceeded
    public synchronized boolean isLimitExceeded() {
        return limitExceeded;
    }

    // Синхронизированный сеттер для limitExceeded
    public synchronized void setLimitExceeded(boolean limitExceeded) {
        this.limitExceeded = limitExceeded;
    }

    // Синхронизированный геттер для dateTime
    public synchronized OffsetDateTime getDateTime() {
        return dateTime;
    }

    // Синхронизированный сеттер для dateTime
    public synchronized void setDateTime(OffsetDateTime dateTime) {
        this.dateTime = dateTime;
    }
}
