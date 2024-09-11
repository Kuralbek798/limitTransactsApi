package com.example.limittransactsapi.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;


@Slf4j
@Service
public class ConverterUtil {


    public BigDecimal currencyConverter(BigDecimal amount, BigDecimal currencyRate ) {
        if (amount == null || currencyRate == null) {
            throw new IllegalArgumentException("Amount and exchange rate must not be null");
        }
        return amount.multiply(currencyRate);
    }

    // Временное решение просто пока не успеваю апи искать, а покупать подписку пока не получается.
    public  BigDecimal convertCurrencyForUSDByKZTRate(BigDecimal amount, BigDecimal exchangeRate) {
        if (amount == null || exchangeRate == null || exchangeRate.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount and exchange rate must not be null or zero.");
        }
        return amount.divide(exchangeRate, 2, RoundingMode.HALF_UP);
    }
}
