package com.example.limittransactsapi.DTO;

import lombok.Getter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Getter
@ToString
public class ExchangeRateDTO {

    private final String currencyPair;
    private final BigDecimal rate;
    private  OffsetDateTime dateTimeRate;


    public ExchangeRateDTO(String currencyPair, BigDecimal rate) {
        this.currencyPair = currencyPair;
        this.rate = rate;

    }

    // Синхронизированный сеттер для dateTimeRate
    public synchronized void setDateTimeRate(OffsetDateTime dateTimeRate) {
        this.dateTimeRate = dateTimeRate;
    }

    // Геттер для dateTimeRate
    public synchronized OffsetDateTime getDateTimeRate() {
        return this.dateTimeRate;
    }

    // Метод для установки значения dateTimeRate на текущее время
    public synchronized void setCurrentDateTimeRate() {
        this.dateTimeRate = OffsetDateTime.now();
    }

}
