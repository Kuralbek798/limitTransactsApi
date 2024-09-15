package com.example.limittransactsapi.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class  LimitDTO {
    private UUID id;
    private BigDecimal limitAmount;
    private String limitCurrency;
    private OffsetDateTime dateTime;

    //for test
    public LimitDTO(BigDecimal limitAmount, UUID id) {
        this.limitAmount = limitAmount;
        this.id = id;
    }
}
