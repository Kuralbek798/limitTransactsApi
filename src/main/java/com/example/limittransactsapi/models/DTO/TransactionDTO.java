package com.example.limittransactsapi.models.DTO;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;


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

    @JsonCreator
    public TransactionDTO(
            @JsonProperty("id") UUID id,
            @JsonProperty("sum") BigDecimal sum,
            @JsonProperty("currency") String currency,
            @JsonProperty("datetimeTransaction") OffsetDateTime datetimeTransaction,
            @JsonProperty("accountFrom") Integer accountFrom,
            @JsonProperty("accountTo") Integer accountTo,
            @JsonProperty("expenseCategory") String expenseCategory,
            @JsonProperty("trDate") OffsetDateTime trDate,
            @JsonProperty("exchangeRate") BigDecimal exchangeRate,
            @JsonProperty("convertedSum") BigDecimal convertedSum,
            @JsonProperty("convertedCurrency") String convertedCurrency,
            @JsonProperty("limitExceeded") boolean limitExceeded) {
        this.id = id;
        this.sum = sum;
        this.currency = currency;
        this.datetimeTransaction = datetimeTransaction;
        this.accountFrom = accountFrom;
        this.accountTo = accountTo;
        this.expenseCategory = expenseCategory;
        this.trDate = trDate;
        this.exchangeRate = exchangeRate;
        this.convertedSum = convertedSum;
        this.convertedCurrency = convertedCurrency;
        this.limitExceeded = limitExceeded;
    }


    public UUID getId() {
        return id;
    }

    public BigDecimal getSum() {
        return sum;
    }

    public String getCurrency() {
        return currency;
    }

    public OffsetDateTime getDatetimeTransaction() {
        return datetimeTransaction;
    }

    public Integer getAccountFrom() {
        return accountFrom;
    }

    public Integer getAccountTo() {
        return accountTo;
    }

    public String getExpenseCategory() {
        return expenseCategory;
    }

    public OffsetDateTime getTrDate() {
        return trDate;
    }

    public BigDecimal getExchangeRate() {
        return exchangeRate;
    }

    public BigDecimal getConvertedSum() {
        return convertedSum;
    }
    public String getConvertedCurrency() {
        return convertedCurrency;
    }

    public boolean isLimitExceeded() {
        return limitExceeded;
    }
}