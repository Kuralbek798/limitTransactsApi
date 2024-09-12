package com.example.limittransactsapi.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExchangeRateDTO {

    private String currencyPair;
    private BigDecimal rate;
    private OffsetDateTime date;

}
