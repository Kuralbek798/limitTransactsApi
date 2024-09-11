package com.example.limittransactsapi.services;


import com.example.limittransactsapi.DTO.LimitDTO;
import com.example.limittransactsapi.Entity.Transaction;
import com.example.limittransactsapi.repository.LimitRepository;
import com.example.limittransactsapi.repository.TransactionRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.StoredProcedureQuery;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class TransactionService {


    private final TransactionRepository transactionRepository;
    private final LimitService limitService;

    @Autowired
    public TransactionService(TransactionRepository transactionRepository, LimitRepository limitRepository, LimitService limitService) {
        this.transactionRepository = transactionRepository;
        this.limitService = limitService;

    }

    //Creates transactions
    public Transaction createTransaction(Transaction transaction) {
        try {
            //receiving current limit for  category.
            Optional<LimitDTO> OptionalCurrentLimitDto = limitService.getLatestLimitInOptionalLimitDto();
            if (OptionalCurrentLimitDto.isPresent()) {
                BigDecimal limitSum = OptionalCurrentLimitDto.get().getLimitSum();

                // Calculating expenses for current month
                BigDecimal totalSpent = transactionRepository.sumAmountsByMonth(transaction.getDatetime().getMonth());

                //checking limit
                if(totalSpent.add(transaction.getSum()).compareTo(limitSum) > 0) {
                    transaction.setLimitExceeded(true);
                    log.warn("Transaction exceeded limit: {}.Total spent: {}, Limit: {}", transaction.getSum(), totalSpent, limitSum);
                }else{
                    transaction.setLimitExceeded(false);
                }

                log.info("Transaction Created: {}", transaction);

                return transactionRepository.save(transaction);
            }

return null;

        } catch (Exception e) {
            log.error(e.getMessage());
            throw new RuntimeException("Transaction could not be created, exception occurred in method createTransaction" + e.getMessage());
        }
    }


    @PersistenceContext
    private EntityManager entityManager; // Инъекция EntityManager для работы с хранимыми процедурами

    // Метод для вызова хранимой процедуры
    public List<Transaction> getExceedingTransactions() {
        StoredProcedureQuery query = entityManager.createStoredProcedureQuery("GetExceedingTransactions", Transaction.class);
        return query.getResultList();
    }
}
