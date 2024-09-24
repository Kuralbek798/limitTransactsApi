package com.example.limittransactsapi.DTO;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
public class TransactionDTO {

    private UUID id;
    private BigDecimal sum;
    private String currency;
    private OffsetDateTime datetimeTransaction;
    private Integer accountFrom;
    private Integer accountTo;
    private String expenseCategory;
    private OffsetDateTime trDate;
    private BigDecimal exchangeRate;
    private BigDecimal convertedSum;
    private String convertedCurrency;
    private boolean limitExceeded = false;

    public TransactionDTO(BigDecimal convertedSum,String convertedCurrency, String expenseCategory, Integer accountFrom, BigDecimal sum) {
      this.convertedSum = convertedSum;
        this.convertedCurrency = convertedCurrency;
        this.expenseCategory = expenseCategory;
        this.accountFrom = accountFrom;
        this.sum = sum;
    }

    public synchronized UUID getId() {
        return id;
    }

    public synchronized void setId(UUID id) {
        this.id = id;
    }

    public synchronized BigDecimal getSum() {
        return sum;
    }

    public synchronized void setSum(BigDecimal sum) {
        this.sum = sum;
    }

    public synchronized String getCurrency() {
        return currency;
    }

    public synchronized void setCurrency(String currency) {
        this.currency = currency;
    }

    public synchronized OffsetDateTime getDatetimeTransaction() {
        return datetimeTransaction;
    }

    public synchronized void setDatetimeTransaction(OffsetDateTime datetimeTransaction) {
        this.datetimeTransaction = datetimeTransaction;
    }

    public synchronized Integer getAccountFrom() {
        return accountFrom;
    }

    public synchronized void setAccountFrom(Integer accountFrom) {
        this.accountFrom = accountFrom;
    }

    public synchronized Integer getAccountTo() {
        return accountTo;
    }

    public synchronized void setAccountTo(Integer accountTo) {
        this.accountTo = accountTo;
    }

    public synchronized String getExpenseCategory() {
        return expenseCategory;
    }

    public synchronized void setExpenseCategory(String expenseCategory) {
        this.expenseCategory = expenseCategory;
    }

    public synchronized OffsetDateTime getTrDate() {
        return trDate;
    }

    public synchronized void setTrDate(OffsetDateTime trDate) {
        this.trDate = trDate;
    }

    public synchronized BigDecimal getExchangeRate() {
        return exchangeRate;
    }

    public synchronized void setExchangeRate(BigDecimal exchangeRate) {
        this.exchangeRate = exchangeRate;
    }

    public synchronized BigDecimal getConvertedSum() {
        return convertedSum;
    }

    public synchronized void setConvertedSum(BigDecimal convertedSum) {
        this.convertedSum = convertedSum;
    }

    public synchronized String getConvertedCurrency() {
        return convertedCurrency;
    }

    public synchronized void setConvertedCurrency(String convertedCurrency) {
        this.convertedCurrency = convertedCurrency;
    }

    public synchronized boolean isLimitExceeded() {
        return limitExceeded;
    }

    public synchronized void setLimitExceeded(boolean limitExceeded) {
        this.limitExceeded = limitExceeded;
    }
}
