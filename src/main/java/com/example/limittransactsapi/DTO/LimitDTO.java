package com.example.limittransactsapi.DTO;

import lombok.NoArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@NoArgsConstructor
@ToString
public class LimitDTO {
    private UUID id;
    private BigDecimal limitAmount;
    private String limitCurrency;
    private OffsetDateTime dateTime;

    // Синхронизированный геттер для id
    public synchronized UUID getId() {
        return id;
    }

    // Синхронизированный сеттер для id
    public synchronized void setId(UUID id) {
        this.id = id;
    }

    // Синхронизированный геттер для limitAmount
    public synchronized BigDecimal getLimitAmount() {
        return limitAmount;
    }

    // Синхронизированный сеттер для limitAmount
    public synchronized void setLimitAmount(BigDecimal limitAmount) {
        this.limitAmount = limitAmount;
    }

    // Синхронизированный геттер для limitCurrency
    public synchronized String getLimitCurrency() {
        return limitCurrency;
    }

    // Синхронизированный сеттер для limitCurrency
    public synchronized void setLimitCurrency(String limitCurrency) {
        this.limitCurrency = limitCurrency;
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
