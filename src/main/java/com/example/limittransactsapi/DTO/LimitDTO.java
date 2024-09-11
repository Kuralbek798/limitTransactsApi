package com.example.limittransactsapi.DTO;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class LimitDTO {

    private BigDecimal limitSum;
    private String limitCurrency;

}
