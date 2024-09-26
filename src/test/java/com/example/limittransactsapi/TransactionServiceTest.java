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
        
        limitDTO = new LimitDTO(UUID.randomUUID(), new BigDecimal("1000.00"), "USD", OffsetDateTime.now());
    }

    @Test
    void testAdditionTransactionsWhenWithinLimit() {

        Map<Integer, BigDecimal> comparerExamplesDB = Map.of(1, BigDecimal.ZERO);
        ConcurrentLinkedQueue<TransactionDTO> transactions = new ConcurrentLinkedQueue<>();


        TransactionDTO transaction1 = new TransactionDTO(
                UUID.randomUUID(),
                new BigDecimal("500.00"),
                "USD",
                OffsetDateTime.now(),
                1,
                2,
                "Category1",
                OffsetDateTime.now(),
                new BigDecimal("1.0"),
                new BigDecimal("500.00"),
                "USD",
                false
        );

        transactions.add(transaction1);

        // Подготовка входящих данных
        Map<Integer, ConcurrentLinkedQueue<TransactionDTO>> clientsTransactions = Map.of(1, transactions);

        // Настраиваем возвращаемое значение для saveCheckedOnLimitAsync
        UUID checkedOnLimitId = UUID.randomUUID();
        CheckedOnLimitDTO checkedOnLimitDTO = new CheckedOnLimitDTO(
                checkedOnLimitId,
                transaction1.getId(),
                limitDTO.getId(),
                false,
                OffsetDateTime.now()
        );

        when(checkedOnLimitService.saveCheckedOnLimitAsync(any())).thenReturn(CompletableFuture.completedFuture(checkedOnLimitDTO));

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
                new BigDecimal("1200.00"),  // Сумма превышающая лимит
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

        UUID checkedOnLimitId = UUID.randomUUID();
        CheckedOnLimitDTO checkedOnLimitDTO = new CheckedOnLimitDTO(
                checkedOnLimitId,
                transaction1.getId(),
                limitDTO.getId(),
                false,
                OffsetDateTime.now()
        );

        // Настраиваем возвращаемое значение для saveCheckedOnLimitAsync
        when(checkedOnLimitService.saveCheckedOnLimitAsync(any())).thenReturn(CompletableFuture.completedFuture(checkedOnLimitDTO));

        // Выполнение тестируемого метода
        CompletableFuture<Void> result = transactionService.additionTransactionsWithComparisonOnLimit(comparerExamplesDB, clientsTransactions, limitDTO);

        // Проверка, что сохранение с превышением лимита было вызвано
        verify(checkedOnLimitService, times(1)).saveCheckedOnLimitAsync(any(CheckedOnLimit.class));
        verify(transactionCRUDService, times(1)).saveTransactionWithHandling(any());
    }
}

