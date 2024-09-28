package com.example.limittransactsapi.Helpers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.RoundingMode;


@Slf4j
@Service
public class Converter {


    public BigDecimal currencyConverter(BigDecimal amount, BigDecimal currencyRate ) {
        if (amount == null || currencyRate == null) {
            throw new IllegalArgumentException("Amount and exchange rate must not be null");
        }
        return amount.multiply(currencyRate);
    }

    public  BigDecimal convertUsdToKztToKztToUsd(BigDecimal usdToKzt) {
        if (usdToKzt.compareTo(BigDecimal.ZERO) == 0) {
            throw new IllegalArgumentException("Курс доллара к тенге не может быть равен нулю.");
        }
        // Возвращение обратного значения для вычисления курса KZT к USD
        return BigDecimal.ONE.divide(usdToKzt, 6, RoundingMode.HALF_UP); // 6 — число знаков после запятой для большей точности
    }



}
