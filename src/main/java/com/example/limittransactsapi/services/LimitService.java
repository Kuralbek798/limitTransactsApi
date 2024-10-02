package com.example.limittransactsapi.services;


import com.example.limittransactsapi.Helpers.mapper.LimitMapper;
import com.example.limittransactsapi.Models.DTO.LimitAccountDTO;
import com.example.limittransactsapi.Models.DTO.LimitDTO;
import com.example.limittransactsapi.Models.DTO.LimitDtoFromClient;
import com.example.limittransactsapi.Models.Entity.Limit;
import com.example.limittransactsapi.repository.LimitRepository;
import com.example.limittransactsapi.services.servicesHelpers.Converter;
import com.example.limittransactsapi.repository.projections.LimitAccountProjection;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class LimitService {

    // Currency pair definitions
    private static final String KZT = "KZT";
    private static final String RUB = "RUB";
    private static final String USD = "USD";
    private static final String KZT_USD_PAIR = "KZT/USD";
    private static final String RUB_USD_PAIR = "RUB/USD";

    private final LimitRepository limitRepository;
    private final ExchangeRateService exchangeRateService;
    private final Converter converter;
    private final Executor customExecutor;

    @Autowired
    public LimitService(LimitRepository limitRepository, ExchangeRateService exchangeRateService,
                        Converter converter, @Qualifier("customExecutor") Executor customExecutor) {
        this.limitRepository = limitRepository;
        this.exchangeRateService = exchangeRateService;
        this.converter = converter;
        this.customExecutor = customExecutor;
    }

    @Async("customExecutor")
    public CompletableFuture<ResponseEntity<String>> setLimitAsync(LimitDtoFromClient limitDtoFromClient) {

        // receiving checked and converted limit from client and try to get limit from db by clients' id.
        CompletableFuture<LimitDTO> futureConvertedClientsLimit = checkCurrencyTypeAndSetToUSDAsync(limitDtoFromClient);
        CompletableFuture<Optional<LimitDTO>> futureCurrentDBLimit = getLatestLimitByClientIdAsync(limitDtoFromClient.getClientId());

        return futureConvertedClientsLimit.thenCombine(futureCurrentDBLimit, (clientsLimit, optionalDBLimit) -> {
            // checking if limit with the same sum already exist.
            if (isLimitExist(clientsLimit, optionalDBLimit)) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body("Лимит с такой суммой уже установлен, пожалуйста выберите другой.");
            }
            // saving limit to db
            Limit savedLimit = limitRepository.save(LimitMapper.INSTANCE.toEntity(clientsLimit));
            log.info("Limit saved successfully: {}", savedLimit);
            // return ResponseEntity created
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body("Лимит успешно создан с ID: " + savedLimit.getId());
        }).exceptionally(ex -> {
            if (ex.getCause() instanceof IllegalArgumentException) {
                log.error("Ошибка бизнес-валидации: {}", ex.getMessage());
                return ResponseEntity.badRequest().body("Ошибка бизнес-валидации: " + ex.getMessage());
            } else {
                log.error("Неожиданная ошибка в методе setLimitAsync: {}", ex.getMessage());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Неожиданная ошибка сервера: " + ex.getMessage());
            }
        });
    }

    @Async("customExecutor")
    public CompletableFuture<LimitDTO> checkCurrencyTypeAndSetToUSDAsync(LimitDtoFromClient limitDtoFromClient) {
        // Check currency type for RUB
        if (limitDtoFromClient.getCurrency().equalsIgnoreCase(RUB)) {
            return exchangeRateService.getCurrencyRate(RUB_USD_PAIR)
                    .thenComposeAsync(exchangeRateDTO -> {
                        BigDecimal exchangeRate = exchangeRateDTO.getRate();
                        if (exchangeRate == null || exchangeRate.compareTo(BigDecimal.ZERO) <= 0) {
                            throw new IllegalArgumentException("Недопустимый курс для RUB.");
                        }
                        BigDecimal convertedSum = converter.currencyConverter(limitDtoFromClient.getLimitSum(), exchangeRate);
                        LimitDTO limiDTO = new LimitDTO(
                                null,
                                convertedSum
                                , "USD"
                                , null
                                , limitDtoFromClient.getClientId()
                                , false
                                , true
                        );
                        return CompletableFuture.completedFuture(limiDTO);
                    });

            // Check currency type for KZT
        } else if (limitDtoFromClient.getCurrency().equalsIgnoreCase(KZT)) {
            return exchangeRateService.getCurrencyRate(KZT_USD_PAIR)
                    .thenComposeAsync(exchangeRateDTO -> {
                        BigDecimal exchangeRate = exchangeRateDTO.getRate();
                        if (exchangeRate == null || exchangeRate.compareTo(BigDecimal.ZERO) <= 0) {
                            throw new IllegalArgumentException("Недопустимый курс для KZT.");
                        }
                        BigDecimal convertedSum = converter.currencyConverter(limitDtoFromClient.getLimitSum(), exchangeRate);
                        LimitDTO limiDTO = new LimitDTO(
                                null,
                                convertedSum
                                , "USD"
                                , null
                                , limitDtoFromClient.getClientId()
                                , false
                                , true
                        );
                        return CompletableFuture.completedFuture(limiDTO);
                    });
        } else if (limitDtoFromClient.getCurrency().equalsIgnoreCase(USD)) {
            return CompletableFuture.completedFuture(new LimitDTO(
                    null,
                    limitDtoFromClient.getLimitSum()
                    , limitDtoFromClient.getCurrency()
                    , null
                    , limitDtoFromClient.getClientId()
                    , false
                    , true)
            );
        }
        {
            throw new IllegalArgumentException("Недопустимая валюта конвертации.");
        }
    }

    @Async("customExecutor")
    public CompletableFuture<Optional<LimitDTO>> getLatestLimitByClientIdAsync(UUID id) {
        try {
            // Fetch the latest active limit for the given client ID from the repository
            var limitEntity = limitRepository.findTopByClientIdAndActiveTrueOrderByDatetimeDesc(id);
            log.info("before Mapped limitEntity: {}", limitEntity);

            // Map the entity to a DTO using the mapper
            var limitDTO = limitEntity.map(limit -> LimitMapper.INSTANCE.toDTO(limit));

            log.info("after Mapped LimitDTO: {}", limitDTO);
            // Return the DTO wrapped in a completed CompletableFuture
            return CompletableFuture.completedFuture(limitDTO);

        } catch (Exception ex) {
            log.error("Error retrieving the latest limit: {}", ex.getMessage(), ex);
            // Return a failed CompletableFuture with the exception
            return CompletableFuture.failedFuture(new Exception("Error retrieving the limit", ex));
        }
    }

    public List<LimitDTO> getAllLimits() {
        log.info("getAllLimits");

        return  LimitMapper.INSTANCE.toDTO(limitRepository.findAll());

    }

    //synchrony private method
    private boolean isLimitExist(LimitDTO limitDtoFromClient, Optional<LimitDTO> limitDtoFromDb) {
        return limitDtoFromDb.isPresent() && limitDtoFromDb.get().getLimitSum().equals(limitDtoFromClient.getLimitSum());
    }

    public CompletableFuture<ConcurrentLinkedQueue<LimitAccountDTO>> getAllActiveLimitsByAccounts(Integer[] accountNumbers) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("getAllActiveLimits: {}___{}", accountNumbers.length, accountNumbers.toString());
                ConcurrentLinkedQueue<LimitAccountProjection> projections = limitRepository.findLatestActiveLimits(accountNumbers);
                log.info("projections: {}, account numbers: {}", projections.size(), projections.stream()
                        .map(LimitAccountProjection::getAccountNumber)
                        .collect(Collectors.toList()));

                return projections.stream()
                        .map(pr -> new LimitAccountDTO(
                                pr.getId(),
                                pr.getLimitSum(),
                                pr.getCurrency(),
                                pr.getDatetime().toInstant().atOffset(ZoneOffset.UTC),
                                pr.getClientId(),
                                pr.getIsBaseLimit(),
                                pr.getIsActive(),
                                pr.getAccountNumber()
                        ))
                        .collect(Collectors.toCollection(() -> new ConcurrentLinkedQueue<LimitAccountDTO>()));
            } catch (Exception e) {
                log.error("Failed to retrieve active limits: ", e);
                throw new RuntimeException("Unable to fetch active limits.");
            }
        }, customExecutor).exceptionally(ex -> {
            log.error("Error occurred during async processing: ", ex);
            return new ConcurrentLinkedQueue<>();
        });
    }
}
