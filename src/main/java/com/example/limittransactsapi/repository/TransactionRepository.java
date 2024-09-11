package com.example.limittransactsapi.repository;


import com.example.limittransactsapi.Entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.math.BigDecimal;
import java.time.Month;
import java.util.List;


public interface TransactionRepository extends JpaRepository<Transaction, Long> {

   //JPA обработает данный запрос согласно конвенции о наименовании, findByLimitExceededTrue() это тоже самое что SELECT * FROM transactions WHERE limit_exceeded = TRUE;
    //List<Transaction> findByLimitExceededTrue();
    @Query("SELECT SUM(tr.sum) FROM Transaction tr WHERE MONTH(tr.datetime) = ?1 AND YEAR(tr.datetime) = YEAR(CURRENT_DATE)")
    BigDecimal sumAmountsByMonth(Month month);



}



