package com.example.limittransactsapi.Entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
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
    private Double close;

    @Column(name = "datetime", nullable = false)
    private OffsetDateTime datetime;
}
