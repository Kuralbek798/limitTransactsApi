package com.example.limittransactsapi.services;

import com.example.limittransactsapi.DTO.CheckedOnLimitDTO;
import com.example.limittransactsapi.DTO.ExchangeRateDTO;
import com.example.limittransactsapi.DTO.LimitDTO;
import com.example.limittransactsapi.DTO.TransactionDTO;
import com.example.limittransactsapi.Entity.CheckedOnLimit;

import com.example.limittransactsapi.mapper.TransactionMapper;
import com.example.limittransactsapi.repository.TransactionProjection;
import com.example.limittransactsapi.repository.TransactionRepository;
import com.example.limittransactsapi.services.crud.TransactionCRUDService;
import com.example.limittransactsapi.util.ConverterUtil;


import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;


import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class TransactionService {

    private static final String KZT_USD_PAIR = "KZT/USD";
    private static final String RUB_USD_PAIR = "RUB/USD";

    private final TransactionRepository transactionRepository;
    private final LimitService limitService;
    private final CheckedOnLimitService checkedOnLimitService;
    private final ConverterUtil converterUtil;
    private final ExchangeRateService exchangeRateService;
    private final Executor customExecutor;
    private final TransactionCRUDService transactionCRUDService;

    @Autowired
    public TransactionService(TransactionRepository transactionRepository, LimitService limitService, CheckedOnLimitService checkedOnLimitService, ConverterUtil converterUtil, ExchangeRateService exchangeRateService, @Qualifier("customExecutor") Executor customExecutor, TransactionCRUDService transactionCRUDService) {
        this.transactionRepository = transactionRepository;
        this.limitService = limitService;
        this.checkedOnLimitService = checkedOnLimitService;
        this.converterUtil = converterUtil;
        this.exchangeRateService = exchangeRateService;
        this.customExecutor = customExecutor;
        this.transactionCRUDService = transactionCRUDService;
    }

    @Async("customExecutor")
    public CompletableFuture<ResponseEntity<String>> setTransactionsToDB(List<TransactionDTO> transactionsListFromService) {
        // Check if the transactions list is null or empty
        if (transactionsListFromService == null || transactionsListFromService.isEmpty()) {
            return CompletableFuture.completedFuture(ResponseEntity.badRequest().body("The list was empty"));
        }
        // receive the latest limit asynchronously
        CompletableFuture<LimitDTO> limitDTO = limitService.getLatestLimitAsync();
        // receive transactions with rates based on the limit
        CompletableFuture<List<TransactionDTO>> transactionWithRates = limitDTO.thenCompose(limit -> getTransactionsWithRates(limit));

        // Combine results of limit and transactions with rates to check validation
        return limitDTO
                .thenCombine(transactionWithRates, (limit, transactAndRates) -> {
                    log.warn("method setTransactionsToDB, transactionsListFromService, transactAndRates,limit {} {} {}", transactionsListFromService, transactAndRates, limit);
                    // Call the asynchronous method to check transactions
                    return checkTransactionsOnActiveLimit(transactionsListFromService, transactAndRates, limit);
                })
                // Handle the ResponseEntity received from the checkTransactions method
                .thenCompose(responseFuture -> responseFuture) // Unwrap the CompletableFuture<ResponseEntity<String>>
                .exceptionally(ex -> {
                    // Log the error and return a 500 Internal Server Error response
                    log.error("Error occurred: {}", ex.getMessage());
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred: " + ex.getMessage());
                });
    }


    @Async("customExecutor")
    public CompletableFuture<ResponseEntity<String>> checkTransactionsOnActiveLimit(List<TransactionDTO> clientsTransactions,
                                                                                    List<TransactionDTO> dbTransactions, LimitDTO limitDTO) {
        // Fetch currency rates asynchronously
        CompletableFuture<Map<String, ExchangeRateDTO>> exchangeRatesMapFuture = getCurrencyRateAsMap();
        log.warn("method checkTransactionsOnActiveLimit, exchangeRatesMapFuture {}", exchangeRatesMapFuture);
        // Prepare to convert database transactions if they exist
        CompletableFuture<Map<String, List<TransactionDTO>>> convertedDBTransactionsFuture;
        log.warn("method checkTransactionsOnActiveLimit, dbTransactions {}", dbTransactions);
        if (dbTransactions != null && !dbTransactions.isEmpty()) {
            // Grouping DB transactions if available
            convertedDBTransactionsFuture = groupTransactionsByExpenseCategory(dbTransactions)
                    .thenApply(groupedDBTransactions -> {

                        // Remove transactions that don't belong to any category
                        groupedDBTransactions.remove("notInCategories");
                        return groupedDBTransactions;

                    })
                    .thenCombine(exchangeRatesMapFuture, (groupedTransactions, exchangeRateMap) -> convertTransactionsSumAndCurrencyByUSDAsync(groupedTransactions, exchangeRateMap))
                    .thenCompose(future -> future); // Combine futures for further processing
            log.warn("method checkTransactionsOnActiveLimit, convertedDBTransactionsFuture {}", convertedDBTransactionsFuture);

        } else {
            // Log case of no transactions found and complete with empty map
            log.info("No transactions found in the database for active limit.");
            convertedDBTransactionsFuture = CompletableFuture.completedFuture(Collections.emptyMap());
        }

        // Group client transactions
        log.warn("method checkTransactionsOnActiveLimit {}", clientsTransactions);
        CompletableFuture<Map<String, List<TransactionDTO>>> groupedClientsTransactionsFuture = groupTransactionsByExpenseCategory(clientsTransactions);
        log.warn("method checkTransactionsOnActiveLimit, groupedClientsTransactionsFuture {}", groupedClientsTransactionsFuture);
        // Saving transactions that are out of specified categories
        CompletableFuture<Void> outCategoryClientsTransactionsFuture = groupedClientsTransactionsFuture.thenAccept(groupedClients -> {
            // Extract transactions not categorized and save them
            log.warn("method checkTransactionsOnActiveLimit, notInCategories {}", groupedClients.get("notInCategories"));
            saveListTransactionsToDBAsync(groupedClients.get("notInCategories"));
            List<TransactionDTO> transactionsOutCategory = groupedClients.remove("notInCategories");

        });

        // Convert client transactions based on the currency rates
        CompletableFuture<Map<String, List<TransactionDTO>>> convertedClientsTransactionsFuture = groupedClientsTransactionsFuture
                .thenCombine(exchangeRatesMapFuture, (groupedTransactions, exchangeRateMap) -> convertTransactionsSumAndCurrencyByUSDAsync(groupedTransactions, exchangeRateMap))
                .thenCompose(future -> future); // Combine futures for further processing

        // Process transactions after the conversion has been completed
        CompletableFuture<Void> processTransactionsFuture = convertedClientsTransactionsFuture.thenCombine(convertedDBTransactionsFuture, (convertedClientsTr, convertedDBTr) -> {
            log.warn("method checkTransactionsOnActiveLimit convertedClientsTransactionsFuture {}", convertedClientsTransactionsFuture);
            log.warn("method checkTransactionsOnActiveLimit convertedClientsTransactionsFuture {}", convertedClientsTransactionsFuture);
            processTransactions(convertedClientsTr, convertedDBTr, limitDTO);
            return null; // Returning null since we are not interested in this value
        });

        // Combine all futures and return the appropriate response
        return CompletableFuture.allOf(outCategoryClientsTransactionsFuture, convertedClientsTransactionsFuture, convertedDBTransactionsFuture)
                .thenCompose(v -> CompletableFuture.completedFuture(ResponseEntity.ok("Transactions processed successfully")))
                .exceptionally(ex -> {
                    // Log error if any of the futures fail
                    log.error("An error occurred during transaction processing: {}", ex.getMessage());
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred during transaction processing: " + ex.getMessage());
                });
    }


    public CompletableFuture<ResponseEntity<String>> processTransactions(
            Map<String, List<TransactionDTO>> clientsTransactMap,
            Map<String, List<TransactionDTO>> dbTransactMap,
            LimitDTO limitDTO) {

        log.warn("method processTransactions convertedClientsTransactionsFuture {}, {},{}", clientsTransactMap, dbTransactMap, limitDTO);
        List<String> categories = List.of("service", "product");
        List<CompletableFuture<Void>> futures = new ArrayList<>(); // Collecting futures

        // Iterate over predefined categories
        for (String category : categories) {
            var clientsTransactions = clientsTransactMap.getOrDefault(category, Collections.emptyList());
            log.warn("method processTransactions clientsTransactions {},", clientsTransactions);
            var dbTransactions = dbTransactMap.getOrDefault(category, Collections.emptyList());
            log.warn("method processTransactions dbTransactions {},", dbTransactions);
            // Group client transactions
            CompletableFuture<Map<Integer, List<TransactionDTO>>> clientsGroupedFuture =
                    groupTransactionsByAccount(clientsTransactions);
            log.warn("method processTransactions clientsGroupedFuture {},", clientsGroupedFuture);
            CompletableFuture<Map<Integer, BigDecimal>> dbSummarizedFuture;

            if (!dbTransactions.isEmpty()) {
                // Group and summarize DB transactions
                dbSummarizedFuture = groupTransactionsByAccount(dbTransactions)
                        .thenCompose(this::summarizeGroupedTransactions);
            } else {
                // Log and complete with an empty map if no DB transactions
                log.info("No transactions found in the database for category: {}", category);
                dbSummarizedFuture = CompletableFuture.completedFuture(Collections.emptyMap());
            }
            log.warn("method processTransactions dbSummarizedFuture {},", dbSummarizedFuture);
            // Combine client and DB transaction summaries
            CompletableFuture<Void> future = clientsGroupedFuture
                    .thenCombine(dbSummarizedFuture, (clientGroups, dbSummaries) ->
                            additionTransactionsWithComparisonOnLimit(clientGroups, dbSummaries, limitDTO))
                    .thenCompose(Function.identity()) // Handle the nested CompletableFuture correctly
                    .exceptionally(ex -> {
                        log.error("Error processing transactions for category {}: {}", category, ex.getMessage());
                        return null;
                    });
            futures.add(future); // Collect future for later completion
        }
        // Wait for all futures to complete and build the response
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> ResponseEntity.ok("All categories processed successfully"))
                .exceptionally(ex -> {
                    log.error("Error processing transactions: {}", ex.getMessage());
                    return ResponseEntity.internalServerError().body("Error while processing transactions");
                });
    }

    @Async("customExecutor")
    public CompletableFuture<Void> additionTransactionsWithComparisonOnLimit(Map<Integer, List<TransactionDTO>> clientsTransactions,
                                                                             Map<Integer, BigDecimal> comparerExamplesDB,
                                                                             LimitDTO limitDTO) {

        List<CompletableFuture<Void>> futures = new ArrayList<>();
        log.warn("additionTransactionsWithComparisonOnLimit clientsTransactions, comparerExamplesDB, limitDTO {},{},{}", clientsTransactions, comparerExamplesDB, limitDTO);
        for (Map.Entry<Integer, List<TransactionDTO>> entry : clientsTransactions.entrySet()) {
            Integer accountFrom = entry.getKey();
            log.warn("additionTransactionsWithComparisonOnLimit accountFrom {}", accountFrom);
            List<TransactionDTO> clientsTransactionsList = entry.getValue();
            log.warn("additionTransactionsWithComparisonOnLimit clientsTransactionsList {}", clientsTransactionsList);
            BigDecimal limit = limitDTO.getLimitSum();
            AtomicReference<BigDecimal> dbSum = new AtomicReference<>(comparerExamplesDB.getOrDefault(accountFrom, BigDecimal.ZERO));
            log.warn("additionTransactionsWithComparisonOnLimit dbSum accountFrom {} {}", dbSum.get(), accountFrom);
            for (TransactionDTO tr : clientsTransactionsList) {
                CompletableFuture<Void> future;
                if (dbSum.get().add(tr.getConvertedSum()).compareTo(limit) <= 0) {
                    CompletableFuture<CheckedOnLimitDTO> savedCheckFuture = checkedOnLimitService
                            .saveCheckedOnLimitAsync(new CheckedOnLimit(tr.getId(), limitDTO.getId(), false));
                    log.warn("method additionTransactionsWithComparisonOnLimit savedCheckFuture {}", savedCheckFuture);
                    CompletableFuture<TransactionDTO> savedTransactionFuture = CompletableFuture.supplyAsync(() ->
                            transactionCRUDService.saveTransactionWithHandling(TransactionMapper.INSTANCE.toEntity(tr))
                    );
                    log.warn("method additionTransactionsWithComparisonOnLimit savedTransactionFuture {}", savedTransactionFuture);
                    future = savedCheckFuture.thenCombine(savedTransactionFuture, (checkedResult, savedTransaction) -> {
                                if (savedTransaction != null && checkedResult != null) {
                                    dbSum.updateAndGet(value -> value.add(tr.getConvertedSum()));
                                    log.warn("method additionTransactionsWithComparisonOnLimit dbSum {}", dbSum);
                                } else {
                                    throw new IllegalStateException("Failed to save transaction or checked limit.");
                                }
                                return null;
                            }).thenRun(() -> {
                            }) // This ensures Void is returned
                            .exceptionally(ex -> {
                                log.error("An error occurred while processing transaction ID {}: {}", tr.getId(), ex.getMessage());
                                return null;
                            });

                } else {
                    future = checkedOnLimitService.saveCheckedOnLimitAsync(new CheckedOnLimit(tr.getId(), limitDTO.getId(), true))
                            .handle((result, ex) -> {
                                if (ex != null) {
                                    log.error("Error saving checked on limit for transaction ID {}: {}", tr.getId(), ex.getMessage());
                                } else {
                                    log.info("Successfully saved checked on limit for transaction ID {}", tr.getId());
                                }
                                return null;
                            }).thenRun(() -> transactionCRUDService.saveTransactionWithHandling(TransactionMapper.INSTANCE.toEntity(tr)))
                            .thenRun(() -> {
                            }); // This ensures Void is returned
                }
                futures.add(future);
            }
        }

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    }

    //+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Async("customExecutor")
    CompletableFuture<Map<Integer, List<TransactionDTO>>> groupTransactionsByAccount(List<TransactionDTO> transactionsList) {
        return CompletableFuture.supplyAsync(() -> {
            if (transactionsList == null) {
                log.error("Error: transactionsDBList is null.");
                return new HashMap<Integer, List<TransactionDTO>>();
            }
            try {
                return transactionsList.stream()
                        .collect(Collectors.groupingBy(TransactionDTO::getAccountFrom));
            } catch (Exception e) {
                log.error("Error occurred while processing transactions: {}", e.getMessage());
                return new HashMap<Integer, List<TransactionDTO>>();
            }
        }).exceptionally(ex -> {
            log.error("Error in asynchronous processing: {}", ex.getMessage());
            return new HashMap<>();
        });
    }

    @Async("customExecutor")
    CompletableFuture<Map<Integer, BigDecimal>> summarizeGroupedTransactions(Map<Integer, List<TransactionDTO>> groupedTransactions) {
        return CompletableFuture.supplyAsync(() -> {
            if (groupedTransactions == null) {
                log.error("Error: groupedTransactions is null.");
                return new HashMap<Integer, BigDecimal>(); // Return an empty map
            }
            try {
                // Суммирование сгруппированных транзакций
                return groupedTransactions.entrySet().stream()
                        .collect(Collectors.toMap(
                                integerListEntry -> integerListEntry.getKey(),
                                entry -> entry.getValue().stream()
                                        .map(transactionDTO -> transactionDTO.getConvertedSum())
                                        .reduce(BigDecimal.ZERO, (bigDecimal, augend) -> bigDecimal.add(augend)) // Summing up the converted sums
                        ));
            } catch (Exception e) {
                log.error("Error occurred while processing grouped transactions: {}", e.getMessage());
                return new HashMap<Integer, BigDecimal>(); // Return an empty map in case of error
            }
        }).exceptionally(ex -> {
            log.error("Error in asynchronous processing: {}", ex.getMessage());
            return new HashMap<>(); // Return an empty map in case of exception
        });
    }

    @Async("customExecutor")
    CompletableFuture<Map<String, ExchangeRateDTO>> getCurrencyRateAsMap() {
        CompletableFuture<ExchangeRateDTO> kztUsdFuture = exchangeRateService.getCurrencyRate(KZT_USD_PAIR);
        CompletableFuture<ExchangeRateDTO> rubUsdFuture = exchangeRateService.getCurrencyRate(RUB_USD_PAIR);

        return kztUsdFuture.thenCombine(rubUsdFuture, (kztUsdRate, rubUsdRate) -> {
            Map<String, ExchangeRateDTO> exchangeRateMap = new HashMap<>();
            exchangeRateMap.put(KZT_USD_PAIR, kztUsdRate);
            exchangeRateMap.put(RUB_USD_PAIR, rubUsdRate);
            return exchangeRateMap;
        }).exceptionally(ex -> {
            log.error("Failed to get currency rates: {}", ex.getMessage(), ex);
            throw new RuntimeException("Error getting currency rates", ex);
        });
    }

    @Async("customExecutor")
    public CompletableFuture<List<TransactionDTO>> getTransactionsWithRates(LimitDTO limitDTO) {
        return CompletableFuture.supplyAsync(() -> {
            int maxRetries = 5;
            int attempt = 0;
            var limitId = limitDTO.getId();
            while (attempt < maxRetries) {
                try {
                    log.info("Attempt {} to fetch transaction projections for limitId: {}", attempt + 1, limitId);
                    List<TransactionProjection> projections = transactionRepository.getTransactionsWithRates(limitId);

                    // if no data then just empty list.
                    if (projections == null || projections.isEmpty()) {
                        log.info("No transaction projections found for limitId: {}", limitId);
                        return Collections.emptyList();
                    }

                    // if data not null then conversion.
                    return projections.stream()
                            .map(projection -> new TransactionDTO(
                                    projection.getId(),
                                    projection.getSum(),
                                    projection.getCurrency(),
                                    OffsetDateTime.ofInstant(projection.getDatetimeTransaction(), ZoneOffset.UTC),
                                    projection.getAccountFrom(),
                                    projection.getAccountTo(),
                                    projection.getExpenseCategory(),
                                    OffsetDateTime.ofInstant(projection.getTrDate(), ZoneOffset.UTC),
                                    projection.getExchangeRate(), null, null, false))
                            .collect(Collectors.toList());

                } catch (Exception e) {
                    log.error("Error fetching transaction projections for limitId {}: {}", limitId, e.getMessage());
                    attempt++;

                    if (attempt >= maxRetries) {
                        throw new RuntimeException("Failed to fetch transaction projections after " + maxRetries + " attempts", e);
                    }

                    try {
                        // sleep for one second.
                        Thread.sleep(1000);
                    } catch (InterruptedException interruptedException) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Interrupted during retry wait", interruptedException);
                    }
                }
            }

            // Мы не должны достичь этого участка кода
            return Collections.emptyList();
        }, customExecutor);
    }


    @Async("customExecutor")
    public CompletableFuture<Void> saveListTransactionsToDBAsync(List<TransactionDTO> transactionDTOListFromService) {
        return CompletableFuture.runAsync(() -> {
            if (transactionDTOListFromService == null || transactionDTOListFromService.isEmpty()) {
                log.warn("Transactions list is empty, no data to save in DB.");
                return;
            }
            try {
                transactionDTOListFromService.forEach(transactionDTO -> {
                    transactionCRUDService.saveTransactionWithHandling(TransactionMapper.INSTANCE.toEntity(transactionDTO));

                });
            } catch (Exception e) {
                log.error("Error occurred while saving data to DB: {}", e.getMessage(), e);
                throw new RuntimeException("Failed to save transactions: " + e.getMessage(), e);
            }
        });
    }

    @Async("customExecutor")
    CompletableFuture<Map<String, List<TransactionDTO>>> groupTransactionsByExpenseCategory(List<TransactionDTO> transactions) {
        return CompletableFuture.supplyAsync(() -> {
            Map<String, List<TransactionDTO>> map = new HashMap<>();

            // Initialize lists for each category
            map.put("service", new ArrayList<>());
            map.put("product", new ArrayList<>());
            map.put("notInCategories", new ArrayList<>());

            for (TransactionDTO transaction : transactions) {
                String category = transaction.getExpenseCategory();

                if ("service".equalsIgnoreCase(category)) {
                    map.get("service").add(transaction);
                } else if ("product".equalsIgnoreCase(category)) {
                    map.get("product").add(transaction);
                } else {
                    map.get("notInCategories").add(transaction);
                }
            }

            return map;
        });
    }

    private void convertTransactionToUSD(TransactionDTO transaction, ExchangeRateDTO exchangeRateRUB, ExchangeRateDTO exchangeRateKZT) {
        BigDecimal rate;
        if ("RUB".equalsIgnoreCase(transaction.getCurrency())) {
            rate = getExchangeRate(transaction.getExchangeRate(), exchangeRateRUB);
            transaction.setConvertedSum(converterUtil.currencyConverter(transaction.getSum(), rate));
            transaction.setConvertedCurrency("USD");
        } else if ("KZT".equalsIgnoreCase(transaction.getCurrency())) {
            rate = getExchangeRate(transaction.getExchangeRate(), exchangeRateKZT);
            transaction.setConvertedSum(converterUtil.currencyConverter(transaction.getSum(), rate));
            transaction.setConvertedCurrency("USD");
        } else if ("USD".equalsIgnoreCase(transaction.getCurrency())) {
            transaction.setConvertedSum(transaction.getSum());
            transaction.setConvertedCurrency(transaction.getCurrency());
        }
    }

    private BigDecimal getExchangeRate(BigDecimal customRate, ExchangeRateDTO defaultRate) {
        if (customRate != null && customRate.compareTo(BigDecimal.ZERO) > 0) {
            return customRate;
        } else if (defaultRate != null && defaultRate.getRate().compareTo(BigDecimal.ZERO) > 0) {
            return defaultRate.getRate();
        } else {
            log.warn("No valid exchange rate available.");
            return BigDecimal.ONE; // Fallback to 1 if no valid rate is available, though this might need refinement
        }
    }


    private CompletableFuture<Map<String, List<TransactionDTO>>> convertTransactionsSumAndCurrencyByUSDAsync(Map<String, List<TransactionDTO>> groupedTransactions, Map<String, ExchangeRateDTO> exchangeRateMap) {
        return CompletableFuture.supplyAsync(() -> {
            ExchangeRateDTO rubUsdRate = exchangeRateMap.get(RUB_USD_PAIR);
            ExchangeRateDTO kztUsdRate = exchangeRateMap.get(KZT_USD_PAIR);

            groupedTransactions.values().forEach(transactionsList -> transactionsList.forEach(transaction -> convertTransactionToUSD(transaction, rubUsdRate, kztUsdRate)));
            log.warn("method convertTransactionsSumAndCurrencyByUSDAsync, groupedTransactions {}", groupedTransactions);
            return groupedTransactions;
        }, customExecutor);
    }


}