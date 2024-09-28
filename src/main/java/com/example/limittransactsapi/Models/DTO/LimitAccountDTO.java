package com.example.limittransactsapi.Models.DTO;

import lombok.Getter;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
public class LimitAccountDTO {

    private final UUID id;
    private final BigDecimal limitSum;
    private final String limitCurrency;
    private final OffsetDateTime dateTime;
    private final UUID clientId;
    private final boolean isBaseLimit;
    private final boolean isActive;
    private final Integer accountNumber;

    @JsonCreator
    public LimitAccountDTO(
            @JsonProperty("id") UUID id,
            @JsonProperty("limitSum") BigDecimal limitSum,
            @JsonProperty("limitCurrency") String limitCurrency,
            @JsonProperty("dateTime") OffsetDateTime dateTime,
            @JsonProperty("clientId") UUID clientId,
            @JsonProperty("isBaseLimit") boolean isBaseLimit,
            @JsonProperty("isActive") boolean isActive,
            @JsonProperty("accountNumber") Integer accountNumber) {
        this.id = id;
        this.limitSum = limitSum;
        this.limitCurrency = limitCurrency;
        this.dateTime = dateTime;
        this.clientId = clientId;
        this.isBaseLimit = isBaseLimit;
        this.isActive = isActive;
        this.accountNumber = accountNumber;
    }
}
