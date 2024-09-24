package com.example.limittransactsapi.repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public interface TransactionProjection {
    UUID getId();
    BigDecimal getSum();
    String getCurrency();
    Instant getDatetimeTransaction(); // Замените на Instant
    Integer getAccountFrom();
    Integer getAccountTo();
    String getExpenseCategory();
    Instant getTrDate(); // Замените на Instant
    BigDecimal getExchangeRate();

}
