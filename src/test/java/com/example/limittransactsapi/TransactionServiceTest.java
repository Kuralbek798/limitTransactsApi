/*
package com.example.limittransactsapi;

import com.example.limittransactsapi.DTO.*;
import com.example.limittransactsapi.repository.TransactionRepository;
import com.example.limittransactsapi.services.*;
import com.example.limittransactsapi.services.crud.TransactionCRUDService;
import com.example.limittransactsapi.util.ConverterUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private LimitService limitService;

    @Mock
    private CheckedOnLimitService checkedOnLimitService;

    @Mock
    private ConverterUtil converterUtil;

    @Mock
    private ExchangeRateService exchangeRateService;

    @Mock
    private TransactionCRUDService transactionCRUDService;

    private TransactionService transactionService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        transactionService = new TransactionService(
                transactionRepository,
                limitService,
                checkedOnLimitService,
                converterUtil,
                exchangeRateService,
                Runnable::run,
                transactionCRUDService
        );
    }

    @Test
    void testSetTransactionsToDB_whenTransactionsListIsEmpty() {
        CompletableFuture<ResponseEntity<String>> responseFuture = transactionService.setTransactionsToDB(null);
        ResponseEntity<String> response = responseFuture.join();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isEqualTo("The list was empty");

        responseFuture = transactionService.setTransactionsToDB(Collections.emptyList());
        response = responseFuture.join();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isEqualTo("The list was empty");
    }

    @Test
    void testSetTransactionsToDB_whenTransactionsListIsNotEmpty() {
        // Given
        TransactionDTO transactionDTO = new TransactionDTO();
        List<TransactionDTO> transactions = Collections.singletonList(transactionDTO);
        LimitDTO limitDTO = new LimitDTO();
        limitDTO.setId(UUID.randomUUID());
        limitDTO.setLimitAmount(BigDecimal.valueOf(10000));

        when(limitService.getLatestLimitAsync()).thenReturn(CompletableFuture.completedFuture(limitDTO));
        when(transactionRepository.getTransactionsWithRates(any(UUID.class))).thenReturn(Collections.emptyList());
        when(exchangeRateService.getCurrencyRate(anyString())).thenReturn(CompletableFuture.completedFuture(new ExchangeRateDTO("USD", BigDecimal.ONE)));

        // When
        CompletableFuture<ResponseEntity<String>> responseFuture = transactionService.setTransactionsToDB(transactions);
        ResponseEntity<String> response = responseFuture.join();

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo("Transactions processed successfully");
    }
}
*/
