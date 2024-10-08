package com.example.limittransactsapi.services;


import com.example.limittransactsapi.services.helpersServices.HttpClientService;
import com.example.limittransactsapi.models.DTO.ExchangeRateDTO;
import com.example.limittransactsapi.models.RateDataFromJson;
import com.example.limittransactsapi.helpers.mapper.ExchangeRateMapper;
import com.example.limittransactsapi.repository.ExchangeRateRepository;
import com.example.limittransactsapi.util.BuildUrlAndDate;
import com.example.limittransactsapi.services.helpersServices.Converter;
import com.example.limittransactsapi.services.helpersServices.PathForApiServis;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.service.spi.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Slf4j
@Service
public class ExchangeRateService {
    // ObjectMapper for JSON processing
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Injecting values from the application properties
    @Value("${app.servisIdentity}")
    private String servisIdentity;

    @Value("${app.uuidKey}")
    private String uuidKey;

    @Value("${app.twelveURL}")
    private String baseUrl;

    private final PathForApiServis pathForApiServis;
    private final ExchangeRateRepository exchangeRateRepository;
    private final HttpClientService httpClientService;
    private final Converter converter;
    private final Executor customExecutor;

    // Currency pair definitions
    private static final String KZT_USD_PAIR = "KZT/USD";
    private static final String USD_KZT_PAIR = "USD/KZT";
    private static final String VALUES = "values";
    private static final String CLOSE = "close";
    private static final String DATETIME = "datetime";

    // Constructor for dependency injection
    @Autowired
    public ExchangeRateService(PathForApiServis pathForApiServis, ExchangeRateRepository exchangeRateRepository,
                               HttpClientService httpClientService, Converter converter, @Qualifier("customExecutor") Executor customExecutor) {
        this.pathForApiServis = pathForApiServis;
        this.exchangeRateRepository = exchangeRateRepository;
        this.httpClientService = httpClientService;
        this.converter = converter;
        this.customExecutor = customExecutor;
    }

    @Async("customExecutor")
    public CompletableFuture<ExchangeRateDTO> getCurrencyRate(String currencyPair) {
        String effectiveCurrencyPair = getEffectiveCurrencyPair(currencyPair);
        LocalDate dateNow = LocalDate.now();

        return fetchRateFromDatabase(effectiveCurrencyPair, dateNow)
                .thenCompose(rateDto -> {
                    if (rateDto != null) {
                        return CompletableFuture.completedFuture(rateDto);
                    } else {
                        return getRateFromApi(effectiveCurrencyPair, dateNow);
                    }
                })
                .exceptionally(ex -> {
                    log.error("Error occurred in getCurrencyRate method: {}", currencyPair, ex);
                    throw new ServiceException("Error occurred in getCurrencyRate method", ex);
                });
    }

    @Async("customExecutor")
    CompletableFuture<ExchangeRateDTO> fetchRateFromDatabase(String effectiveCurrencyPair, LocalDate dateNow) {

        try {
            return CompletableFuture.completedFuture(exchangeRateRepository.findTopByCurrencyPairOrderByDateTimeRateDesc(effectiveCurrencyPair)
                    .filter(rate -> LocalDate.ofInstant(rate.getDateTimeRate().toInstant(), ZoneId.systemDefault()).equals(dateNow))
                    .map(rate -> {
                        log.info("Exchange rate found in database for currency pair: {}", effectiveCurrencyPair);
                        return ExchangeRateMapper.INSTANCE.toDTO(rate);
                    })
                    .orElse(null));
        } catch (Exception e) {
            log.error("Error occurred while fetching the exchange rate from the database: {}", effectiveCurrencyPair, e);
            throw new ServiceException("Error occurred while fetching the exchange rate from the database", e);
        }

    }

    @Async("customExecutor")
    public CompletableFuture<ExchangeRateDTO> getRateFromApi(String effectiveCurrencyPair, LocalDate dateNow) {

        try {
            String apiKey = pathForApiServis.getDecryptedApiKey(servisIdentity, uuidKey);

            return getResponseFromHttpRequest(apiKey, effectiveCurrencyPair, dateNow)
                    .thenApply(jsonRoot -> fetchAndSaveExchangeRate(jsonRoot, effectiveCurrencyPair))
                    .thenApply(exchangeRateDTO -> {
                        log.info("Exchange rate successfully fetched and saved from API for currency pair: {}", effectiveCurrencyPair);
                        return exchangeRateDTO;
                    })
                    .exceptionally(ex -> {
                        log.error("Error occurred while fetching the exchange rate from API for currency pair: {}", effectiveCurrencyPair, ex);
                        throw new ServiceException("Error occurred while fetching the exchange rate from API", ex);
                    });
        } catch (Exception e) {
            log.error("Error occurred while decrypting the API key: {}", effectiveCurrencyPair, e);
            throw new ServiceException("Error occurred while decrypting the API key", e);
        }
    }

