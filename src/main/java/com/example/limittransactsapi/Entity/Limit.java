package com.example.limittransactsapi.Entity;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.persistence.*;
import org.springframework.beans.factory.annotation.Value;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Data
@AllArgsConstructor
// @NoArgsConstructor чтобы JPA мог создавать экземпляры сущностей посредством рефлексии и для Jackson

@NoArgsConstructor
@Table(name = "limits")

public class Limit {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "limit_sum", nullable = false)
    private BigDecimal limitSum = BigDecimal.valueOf(1_000.00);

    @Column(name = "limit_currency", nullable = false, length = 3)
    private String limitCurrency = "USD";

    @Column(name = "limit_datetime")
    private OffsetDateTime limitDatetime;

}
