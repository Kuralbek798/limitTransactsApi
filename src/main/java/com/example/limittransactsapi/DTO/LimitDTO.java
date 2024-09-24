package com.example.limittransactsapi.DTO;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public class LimitDTO {

    private final UUID id;
    private final BigDecimal limitSum;
    private final String currency;
    private final OffsetDateTime datetime;

    @JsonCreator
    public LimitDTO(
            @JsonProperty("id") UUID id,
            @JsonProperty("limitSum") BigDecimal limitSum,
            @JsonProperty("currency") String currency,
            @JsonProperty("datetime") OffsetDateTime datetime) {
        this.id = id;
        this.limitSum = limitSum;
        this.currency = currency;
        this.datetime = datetime;
    }

    // Геттеры
    public UUID getId() {
        return id;
    }

    public BigDecimal getLimitSum() {
        return limitSum;
    }

    public String getCurrency() {
        return currency;
    }

    public OffsetDateTime getDatetime() {
        return datetime;
    }
}