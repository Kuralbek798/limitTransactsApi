package com.example.limittransactsapi.services;


import com.example.limittransactsapi.DTO.LimitDTO;
import com.example.limittransactsapi.Entity.Limit;
import com.example.limittransactsapi.mapper.LimitMapper;
import com.example.limittransactsapi.repository.LimitRepository;
import com.example.limittransactsapi.util.ConverterUtil;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;

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
    private final CurrencyService currencyService;
    private final ConverterUtil converterUtil;

    @Autowired
    public LimitService(LimitRepository limitRepository, CurrencyService currencyService, ConverterUtil converterUtil) {
        this.limitRepository = limitRepository;
        this.currencyService = currencyService;
        this.converterUtil = converterUtil;
    }

    // checking and setting limits method
    public ResponseEntity<LimitDTO> setLimit(LimitDTO limitDtoFromClient) {
        try {
              Optional<LimitDTO>OptionalLimitDtoFromDB = getLatestLimitInOptionalLimitDto();

            if (isLimitExist(OptionalLimitDtoFromDB, limitDtoFromClient)) {
                throw new IllegalArgumentException("A limit with the same sum has already been set; please choose another one.");
            } else {
                // проверяем в какой валюте был задан лимит, если не USD, то конвертируем по курсу и заменяем данные полученные от клиента.
                var convertedLimit = checkLimitCurrencyAndSetToUSD(limitDtoFromClient);
                limitDtoFromClient.setLimitCurrency(convertedLimit.getLimitCurrency());
                limitDtoFromClient.setLimitSum(convertedLimit.getLimitSum());
            }

            Limit savedLimit = limitRepository.save(LimitMapper.INSTANCE.toEntity(limitDtoFromClient));
            log.info("Limit saved successfully: {}", limitDtoFromClient);

            return new ResponseEntity<>(LimitMapper.INSTANCE.toDTO(savedLimit), HttpStatus.CREATED);

        } catch (IllegalArgumentException | IllegalStateException e) {
            log.error("Error in method setLimit: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        } catch (Exception e) {
            log.error("Unexpected error in method setLimit: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    public Optional<LimitDTO> getLatestLimitInOptionalLimitDto() {
        Optional<Limit> existingLimit = limitRepository.findTopByOrderByLimitDatetimeDesc();
        return  existingLimit.map(limit -> LimitMapper.INSTANCE.toDTO(limit));
    }

    private boolean isLimitExist(Optional<LimitDTO> OptionalLimitDto, LimitDTO limitDTO) {
        return OptionalLimitDto.isPresent() && OptionalLimitDto.get().getLimitSum().equals(limitDTO.getLimitSum());
    }

    private LimitDTO checkLimitCurrencyAndSetToUSD(LimitDTO limitDto) {
        BigDecimal exchangeRate;

        if (limitDto.getLimitCurrency().equals(RUB)) {
            var exchangeRateDTO = currencyService.getCurrencyRate(RUB_USD_PAIR);
            exchangeRate = exchangeRateDTO.getRate();
            if (exchangeRate == null || exchangeRate.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Invalid exchange rate for RUB.");
            }
            BigDecimal bigDecimal = converterUtil.currencyConverter(limitDto.getLimitSum(), exchangeRate);
            limitDto.setLimitSum(bigDecimal);
            limitDto.setLimitCurrency(USD);

        } else if (limitDto.getLimitCurrency().equals(KZT)) {
            var exchangeRateDTO = currencyService.getCurrencyRate(KZT_USD_PAIR);
            exchangeRate = exchangeRateDTO.getRate();
            if (exchangeRate == null || exchangeRate.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Invalid exchange rate for KZT.");
            }
            BigDecimal bigDecimal = converterUtil.convertCurrencyForUSDByKZTRate(limitDto.getLimitSum(), exchangeRate);
            limitDto.setLimitSum(bigDecimal);
            limitDto.setLimitCurrency(USD);
        }
        return limitDto;
    }

    public boolean insertMonthlyLimit() {
        try {
            Limit limit = new Limit();
            limit.setLimitCurrency(USD);
            limit.setLimitSum(BigDecimal.valueOf(1000));

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

}
