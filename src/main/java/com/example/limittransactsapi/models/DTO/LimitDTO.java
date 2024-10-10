package com.example.limittransactsapi.models.DTO;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.ToString;
import lombok.Value;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
@Value
@ToString
public class LimitDTO {

    private final UUID id;
    private final BigDecimal limitSum;
    private final String currency;
    private final OffsetDateTime datetime;
    private final UUID clientId;
    private final boolean baseLimit;
    private boolean active;

}