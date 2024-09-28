package com.example.limittransactsapi.services;


import com.example.limittransactsapi.Helpers.mapper.LimitMapper;
import com.example.limittransactsapi.Models.DTO.LimitDTO;

import com.example.limittransactsapi.Models.Entity.Limit;

import com.example.limittransactsapi.repository.LimitRepository;
import com.example.limittransactsapi.Helpers.Converter;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

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
  /*  @Async("customExecutor")
    public CompletableFuture<ResponseEntity<LimitDTO>> setLimitAsync(LimitDTO limitDtoFromClient) {

        var futureConvertedClientsLimit = checkCurrencyTypeAndSetToUSDAsync(limitDtoFromClient);
        var futureCurrentDBLimit = getLatestLimitAsync(LimitDTO limitDtoFromClient);

        return futureConvertedClientsLimit.thenCombine(futureCurrentDBLimit, (clientsLimit, optionalDBLimit) -> {
            if (isLimitExist(clientsLimit, optionalDBLimit)) {
                throw new IllegalArgumentException("Лимит с той же суммой уже установлен; выберите другой.");
            }
            return clientsLimit;
        }).thenApply(validatedLimit -> {
            Limit savedLimit = limitRepository.save(ClientLimitMapper.INSTANCE.toEntity(validatedLimit));
            log.info("Limit saved successfully: {}", savedLimit);
            return ResponseEntity.status(HttpStatus.CREATED).body(ClientLimitMapper.INSTANCE.toDTO(savedLimit));
        }).exceptionally(ex -> {
            if (ex.getCause() instanceof IllegalArgumentException) {
                log.error("Ошибка бизнес-валидации: {}", ex.getMessage());
                return ResponseEntity.badRequest().body(null);
            } else {
                log.error("Неожиданная ошибка в методе setLimitAsync: {}", ex.getMessage());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
            }
        });
    }*/

    /*@Async("customExecutor")
    public CompletableFuture<LimitDTO> checkCurrencyTypeAndSetToUSDAsync(LimitDTO limitDtoFromClient) {
        // Check currency type for RUB
        if (limitDtoFromClient.getCurrency().equals(RUB)) {
            return exchangeRateService.getCurrencyRate(RUB_USD_PAIR)
                    .thenComposeAsync(exchangeRateDTO -> {
                        BigDecimal exchangeRate = exchangeRateDTO.getRate();
                        if (exchangeRate == null || exchangeRate.compareTo(BigDecimal.ZERO) <= 0) {
                            throw new IllegalArgumentException("Недопустимый курс для RUB.");
                        }
                        BigDecimal convertedCurrency = converterUtil.currencyConverter(limitDtoFromClient.getLimitSum(), exchangeRate);
                        LimitDTO limiDTO = new LimitDTO(
                                null,
                                convertedCurrency
                                , "USD"
                                ,null
                                ,limitDtoFromClient.getClientId()
                                ,false
                        );
                        return CompletableFuture.completedFuture(limiDTO);
                    });

            // Check currency type for KZT
        } else if (limitDtoFromClient.getCurrency().equals(KZT)) {
            return exchangeRateService.getCurrencyRate(KZT_USD_PAIR)
                    .thenComposeAsync(exchangeRateDTO -> {
                        BigDecimal exchangeRate = exchangeRateDTO.getRate();
                        if (exchangeRate == null || exchangeRate.compareTo(BigDecimal.ZERO) <= 0) {
                            throw new IllegalArgumentException("Недопустимый курс для KZT.");
                        }
                        BigDecimal convertedCurrency = converterUtil.currencyConverter(limitDtoFromClient.getLimitSum(), exchangeRate);
                        LimitDTO limiDTO = new LimitDTO(
                                null,
                                convertedCurrency
                                , "USD"
                                ,null
                                ,limitDtoFromClient.getClientId()
                                ,false
                        );
                        return CompletableFuture.completedFuture(limiDTO);
                    });
        } else {
            throw new IllegalArgumentException("Недопустимая валюта конвертации.");
        }
    }

    @Async("customExecutor")
    public CompletableFuture<LimitDTO> getLatestLimitAsync(LimitDTO  limitDtoFromClient) {
        return CompletableFuture
                .supplyAsync(() -> {
                    // Fetch the latest limit from the repository
                    var limitEntity = limitRepository.findTopByOrderByDatetimeDesc();
                    log.info("before Mapped limitEntity: {}", limitEntity);

                    var limitDTO = limitEntity.filter(limit -> limit.getLimitSum() != null) // Check that limitSum is not null
                            .map(limit -> LimitMapper.INSTANCE.toDTO(limit))
                            .orElseThrow(() -> new IllegalArgumentException("Нет существующего лимита для проверки.")); // Throw exception if no value is found
                    log.info("arfter Mapped LimitDTO: {}", limitDTO);
                    return limitDTO;
                })
                .exceptionally(ex -> {
                    log.error("Ошибка при получении последнего лимита: {} причина {}", ex.getMessage(), ex.getCause());
                    throw new CompletionException(new Exception("Ошибка при получении лимита", ex));
                });
    }

    public LimitDTO findLimit() {

        var a = limitRepository.findTopByOrderByDatetimeDesc();
        log.info("Mapped LimitDTO: {}", a);
        var n = a.map(limit -> LimitMapper.INSTANCE.toDTO(limit)).orElse(null);
        log.info("Mapped LimitDTO: {}", n);

        return n;
    }



    //synchrony private method
    private boolean isLimitExist(LimitDtoFromClient limitDtoFromClient, LimitDTO limitDto) {
        return limitDto.getLimitSum().equals(limitDtoFromClient.getLimitSum());
    }*/
    @Transactional
    public boolean setMonthlyLimitByDefault() {
        try {
            Limit limit = new Limit();
            limit.setLimitSum(BigDecimal.valueOf(1000));
            limit.setCurrency(USD);
            limit.setBaseLimit(true);
            limit.setActive(true);

            Optional<Limit> savedLimit = limitRepository.saveWithOptional(limit);
            if (savedLimit.isPresent() && savedLimit.get().getId() != null && savedLimit.get().getLimitSum().equals(BigDecimal.valueOf(1000))) {
                log.info("Default limit saved successfully: {}", savedLimit);
                return true;
            } else {
                log.warn("Limit was not saved: {}", limit);
                return false;
            }
        } catch (Exception e) {
            log.error("Unexpected error in method insertMonthlyLimit: {}", e.getMessage(), e);
            return false;
        }
    }

    public LimitDTO getLimitById(UUID id) {
        return limitRepository.findTopByClientIdAndIsActiveTrueOrderByDatetimeDesc(id)
                .map(limit -> LimitMapper.INSTANCE.toDTO(limit))
                .orElse(null);
    }


    @Transactional
    public void updateStatusIsActive() {
        limitRepository.updateStatusIsActive();
    }

    public CompletableFuture<ResponseEntity<LimitDTO>> setLimitAsync(@Valid LimitDTO limit) {
        return null;
    }
}
