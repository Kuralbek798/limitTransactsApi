package com.example.limittransactsapi.models.DTO;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.Value;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Value
@ToString
public class TransactionLimitDTO {
    private UUID id;
    private UUID transactionId;
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
