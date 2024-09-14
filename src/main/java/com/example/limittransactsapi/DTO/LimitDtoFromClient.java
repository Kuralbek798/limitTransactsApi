package com.example.limittransactsapi.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LimitDtoFromClient {
    private BigDecimal limitSum;
    private String limitCurrency;
}
