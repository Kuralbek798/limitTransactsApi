package com.example.limittransactsapi;

import com.example.limittransactsapi.models.DTO.LimitDTO;

import com.example.limittransactsapi.models.entity.Limit;
import com.example.limittransactsapi.repository.LimitRepository;
import com.example.limittransactsapi.services.LimitService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class LimitServiceTest {

    @Mock
    private LimitRepository limitRepository;

    @InjectMocks
    private LimitService limitService;

    private UUID clientId;
    private Limit limitEntity;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        clientId = UUID.randomUUID();

        // Инициализация LimitEntity с тестовыми данными
        limitEntity = new Limit();
        limitEntity.setId(UUID.randomUUID());
        limitEntity.setLimitSum(BigDecimal.valueOf(1000));
        limitEntity.setCurrency("USD");
        limitEntity.setDatetime(OffsetDateTime.now());
        limitEntity.setClientId(clientId);
        limitEntity.setBaseLimit(true);
        limitEntity.setActive(true);
    }

    @Test
    void shouldReturnLimitDTOWhenLimitExists() {
        // Настройка мока для возврата LimitEntity
        when(limitRepository.findTopByClientIdAndActiveTrueOrderByDatetimeDesc(clientId))
                .thenReturn(Optional.of(limitEntity));

        // Создание ожидаемого объекта DTO
        LimitDTO expectedLimitDTO = new LimitDTO(
                limitEntity.getId(),
                limitEntity.getLimitSum(),
                limitEntity.getCurrency(),
                limitEntity.getDatetime(),
                limitEntity.getClientId(),
                limitEntity.isBaseLimit(),
                limitEntity.isActive()
        );

        // Вызов
        CompletableFuture<Optional<LimitDTO>> resultFuture = limitService.getLatestLimitByClientIdAsync(clientId);


        Optional<LimitDTO> result = resultFuture.join();
        assertTrue(result.isPresent());

        // Сравнение полей
        assertEquals(expectedLimitDTO.getId(), result.get().getId());
        assertEquals(expectedLimitDTO.getLimitSum(), result.get().getLimitSum());
        assertEquals(expectedLimitDTO.getCurrency(), result.get().getCurrency());
        assertEquals(expectedLimitDTO.getDatetime(), result.get().getDatetime());
        assertEquals(expectedLimitDTO.getClientId(), result.get().getClientId());
        assertEquals(expectedLimitDTO.isBaseLimit(), result.get().isBaseLimit());
        assertEquals(expectedLimitDTO.isActive(), result.get().isActive());
    }

    @Test
    void shouldReturnEmptyWhenNoLimitExists() {
        // Настройка мока для возврата пустого Optional
        when(limitRepository.findTopByClientIdAndActiveTrueOrderByDatetimeDesc(clientId))
                .thenReturn(Optional.empty());

        // Вызов
        CompletableFuture<Optional<LimitDTO>> resultFuture = limitService.getLatestLimitByClientIdAsync(clientId);

        // Проверка
        Optional<LimitDTO> result = resultFuture.join();
        assertFalse(result.isPresent());
    }

    @Test
    void shouldHandleExceptionGracefully() {
        // Настройка мока для выброса исключения
        when(limitRepository.findTopByClientIdAndActiveTrueOrderByDatetimeDesc(clientId))
                .thenThrow(new RuntimeException("Database error"));

        // Вызов
        CompletableFuture<Optional<LimitDTO>> resultFuture = limitService.getLatestLimitByClientIdAsync(clientId);

        // Проверка
        assertTrue(resultFuture.isCompletedExceptionally());
        Exception exception = assertThrows(Exception.class, resultFuture::join);
        assertTrue(exception.getMessage().contains("Error retrieving the limit"));
    }
}
