package com.example.limittransactsapi.services.crud;

import com.example.limittransactsapi.DTO.TransactionDTO;
import com.example.limittransactsapi.Entity.Transaction;
import com.example.limittransactsapi.mapper.TransactionMapper;
import com.example.limittransactsapi.repository.TransactionRepository;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.CompletableFuture;
@Service
@Slf4j
public class TransactionCRUDService {

private final TransactionRepository transactionRepository;

    @Autowired
    public TransactionCRUDService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @Transactional
    public TransactionDTO saveTransactionWithHandling(Transaction transaction) {
        if (transaction == null) {
            log.error("Transaction object is null.");
            throw new IllegalArgumentException();
        }

        int maxAttempts = 3;
        int attempt = 0;
        Exception lastException = null;

        while (attempt < maxAttempts) {
            try {
                var savedTr = transactionRepository.save(transaction);
                // Здесь мы убираем условие проверяющее savedTr на null
                return TransactionMapper.INSTANCE.toDTO(savedTr); // Если метод завершился без исключений, возвращаем DTO
            } catch (Exception e) {
                lastException = e;
                attempt++;
                log.error("Attempt " + attempt + " failed to save transaction. Error: " + e.getMessage());
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    log.error("Thread was interrupted during sleep. Error: " + ie.getMessage());
                }
            }
        }

        // Ошибка не была обработана после трех попыток
        log.error("Error saving transaction after " + maxAttempts + " attempts: " + lastException.getMessage());
        return null; // Если мы доходим сюда, значит сохранение не удалось
    }


}
