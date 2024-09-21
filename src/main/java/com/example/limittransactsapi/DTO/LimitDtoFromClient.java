package com.example.limittransactsapi.DTO;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LimitDtoFromClient {

    @NotNull(message = "Сумма лимита не должна быть пустой")
    private BigDecimal limitSum;
    @NotNull(message = "Валюта лимита не должна быть пустой")
    private String limitCurrency;
}