    @Async("customExecutor")
    public CompletableFuture<JsonNode> getResponseFromHttpRequest(String apiKey, String currencyPair, LocalDate dateNow) {

            try {
                LocalDate startDate = dateNow;
                LocalDate endDate = startDate.plusDays(1);
                String url = BuildUrlAndDate.buildUrl(baseUrl, currencyPair, startDate, endDate, apiKey);
                JsonNode root = fetchJsonResponse(url);
                if (isValidResponse(root)){
                    return CompletableFuture.completedFuture(root);
                }
                return CompletableFuture.completedFuture(tryGetJsonResponse(apiKey, currencyPair, startDate, endDate));
            } catch (JsonProcessingException e) {
                log.error("Error processing JSON response: {}", e.getMessage());
                throw new ServiceException("Failed to fetch and save exchange rate", e);
            } catch (Exception e) {
                log.error("Unexpected error: {}", e.getMessage());
                throw new ServiceException("Failed to fetch and save exchange rate", e);
            }
    }

    // Fetches the exchange rate data from the JSON response and saves it
    public ExchangeRateDTO fetchAndSaveExchangeRate(JsonNode jsonRoot, String currencyPair) {
        try {
            RateDataFromJson rateDataFromJson = extractRateFromJsonRoot(jsonRoot);
            currencyPair = getEffectiveCurrencyPair(currencyPair);
            if (rateDataFromJson != null && rateDataFromJson.getCloseRate() != null) {
                ExchangeRateDTO exchangeRateDTO =
                        new ExchangeRateDTO(
                                currencyPair,
                                rateDataFromJson.getCloseRate(),
                                rateDataFromJson.getCloseRate(),
                                rateDataFromJson.getDateTime()
                        );
                if (currencyPair.equals(USD_KZT_PAIR)) {
                    new ExchangeRateDTO(
                            exchangeRateDTO.getCurrencyPair(),
                            converter.convertUsdToKztToKztToUsd(exchangeRateDTO.getRate()),
                            converter.convertUsdToKztToKztToUsd(exchangeRateDTO.getRate()),
                            exchangeRateDTO.getDateTimeRate()
                    );
                }
                log.info("Fetched exchange rate from API for currency pair: {}", currencyPair);
                var exchageRate = exchangeRateRepository.save(ExchangeRateMapper.INSTANCE.toEntity(exchangeRateDTO));
                return ExchangeRateMapper.INSTANCE.toDTO(exchageRate);
            }
            log.warn("No exchange rate found for currency pair: {}", currencyPair);
            return null;
        } catch (Exception e) {
            log.error("Unexpected error in the method fetchAndSaveExchangeRate : {}", e.getMessage());
            throw e;
        }
    }

    // Extracts the rate data from the JSON root node
    private RateDataFromJson extractRateFromJsonRoot(JsonNode jsonRoot) {
        RateDataFromJson rateDataFromJson = new RateDataFromJson();
        JsonNode values = jsonRoot.path(VALUES);
        try {
            // Process the array of values
            if (values.isArray() && values.size() > 0) {
                for (JsonNode latestData : values) {
                    String strClose = latestData.get(CLOSE).asText();
                    BigDecimal close = new BigDecimal(strClose);
                    String dateStr = latestData.get(DATETIME).asText();
                    // Only use entries with non-zero close values
                    if (close.compareTo(BigDecimal.ZERO) > 0) {
                        rateDataFromJson.setCloseRate(close);
                        String formattedISODateTimeStr = dateStr.replace(" ", "T") + "Z";
                        rateDataFromJson.setDateTime(OffsetDateTime.parse(formattedISODateTimeStr));

                        return rateDataFromJson;
                    }
                }
            }
        } catch (IllegalArgumentException | DateTimeParseException e) {
            log.error("Error extracting rate data from JSON: {}", e.getMessage());
            return null;
        } catch (NullPointerException e) {
            log.error("Encountered null value while processing JSON: {}", e.getMessage());
            return null;
        }
        log.error("All close values are zero.");
        return null;
    }

    private String getEffectiveCurrencyPair(String currencyPair) {
        return KZT_USD_PAIR.equals(currencyPair) ? USD_KZT_PAIR : currencyPair;
    }

    private JsonNode fetchJsonResponse(String url) throws JsonProcessingException {
        CompletableFuture<String> jsonResponse = httpClientService.sendRequest(url);
        return objectMapper.readTree(jsonResponse.join());
    }

    private boolean isValidResponse(JsonNode jsonResponse) {
        return jsonResponse.path(VALUES).isArray() && jsonResponse.path(VALUES).size() > 0;
    }

    private JsonNode tryGetJsonResponse(String apiKey, String currencyPair, LocalDate startDate, LocalDate endDate) throws JsonProcessingException {
        for (int index = 1; index <= 5; index++) {
            LocalDate currentStartDate = startDate.minusDays(index);
            String url = BuildUrlAndDate.buildUrl(baseUrl, currencyPair, currentStartDate, endDate, apiKey);
            JsonNode root = fetchJsonResponse(url);
            if (isValidResponse(root)) return root;
        }
        throw new ServiceException("Failed to fetch valid exchange rate data in retry attempts");
    }

}