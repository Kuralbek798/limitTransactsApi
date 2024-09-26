package com.example.limittransactsapi;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.example.limittransactsapi.DTO.CheckedOnLimitDTO;
import com.example.limittransactsapi.DTO.LimitDTO;
import com.example.limittransactsapi.DTO.TransactionDTO;
import com.example.limittransactsapi.Entity.CheckedOnLimit;
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
import java.util.Map;
import java.util.UUID;
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
        // Создаем LimitDTO с использованием конструктора
        limitDTO = new LimitDTO(UUID.randomUUID(), new BigDecimal("1000.00"), "USD", OffsetDateTime.now());
    }

    @Test
    void testAdditionTransactionsWhenWithinLimit() {
        // Параметры теста
        Map<Integer, BigDecimal> comparerExamplesDB = Map.of(1, BigDecimal.ZERO);
        ConcurrentLinkedQueue<TransactionDTO> transactions = new ConcurrentLinkedQueue<>();

        // Создание TransactionDTO с использованием всех необходимых параметров
        TransactionDTO transaction1 = new TransactionDTO(
                UUID.randomUUID(),  // ID транзакции
                new BigDecimal("500.00"),  // Сумма в пределах лимита
                "USD",  // Валюта
                OffsetDateTime.now(),  // Текущее время
                1,  // accountFrom
                2,  // accountTo
                "Category1",  // expenseCategory
                OffsetDateTime.now(),  // trDate
                new BigDecimal("1.0"),  // exchangeRate
                new BigDecimal("500.00"),  // convertedSum
                "USD",  // convertedCurrency
                false  // limitExceeded
        );

        transactions.add(transaction1);

        // Подготовка входящих данных
        Map<Integer, ConcurrentLinkedQueue<TransactionDTO>> clientsTransactions = Map.of(1, transactions);

        // Настраиваем возвращаемое значение для saveTransactionalAsync
        when(transactionCRUDService.saveTransactionWithHandling(any())).thenReturn(transaction1);

        // Настраиваем возвращаемое значение для saveCheckedOnLimitAsync
        when(checkedOnLimitService.saveCheckedOnLimitAsync(any())).thenReturn(CompletableFuture.completedFuture(
                new CheckedOnLimitDTO(UUID.randomUUID(), transaction1.getId(), limitDTO.getId(), false, OffsetDateTime.now())
        ));

        // Выполнение тестируемого метода
        CompletableFuture<Void> result = transactionService.additionTransactionsWithComparisonOnLimit(comparerExamplesDB, clientsTransactions, limitDTO);

        // Проверка, что операции сохранения были вызваны
        verify(checkedOnLimitService, times(1)).saveCheckedOnLimitAsync(any());
        verify(transactionCRUDService, times(1)).saveTransactionWithHandling(any());
    }

    @Test
    void testAdditionTransactionsWhenExceedsLimit() {
        // Параметры теста
        Map<Integer, BigDecimal> comparerExamplesDB = Map.of(1, BigDecimal.ZERO);
        ConcurrentLinkedQueue<TransactionDTO> transactions = new ConcurrentLinkedQueue<>();

        // Создание TransactionDTO с использованием всех необходимых параметров (для превышения лимита)
        TransactionDTO transaction1 = new TransactionDTO(
                UUID.randomUUID(),  // ID транзакции
                new BigDecimal("1200.00"),  // Сумма превышает лимит
                "USD",  // Валюта
                OffsetDateTime.now(),  // Текущее время
                1,  // accountFrom
                2,  // accountTo
                "Category2",  // expenseCategory
                OffsetDateTime.now(),  // trDate
                new BigDecimal("1.0"),  // exchangeRate
                new BigDecimal("1200.00"),  // convertedSum
                "USD",  // convertedCurrency
                false  // limitExceeded
        );

        transactions.add(transaction1);

        // Подготовка входящих данных
        Map<Integer, ConcurrentLinkedQueue<TransactionDTO>> clientsTransactions = Map.of(1, transactions);

        // Настраиваем возвращаемое значение для сохраненной транзакции
        when(transactionCRUDService.saveTransactionWithHandling(any())).thenReturn(transaction1);

        // Настраиваем возвращаемое значение для saveCheckedOnLimitAsync с превышением лимита
        when(checkedOnLimitService.saveCheckedOnLimitAsync(any())).thenReturn(CompletableFuture.completedFuture(
                new CheckedOnLimitDTO(UUID.randomUUID(), transaction1.getId(), limitDTO.getId(), true, OffsetDateTime.now())
        ));

        // Выполнение тестируемого метода
        CompletableFuture<Void> result = transactionService.additionTransactionsWithComparisonOnLimit(comparerExamplesDB, clientsTransactions, limitDTO);

        // Проверка, что сохранение с превышением лимита было вызвано
        verify(checkedOnLimitService, times(1)).saveCheckedOnLimitAsync(any(CheckedOnLimit.class));
        verify(transactionCRUDService, times(1)).saveTransactionWithHandling(any());
    }
}
