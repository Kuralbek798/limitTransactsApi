package com.example.limittransactsapi.Models;

import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
public class RateDataFromJson {
    private BigDecimal closeRate;
    private OffsetDateTime dateTime;
}
