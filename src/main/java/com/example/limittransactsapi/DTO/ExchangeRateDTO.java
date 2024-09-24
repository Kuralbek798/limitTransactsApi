package com.example.limittransactsapi.DTO;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Getter
@ToString
public final class ExchangeRateDTO {

    private final String currencyPair;
    private final BigDecimal rate;
    private final OffsetDateTime dateTimeRate;

    @JsonCreator
    public ExchangeRateDTO(
            @JsonProperty("currencyPair") String currencyPair,
            @JsonProperty("rate") BigDecimal rate,
            @JsonProperty("dateTimeRate") OffsetDateTime dateTimeRate) {
        this.currencyPair = currencyPair;
        this.rate = rate;
        this.dateTimeRate = dateTimeRate;
    }


}