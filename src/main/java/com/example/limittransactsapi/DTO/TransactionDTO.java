package com.example.limittransactsapi.DTO;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class TransactionDTO {


    private UUID id;
    private BigDecimal amount;
    private String currency;
    private OffsetDateTime datetimeTransaction;
    private Integer accountFrom;
    private Integer accountTo;
    private String expenseCategory;
    private OffsetDateTime trDate;
    private BigDecimal exchangeRate;
    private BigDecimal sum;

    public TransactionDTO(  String currency, String expenseCategory, Integer accountFrom, BigDecimal sum) {
        this.currency = currency;
        this.expenseCategory = expenseCategory;
        this.accountFrom = accountFrom;
        this.sum = sum;
    }

}
