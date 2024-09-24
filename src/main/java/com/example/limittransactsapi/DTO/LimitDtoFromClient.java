package com.example.limittransactsapi.DTO;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public final class LimitDtoFromClient {

    private final BigDecimal limitSum;
    private final String currency;

    @JsonCreator
    public LimitDtoFromClient(
            @JsonProperty("limitSum") @NotNull(message = "Limit sum must not be null") BigDecimal limitSum,
            @JsonProperty("currency") @NotNull(message = "Limit currency must not be null") String currency) {
        this.limitSum = limitSum;
        this.currency = currency;
    }

    // Getters
    public BigDecimal getLimitSum() {
        return limitSum;
    }

    public String getCurrency() {
        return currency;
    }
}