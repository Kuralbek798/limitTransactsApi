package com.example.limittransactsapi.Entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;


@EqualsAndHashCode
@Data
@Entity
@Table(name = "exchange_rates")
@AllArgsConstructor
@NoArgsConstructor
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

    @Column(name = "datetime_rate", nullable = false)
    private OffsetDateTime datetimeRate;





}
