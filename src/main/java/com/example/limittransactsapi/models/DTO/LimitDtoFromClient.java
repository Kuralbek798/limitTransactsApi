package com.example.limittransactsapi.models.DTO;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotNull;

import lombok.Getter;


import java.math.BigDecimal;

import java.util.UUID;

@Getter
public final class LimitDtoFromClient {

    @NotNull(message = "Limit sum must not be null")
    private final BigDecimal limitSum;

    @NotNull(message = "Limit currency must not be null")
    private final String currency;

    @NotNull(message = "Client ID must not be null")
    private final UUID clientId;
    @JsonCreator
    public LimitDtoFromClient(
            @JsonProperty("limitSum")  BigDecimal limitSum,
            @JsonProperty("currency")  String currency,
            @JsonProperty("clientId")  UUID clientId) {
        this.limitSum = limitSum;
        this.currency = currency;
        this.clientId = clientId;
    }


}
