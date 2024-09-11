package com.example.limittransactsapi.util;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;

public class BuildUrlAndDateUtil {

    public static OffsetDateTime getStartOfWeek(OffsetDateTime date) {
        // Получаем начало недели (понедельник) на основании текущей даты
        return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    public static String buildUrl(String url, String symbol, LocalDate startDate, LocalDate endDate, String apiKey) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        return String.format(url, symbol, startDate.format(formatter), endDate.format(formatter), apiKey);
    }

}
