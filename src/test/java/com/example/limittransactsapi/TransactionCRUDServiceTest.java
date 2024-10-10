/*
package com.example.limittransactsapi;

import static org.mockito.Mockito.*;

import com.example.limittransactsapi.helpers.mapper.TransactionMapper;
import com.example.limittransactsapi.models.DTO.TransactionDTO;
import com.example.limittransactsapi.models.entity.Transaction;
import com.example.limittransactsapi.repository.TransactionRepository;
import com.example.limittransactsapi.services.crud.TransactionCRUDService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.springframework.test.util.ReflectionTestUtils;


import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TransactionCRUDServiceTest {
    @Mock // имитируем репозиторий
    private TransactionRepository transactionRepository;

    @Mock // имитируем логер
    private Logger logger;

    @InjectMocks // внедряем
    private TransactionCRUDService transactionCRUDService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

    }

    @Test
     void shouldThrowExceptionWhenTransactionIsNull() {
        IllegalArgumentException exception = Assertions.assertThrows(IllegalArgumentException.class, () -> {
            transactionCRUDService.saveTransactionWithHandling(null);
        });
        Assertions.assertNotNull(exception);
        Mockito.verify(logger).error("Transaction object is null.");

    }

    void shouldSaveTransactionOnFirstAttempt(){
        Transaction transaction = new Transaction();
        TransactionDTO transactionDTO = new TransactionDTO();

        when(transactionRepository.save(transaction)).thenReturn(transaction);

        TransactionMapper INSTANCE = Mockito.mock(TransactionMapper.class);
        when(INSTANCE.toDTO(transaction)).thenReturn(transactionDTO);
        ReflectionTestUtils.setField(TransactionMapper.class, "INSTANCE", INSTANCE);

        TransactionDTO result = transactionCRUDService.saveTransactionWithHandling(transaction);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(transactionDTO, result);
    }
}
*/
