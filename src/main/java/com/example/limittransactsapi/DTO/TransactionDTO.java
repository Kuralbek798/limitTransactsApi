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
public class TransactionDTO {


    private BigDecimal sum;
    private String currency;
    private OffsetDateTime datetime;
    private Integer accountFrom;
    private Integer accountTo;
    private String expenseCategory;
}
