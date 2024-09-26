package com.example.limittransactsapi.repository;


import com.example.limittransactsapi.DTO.TransactionLimitDTO;
import com.example.limittransactsapi.Entity.CheckedOnLimit;

import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CheckedOnLimitRepository extends JpaRepository<CheckedOnLimit, UUID> {

    List<CheckedOnLimit> findAllByLimitId(UUID limitId);

    @Query("SELECT new com.example.limittransactsapi.DTO.TransactionLimitDTO(t.id, t.sum, t.currency, t.accountFrom,t.accountTo, t.expenseCategory, t.datetimeTransaction, l.id, l.limitSum, l.currency, c.limitExceeded,  l.datetime) " +
            "FROM CheckedOnLimit c " +
            "JOIN Transaction t ON c.transactionId = t.id " +
            "JOIN Limit l ON c.limitId = l.id " +
            "WHERE c.limitExceeded = TRUE")
    List<TransactionLimitDTO> findExceededLimits();
}
