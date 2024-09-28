package com.example.limittransactsapi.Models.Entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;


@EqualsAndHashCode
@Data
@Entity
@Table(name = "exchange_rates")
public class ExchangeRate {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "currency_pair", nullable = false)
    private String currencyPair;

    @Column(name = "rate", nullable = false)
    private BigDecimal rate;

    @Column(name = "close", nullable = false)
    private BigDecimal close;

    @Column(name = "datetime_rate", nullable = false)
    private OffsetDateTime dateTimeRate;


    public ExchangeRate(UUID id, String currencyPair, BigDecimal rate, BigDecimal close, OffsetDateTime dateTimeRate) {
        this.id = id;
        this.currencyPair = currencyPair;
        this.rate = rate;
        this.close = close;
        this.dateTimeRate = dateTimeRate;
    }

    public ExchangeRate() {
    }
}
