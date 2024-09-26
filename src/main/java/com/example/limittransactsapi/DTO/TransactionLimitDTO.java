package com.example.limittransactsapi.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
public class TransactionLimitDTO {
    private UUID id;
    private UUID transactionId;

    public TransactionLimitDTO(UUID transactionId, BigDecimal transactionSum,
                               String transactionCurrency, Integer accountFrom, Integer accountTo,
                               String expenseCategory, OffsetDateTime transactionDate, UUID limitId,
                               BigDecimal limitSum, String currency, boolean limitExceeded, OffsetDateTime limitSetDate) {
        this.transactionId = transactionId;
        this.transactionSum = transactionSum;
        this.transactionCurrency = transactionCurrency;
        this.accountFrom = accountFrom;
        this.accountTo = accountTo;
        this.expenseCategory = expenseCategory;
        this.transactionDate = transactionDate;
        this.limitId = limitId;
        this.limitSum = limitSum;
        this.currency = currency;
        this.limitExceeded = limitExceeded;
        this.limitSetDate = limitSetDate;
    }

    private BigDecimal transactionSum;
    private String transactionCurrency;
    private Integer accountFrom;
    private Integer accountTo;
    private String expenseCategory;
    private OffsetDateTime transactionDate;
    private UUID limitId;
    private BigDecimal limitSum;
    private String currency;
    private boolean limitExceeded;
    private OffsetDateTime limitSetDate;




}
