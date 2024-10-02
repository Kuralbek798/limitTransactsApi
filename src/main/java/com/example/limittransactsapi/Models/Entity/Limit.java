package com.example.limittransactsapi.Models.Entity;


import jakarta.persistence.*;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Data
@AllArgsConstructor

@NoArgsConstructor
@Table(name = "limits")

public class Limit {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "limit_sum", nullable = false)
    private BigDecimal limitSum;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency = "USD";

    @Column(name = "datetime", nullable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private OffsetDateTime datetime;

    @Column(name = "client_id")
    private UUID clientId;

    @Column(name = "is_base_limit")
    private boolean baseLimit;

    @Column(name = "is_active")
    private boolean active = true;

}
