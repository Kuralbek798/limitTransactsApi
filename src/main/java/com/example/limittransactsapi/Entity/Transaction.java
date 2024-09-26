package com.example.limittransactsapi.Entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jdk.jfr.Name;
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
@Table(name = "transactions")
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "sum", nullable = false)
    private BigDecimal sum;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "datetime_transaction", nullable = false)
    private OffsetDateTime datetimeTransaction;

    @Column(name = "account_from", nullable = false)
    private Integer accountFrom;

    @Column(name = "account_to", nullable = false)
    private Integer accountTo;

    @Column(name = "expense_category", length = 50)
    private String expenseCategory;

}
