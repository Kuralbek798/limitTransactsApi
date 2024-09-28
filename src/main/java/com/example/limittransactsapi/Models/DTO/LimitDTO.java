package com.example.limittransactsapi.Models.DTO;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
public class LimitDTO {

    private final UUID id;

    @NotNull(message = "Limit sum must not be null")
    private final BigDecimal limitSum;

    @NotNull(message = "Limit currency must not be null")
    private final String currency;

    private final OffsetDateTime datetime;

    @NotNull(message = "Client ID must not be null")
    private final UUID clientId;

    private final boolean isBaseLimit;

    @JsonCreator
    public LimitDTO(
            UUID id, @JsonProperty("limitSum") BigDecimal limitSum,
            @JsonProperty("currency") String currency,
            @JsonProperty("datetime") OffsetDateTime datetime,
            @JsonProperty("clientId") UUID clientId,
            @JsonProperty("isBaseLimit") boolean isBaseLimit) {
        this.id = id;
        this.limitSum = limitSum;
        this.currency = currency;
        this.datetime = datetime;
        this.clientId = clientId;
        this.isBaseLimit = isBaseLimit;
    }
}