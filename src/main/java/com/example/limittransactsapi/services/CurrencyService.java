package com.example.limittransactsapi.services;

// Import statements
import com.example.limittransactsapi.DTO.ExchangeRateDTO;
import com.example.limittransactsapi.Entity.ExchangeRate;
import com.example.limittransactsapi.Model.RateDataFromJson;
import com.example.limittransactsapi.mapper.ExchangeRateMapper;
import com.example.limittransactsapi.repository.ExchangeRateRepository;
import com.example.limittransactsapi.util.BuildUrlAndDateUtil;
import com.example.limittransactsapi.util.HttpClientServiceUtil;
import com.example.limittransactsapi.util.PathForApiServisUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.service.spi.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
public class CurrencyService {
    // ObjectMapper for JSON processing
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Injecting values from the application properties
    @Value("${app.servisIdentity}")
    private String servisIdentity;

    @Value("${app.uuidKey}")
    private String uuidKey;

    @Value("${app.twelveURL}")
    private String baseUrl;

    private final PathForApiServisUtil pathForApiServisUtil;
    private final ExchangeRateRepository exchangeRateRepository;
    private final HttpClientServiceUtil httpClientServiceUtil;

    // Currency pair definitions
    private static final String KZT_USD_PAIR = "KZT/USD";
    private static final String USD_KZT_PAIR = "USD/KZT";
    private static final String VALUES = "values";
    private static final String CLOSE = "close";
    private static final String DATETIME = "datetime";

    // Constructor for dependency injection
    @Autowired
    public CurrencyService(PathForApiServisUtil pathForApiServisUtil, ExchangeRateRepository exchangeRateRepository,
                           HttpClientServiceUtil httpClientServiceUtil) {
        this.pathForApiServisUtil = pathForApiServisUtil;
        this.exchangeRateRepository = exchangeRateRepository;
        this.httpClientServiceUtil = httpClientServiceUtil;
    }

    // Method to get the currency rate
    public ExchangeRateDTO getCurrencyRate(String currencyPair) {
        try {
            // Translate KZT/USD to USD/KZT
            if (KZT_USD_PAIR.equals(currencyPair)) {
                currencyPair = USD_KZT_PAIR;
            }
            LocalDate dateNow = LocalDate.now(); // Get the current date

            // Fetch the latest rate from the database
            Optional<ExchangeRate> optionalRate = exchangeRateRepository.findTopByCurrencyPairOrderByDateDesc(currencyPair);

            // If the rate exists and is up to date, return it
            if (optionalRate.isPresent() &&
                    LocalDate.ofInstant(optionalRate.get().getDatetime().toInstant(), ZoneId.systemDefault()).equals(dateNow)) {
                log.info("Exchange rate found in database for currency pair: {}", currencyPair);

                return ExchangeRateMapper.INSTANCE.toDTO(optionalRate.get());

            } else {
                log.warn("Exchange rate not found for currency pair: {}", currencyPair);
            }

            // Fetch new data from the external API
            String apiKey = pathForApiServisUtil.getDecryptedApiKey(servisIdentity, uuidKey);
            var jsonRoot = getResponseFromHttpRequest(apiKey, currencyPair, dateNow);
            var exchangeRate = fetchAndSaveExchangeRate(jsonRoot, currencyPair);

            return ExchangeRateMapper.INSTANCE.toDTO(exchangeRate);

        } catch (Exception e) {
            // Log and wrap any exceptions into a ServiceException
            log.error("Error occurred while fetching the exchange rate for currency pair: {}", currencyPair, e);
            throw new ServiceException("An error occurred in method getCurrencyRate: " + e);
        }
    }

    // Helper method to perform the HTTP request and retrieve the JSON response
    private JsonNode getResponseFromHttpRequest(String apiKey, String currencyPair, LocalDate dateNow) {
        try {
            LocalDate startDate = dateNow;
            LocalDate endDate = startDate.plusDays(1);
            String url = BuildUrlAndDateUtil.buildUrl(baseUrl, currencyPair, startDate, endDate, apiKey);

            String jsonResponse = String.valueOf(httpClientServiceUtil.sendRequest(url));

            // Parse and check the JSON response
            JsonNode root = objectMapper.readTree(jsonResponse);

            if (root.path(VALUES).isArray() && root.path(VALUES).size() > 0) {
                return root;
            }

            // Handle case where no data is available for the specified dates
            if ("400".equals(root.path("code").toString()) &&
                    root.path("message").toString().contains("No data is available on the specified dates") &&
                    root.path("status").toString().contains("error")) {

                for (int i = 0; i < 5; i++) {
                    startDate = startDate.plusDays(1);
                    url = BuildUrlAndDateUtil.buildUrl(baseUrl, currencyPair, startDate, endDate, apiKey);
                    jsonResponse = String.valueOf(httpClientServiceUtil.sendRequest(url));

                    if (jsonResponse != null) {
                        root = objectMapper.readTree(jsonResponse);

                        if (root.path(VALUES).isArray() && root.path(VALUES).size() > 0) {
                            return root;
                        }
                    }
                }
            }

            return null;
        } catch (JsonProcessingException e) {
            log.error("Error while processing JSON response: {}", e.getMessage());
            throw new ServiceException("Failed to fetch and save exchange rate", e);
        } catch (Exception e) {
            log.error("Unexpected error: {}", e.getMessage());
            throw new ServiceException("Failed to fetch and save exchange rate", e);
        }
    }

    // Fetches the exchange rate data from the JSON response and saves it
    private ExchangeRate fetchAndSaveExchangeRate(JsonNode jsonRoot, String currencyPair) {
        try {
            RateDataFromJson rateDataFromJson = extractRateFromJsonRoot(jsonRoot);

            if (rateDataFromJson != null && rateDataFromJson.getCloseRate() != null) {
                ExchangeRate exchangeRate = new ExchangeRate(UUID.randomUUID(), currencyPair,
                        BigDecimal.valueOf(rateDataFromJson.getCloseRate()), rateDataFromJson.getCloseRate(), rateDataFromJson.getDate());
                log.info("Fetched exchange rate from API for currency pair: {}", currencyPair);
                return exchangeRateRepository.save(exchangeRate);
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
                    double close = latestData.path(CLOSE).asDouble();
                    String dateStr = latestData.path(DATETIME).asText();

                    // Only use entries with non-zero close values
                    if (close != 0) {
                        rateDataFromJson.setCloseRate(close);
                        String formattedISODateTimeStr = dateStr.replace(" ", "T");
                        rateDataFromJson.setDate(OffsetDateTime.parse(formattedISODateTimeStr));

                        return rateDataFromJson;
                    }
                }
            }
        } catch (IllegalArgumentException | DateTimeParseException e) {
            log.warn("Error extracting rate data from JSON: {}", e.getMessage());
            return null;
        } catch (NullPointerException e) {
            log.warn("Encountered null value while processing JSON: {}", e.getMessage());
            return null;
        }

        log.warn("All close values are zero.");
        return null;
    }
}