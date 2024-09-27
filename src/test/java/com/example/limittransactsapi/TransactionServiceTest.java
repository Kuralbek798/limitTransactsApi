/*
package com.example.limittransactsapi;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.example.limittransactsapi.DTO.CheckedOnLimitDTO;
import com.example.limittransactsapi.DTO.LimitDTO;
import com.example.limittransactsapi.DTO.TransactionDTO;
import com.example.limittransactsapi.services.CheckedOnLimitService;
import com.example.limittransactsapi.services.TransactionService;
import com.example.limittransactsapi.services.crud.TransactionCRUDService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;

@ExtendWith(MockitoExtension.class)
public class TransactionServiceTest {

    @Mock
    private CheckedOnLimitService checkedOnLimitService;

    @Mock
    private TransactionCRUDService transactionCRUDService;

    @InjectMocks
    private TransactionService transactionService;

    private LimitDTO limitDTO;

    @BeforeEach
    void setUp() {
        limitDTO = new LimitDTO(UUID.randomUUID(), new BigDecimal("1000.00"), "USD", OffsetDateTime.now());
    }

    @Test
    void testAdditionTransactionsWithConditionalMocking() {
        List<TransactionDTO> transactionsList = List.of(
                new TransactionDTO(UUID.randomUUID(), new BigDecimal("500.00"), "USD", OffsetDateTime.now(), 1, 2, "product", OffsetDateTime.now(), new BigDecimal("1.0"), new BigDecimal("500.00"), "USD", false),
                new TransactionDTO(UUID.randomUUID(), new BigDecimal("1200.00"), "USD", OffsetDateTime.now(), 1, 2, "service", OffsetDateTime.now(), new BigDecimal("1.0"), new BigDecimal("1200.00"), "USD", false)
        );

        for (TransactionDTO transactionDTO : transactionsList) {
            ConcurrentLinkedQueue<TransactionDTO> transactions = new ConcurrentLinkedQueue<>();
            transactions.add(transactionDTO);

            Map<Integer, ConcurrentLinkedQueue<TransactionDTO>> clientsTransactions = Map.of(1, transactions);


            if (transactionDTO.getConvertedSum().compareTo(limitDTO.getLimitSum()) > 0) {
                lenient().when(checkedOnLimitService.saveCheckedOnLimitAsync(any())).thenReturn(CompletableFuture.completedFuture(
                        new CheckedOnLimitDTO(UUID.randomUUID(), transactionDTO.getId(), limitDTO.getId(), true, null)
                ));
            } else {
                when(checkedOnLimitService.saveCheckedOnLimitAsync(any())).thenReturn(CompletableFuture.completedFuture(
                        new CheckedOnLimitDTO(UUID.randomUUID(), transactionDTO.getId(), limitDTO.getId(), false, null)
                ));
            }

            when(transactionCRUDService.saveTransactionWithHandling(any())).thenReturn(transactionDTO);

            CompletableFuture<Void> result = transactionService.additionTransactionsWithComparisonOnLimit(Map.of(1, BigDecimal.ZERO), clientsTransactions, limitDTO);

            result.join();

            verify(checkedOnLimitService, times(1)).saveCheckedOnLimitAsync(any());
            verify(transactionCRUDService, times(1)).saveTransactionWithHandling(any());


            reset(checkedOnLimitService, transactionCRUDService);
        }
    }
}*/
