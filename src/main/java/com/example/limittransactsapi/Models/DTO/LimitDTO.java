package com.example.limittransactsapi.Models.DTO;

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


    private final UUID clientId;

    private final boolean baseLimit;
    private boolean active;

    @JsonCreator
    public LimitDTO(
            UUID id, @JsonProperty("limitSum") BigDecimal limitSum,
            @JsonProperty("currency") String currency,
            @JsonProperty("datetime") OffsetDateTime datetime,
            @JsonProperty("clientId") UUID clientId,
            @JsonProperty("baseLimit") boolean baseLimit,
            @JsonProperty("active") boolean active) {
        this.id = id;
        this.limitSum = limitSum;
        this.currency = currency;
        this.datetime = datetime;
        this.clientId = clientId;
        this.baseLimit = baseLimit;
        this.active = active;
    }

    public UUID getId() {
        return this.id;
    }

    public BigDecimal getLimitSum() {
        return this.limitSum;
    }

    public String getCurrency() {
        return this.currency;
    }

    public OffsetDateTime getDatetime() {
        return this.datetime;
    }

    public UUID getClientId() {
        return this.clientId;
    }

    public boolean isBaseLimit() {
        return this.baseLimit;
    }

    public boolean isActive() {
        return this.active;
    }
}