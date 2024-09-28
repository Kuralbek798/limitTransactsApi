package com.example.limittransactsapi.Models.Entity;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CheckedOnLimit {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
   private UUID id;

    @Column(name = "transaction_id", nullable = false)
    private UUID transactionId;

    @Column(name = "limit_id", nullable = false)
    private UUID limitId;

    @Column(name = "limit_exceeded", nullable = false)
    private boolean limitExceeded = false;

    @Column(name = "datetime", nullable = false)
    private OffsetDateTime datetime = OffsetDateTime.now();

    public CheckedOnLimit(UUID transactionId, UUID limitId, boolean limitExceeded) {
        this.transactionId = transactionId;
        this.limitId = limitId;
        this.limitExceeded = limitExceeded;

    }
}
