package com.example.limittransactsapi.services;


import com.example.limittransactsapi.DTO.LimitDTO;
import com.example.limittransactsapi.DTO.LimitDtoFromClient;
import com.example.limittransactsapi.Entity.Limit;
import com.example.limittransactsapi.mapper.ClientLimitMapper;
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
    private final ExchangeRateService exchangeRateService;
    private final ConverterUtil converterUtil;

    @Autowired
    public LimitService(LimitRepository limitRepository, ExchangeRateService exchangeRateService, ConverterUtil converterUtil) {
        this.limitRepository = limitRepository;
        this.exchangeRateService = exchangeRateService;
        this.converterUtil = converterUtil;
    }

    // checking and setting limits method
    public ResponseEntity<LimitDtoFromClient> setLimit(LimitDtoFromClient limitDtoFromClient) {
        try {
            //receiving latest limit from db
            Optional<LimitDTO> OptionalLimitDtoFromDB = getLatestLimitInOptionalLimitDto();

            //checking and converting limit from client
            var convertedLimit = checkLimitCurrencyTypeAndSetToUSD(limitDtoFromClient);
            limitDtoFromClient.setLimitCurrency(convertedLimit.getLimitCurrency());
            limitDtoFromClient.setLimitSum(convertedLimit.getLimitSum());

            //comparing existing limit with client's limit
            if (isLimitExist(OptionalLimitDtoFromDB, limitDtoFromClient)) {
                throw new IllegalArgumentException("A limit with the same sum has already been set; please choose another one.");
            }

            Limit savedLimit = limitRepository.save(ClientLimitMapper.INSTANCE.toEntity(limitDtoFromClient));
            log.info("Limit saved successfully: {}", limitDtoFromClient);

            return new ResponseEntity<>(ClientLimitMapper.INSTANCE.toDTO(savedLimit), HttpStatus.CREATED);

        } catch (IllegalArgumentException | IllegalStateException e) {
            log.error("Error in method setLimit: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        } catch (Exception e) {
            log.error("Unexpected error in method setLimit: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    public Optional<LimitDTO> getLatestLimitInOptionalLimitDto() {
        Optional<Limit> existingLimit = limitRepository.findTopByOrderByDatetimeDesc();
        return existingLimit.map(limit -> LimitMapper.INSTANCE.toDTO(limit));
    }

    private boolean isLimitExist(Optional<LimitDTO> OptionalLimitDto, LimitDtoFromClient limitDtoFromClient) {
        return OptionalLimitDto.isPresent() && OptionalLimitDto.get().getLimitAmount().equals(limitDtoFromClient.getLimitSum());
    }

    private LimitDtoFromClient checkLimitCurrencyTypeAndSetToUSD(LimitDtoFromClient limitDtoFromClient) {
        BigDecimal exchangeRate;
        // checking currency type if it is RUB
        if (limitDtoFromClient.getLimitCurrency().equals(RUB)) {
            //receiving rate
            var exchangeRateDTO = exchangeRateService.getCurrencyRate(RUB_USD_PAIR);
            exchangeRate = exchangeRateDTO.getRate();
            if (exchangeRate == null || exchangeRate.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Invalid exchange rate for RUB.");
            }
            //converting limit sum by rate
            BigDecimal bigDecimal = converterUtil.currencyConverter(limitDtoFromClient.getLimitSum(), exchangeRate);
            limitDtoFromClient.setLimitSum(bigDecimal);
            limitDtoFromClient.setLimitCurrency(USD);
            //checking currency type if it is KZT
        } else if (limitDtoFromClient.getLimitCurrency().equals(KZT)) {
            //receiving currency rate
            var exchangeRateDTO = exchangeRateService.getCurrencyRate(KZT_USD_PAIR);
            exchangeRate = exchangeRateDTO.getRate();
            if (exchangeRate == null || exchangeRate.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Invalid exchange rate for KZT.");
            }
            //converting limit sum by rate
            var bigDecimal = converterUtil.currencyConverter(limitDtoFromClient.getLimitSum(), exchangeRate);
            limitDtoFromClient.setLimitSum(bigDecimal);
            limitDtoFromClient.setLimitCurrency(USD);
        }
        return limitDtoFromClient;
    }

    public boolean setMonthlyLimitByDefault() {
        try {
            Limit limit = new Limit();
            limit.setCurrency(USD);
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
