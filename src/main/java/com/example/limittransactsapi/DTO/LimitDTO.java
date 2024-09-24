package com.example.limittransactsapi.DTO;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class LimitDTO {
    private UUID id;
    private BigDecimal limitSum;
    private String currency;
    private OffsetDateTime datetime;

    // Синхронизированный геттер для id
    public  UUID getId() {
        return id;
    }

    // Синхронизированный сеттер для id
    public  void setId(UUID id) {
        this.id = id;
    }

    // Синхронизированный геттер для limitAmount
    public  BigDecimal getLimitSum() {
        return limitSum;
    }

    // Синхронизированный сеттер для limitAmount
    public  void setLimitSum(BigDecimal limitSum) {
        this.limitSum = limitSum;
    }

    // Синхронизированный геттер для limitCurrency
    public  String getCurrency() {
        return currency;
    }

    // Синхронизированный сеттер для limitCurrency
    public  void setCurrency(String currency) {
        this.currency = currency;
    }

    // Синхронизированный геттер для dateTime
    public  OffsetDateTime getDatetime() {
        return datetime;
    }

    // Синхронизированный сеттер для dateTime
    public  void setDatetime(OffsetDateTime datetime) {
        this.datetime = datetime;
    }
}
