package com.example.limittransactsapi.repository;



import com.example.limittransactsapi.Models.Entity.Transaction;
import com.example.limittransactsapi.repository.projections.TransactionProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;


import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {




   @Query(value = "SELECT " +
           "id AS id, " +
           "sum AS sum, " +
           "currency AS currency, " +
           "datetime_transaction AS datetimeTransaction, " +
           "account_from AS accountFrom, " +
           "account_to AS accountTo, " +
           "expense_category AS expenseCategory, " +
           "tr_date AS trDate, " +
           "exchange_rate AS exchangeRate " +
           "FROM get_transactions_with_rates(:limitId)",
           nativeQuery = true)
   ConcurrentLinkedQueue<TransactionProjection> findTransactionsWithRatesByLimitId(UUID limitId);



}



