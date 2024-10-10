package com.example.limittransactsapi.models.DTO;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.ToString;
import lombok.Value;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Value
@ToString
public class TransactionDTO {

    private final UUID id;
    private final BigDecimal sum;
    private final String currency;
    private final OffsetDateTime datetimeTransaction;
    private final Integer accountFrom;
    private final Integer accountTo;
    private final String expenseCategory;
    private final OffsetDateTime trDate;
    private final BigDecimal exchangeRate;
    private final BigDecimal convertedSum;
    private final String convertedCurrency;
    private final boolean limitExceeded;
}