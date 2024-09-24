package com.example.limittransactsapi.DTO;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.ToString;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@ToString
public final class CheckedOnLimitDTO {

    private final UUID id;
    private final UUID transactionId;
    private final UUID limitId;
    private final boolean limitExceeded;
    private final OffsetDateTime datetime;

    @JsonCreator
    public CheckedOnLimitDTO(
            @JsonProperty("id") UUID id,
            @JsonProperty("transactionId") UUID transactionId,
            @JsonProperty("limitId") UUID limitId,
            @JsonProperty("limitExceeded") boolean limitExceeded,
            @JsonProperty("datetime") OffsetDateTime datetime) {
        this.id = id;
        this.transactionId = transactionId;
        this.limitId = limitId;
        this.limitExceeded = limitExceeded;
        this.datetime = datetime;
    }
}