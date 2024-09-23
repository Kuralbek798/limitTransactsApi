package com.example.limittransactsapi.services;

import com.example.limittransactsapi.DTO.CheckedOnLimitDTO;
import com.example.limittransactsapi.DTO.ExchangeRateDTO;
import com.example.limittransactsapi.DTO.LimitDTO;
import com.example.limittransactsapi.DTO.TransactionDTO;
import com.example.limittransactsapi.Entity.CheckedOnLimit;
import com.example.limittransactsapi.Entity.Transaction;
import com.example.limittransactsapi.mapper.TransactionMapper;
import com.example.limittransactsapi.repository.TransactionProjection;
import com.example.limittransactsapi.repository.TransactionRepository;
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

    @Autowired
    public TransactionService(TransactionRepository transactionRepository, LimitService limitService, CheckedOnLimitService checkedOnLimitService, ConverterUtil converterUtil, ExchangeRateService exchangeRateService, @Qualifier("taskExecutor") Executor customExecutor) {
        this.transactionRepository = transactionRepository;
        this.limitService = limitService;
        this.checkedOnLimitService = checkedOnLimitService;
        this.converterUtil = converterUtil;
        this.exchangeRateService = exchangeRateService;
        this.customExecutor = customExecutor;
    }

    @Async("customExecutor")
    public CompletableFuture<ResponseEntity<String>> setTransactionsToDB(List<TransactionDTO> transactionsListFromService) {
        if (transactionsListFromService == null || transactionsListFromService.isEmpty()) {
            return CompletableFuture.completedFuture(ResponseEntity.badRequest().body("The list was empty"));
        }
        return limitService.getLatestLimitAsync().thenCompose(currentLimit -> getTransactionsWithRates(currentLimit.getId())
                        .thenCompose(transactionsWithRates -> checkTransactionsOnActiveLimit(transactionsListFromService, transactionsWithRates, currentLimit)))
                .exceptionally(ex -> {
                    log.error("Error occurred: {}", ex.getMessage());
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred: " + ex.getMessage());
                });
    }

    @Async("customExecutor")
    CompletableFuture<ResponseEntity<String>> checkTransactionsOnActiveLimit(List<TransactionDTO> clientsTransactions, List<TransactionDTO> dbTransactions, LimitDTO limitDTO) {

        //receiving currency rate
        CompletableFuture<Map<String, ExchangeRateDTO>> exchangeRatesMapFuture = getCurrencyRateAsMap();
        //grouping data by category product/service/notInCategory
        CompletableFuture<Map<String, List<TransactionDTO>>> groupedDBTransactionsFuture = groupTransactionsByExpenseCategory(dbTransactions);
        //remove data which not in categories.
        groupedDBTransactionsFuture.thenAccept(groupedDBTransactions -> groupedDBTransactions.remove("notInCategories"));
        CompletableFuture<Map<String, List<TransactionDTO>>> groupedClientsTransactionsFuture = groupTransactionsByExpenseCategory(clientsTransactions);
        //save and remove data which not in categories.
        CompletableFuture<Void> outCategoryTransactionsFuture = groupedClientsTransactionsFuture.thenAccept(groupedClients -> {
            List<TransactionDTO> transactionsOutCategory = groupedClients.getOrDefault("notInCategories", new ArrayList<>());
            //save to DB
            saveListTransactionsToDBAsync(transactionsOutCategory);
            //remove data from map
            groupedClients.remove("notInCategories");
        });
        // Convert client transactions
        CompletableFuture<Map<String, List<TransactionDTO>>> convertedClientsTransactionsFuture = groupedClientsTransactionsFuture
                .thenCombine(exchangeRatesMapFuture, this::convertTransactionsSumAndCurrencyByUSDAsync)
                .thenCompose(CompletableFuture -> CompletableFuture);

        // Convert DB transactions
        CompletableFuture<Map<String, List<TransactionDTO>>> convertedDBTransactionsFuture = groupedDBTransactionsFuture
                .thenCombine(exchangeRatesMapFuture, this::convertTransactionsSumAndCurrencyByUSDAsync)
                .thenCompose(CompletableFuture -> CompletableFuture);

        // Process transactions after conversion
        CompletableFuture<Void> processTransactionsFuture = convertedClientsTransactionsFuture.thenCombine(convertedDBTransactionsFuture, (convertedClientsTr, convertedDBTr) -> {

            processTransactions(convertedClientsTr, convertedDBTr, limitDTO);
            return null;

        });

        return CompletableFuture.allOf(outCategoryTransactionsFuture, convertedClientsTransactionsFuture, convertedDBTransactionsFuture)
                .thenCompose(v -> CompletableFuture.completedFuture(ResponseEntity.ok("Transactions processed successfully")))
                .exceptionally(ex -> {
                    log.error("An error occurred during transaction processing: {}", ex.getMessage());
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred during transaction processing: " + ex.getMessage());
                });
    }

    public CompletableFuture<ResponseEntity<String>> processTransactions(Map<String, List<TransactionDTO>> clientsTransactMap,
                                                                         Map<String, List<TransactionDTO>> dbTransactMap,
                                                                         LimitDTO limitDTO) {
        List<String> categories = List.of("service", "product");
        List<CompletableFuture<Void>> futures = new ArrayList<>(); // Collecting futures

        for (String category : categories) {
            var clientsTransactions = clientsTransactMap.getOrDefault(category, Collections.emptyList());
            var dbTransactions = dbTransactMap.getOrDefault(category, Collections.emptyList());

            CompletableFuture<Map<Integer, List<TransactionDTO>>> clientsGroupedFuture = groupTransactionsByAccount(clientsTransactions);
            CompletableFuture<Map<Integer, BigDecimal>> dbSummarizedFuture = groupTransactionsByAccount(dbTransactions)
                    .thenCompose(this::summarizeGroupedTransactions);

            CompletableFuture<Void> future = clientsGroupedFuture
                    .thenCombine(dbSummarizedFuture, (clientGroups, dbSummaries) ->
                            additionTransactionsWithComparisonOnLimit(clientGroups, dbSummaries, limitDTO))
                    .thenCompose(Function.identity()) // Correctly handle the nested CompletableFuture
                    .exceptionally(ex -> {
                        log.error("Error processing transactions for category {}: {}", category, ex.getMessage());
                        return null;
                    });

            futures.add(future);
        }

        // Wait for all futures to complete and build the response
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> ResponseEntity.ok("All categories processed successfully"))
                .exceptionally(ex -> {
                    log.error("Error processing transactions: {}", ex.getMessage());
                    return ResponseEntity.internalServerError().body("Error while processing transactions");
                });
    }



/*
    @Async("customExecutor")
    CompletableFuture<Void> additionTransactionsWithComparisonOnLimit(Map<Integer, List<TransactionDTO>> clientsTransactions,
                                                                      Map<Integer, BigDecimal> comparerExamplesDB,
                                                                      LimitDTO limitDTO) {

        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (Map.Entry<Integer, List<TransactionDTO>> entry : clientsTransactions.entrySet()) {
            Integer accountFrom = entry.getKey();
            List<TransactionDTO> clientsTransactionsList = entry.getValue();
            BigDecimal limit = limitDTO.getLimitAmount();
            AtomicReference<BigDecimal> dbSum = new AtomicReference<>(comparerExamplesDB.getOrDefault(accountFrom, BigDecimal.ZERO));

            for (TransactionDTO tr : clientsTransactionsList) {
                CompletableFuture<Void> future;
                if (dbSum.get().add(tr.getConvertedSum()).compareTo(limit) <= 0) {
                    CompletableFuture<CheckedOnLimitDTO> savedCheckFuture = checkedOnLimitService
                            .saveCheckedOnLimitAsync(new CheckedOnLimit(tr.getId(), limitDTO.getId(), false));

                    CompletableFuture<TransactionDTO> savedTransactionFuture = CompletableFuture.supplyAsync(() ->
                            saveTransactionWithHandling(TransactionMapper.INSTANCE.toEntity(tr))
                    );

                    future = savedCheckFuture.thenCombine(savedTransactionFuture, (checkedResult, savedTransaction) -> {
                                if (savedTransaction != null && checkedResult != null) {
                                    dbSum.updateAndGet(value -> value.add(tr.getConvertedSum()));
                                } else {
                                    throw new IllegalStateException("Failed to save transaction or checked limit.");
                                }
                                return null;
                            }).thenRun(() -> {}) // This ensures Void is returned
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
                            }).thenRun(() -> saveTransactionWithHandling(TransactionMapper.INSTANCE.toEntity(tr)))
                            .thenRun(() -> {}); // This ensures Void is returned
                }
                futures.add(future);
            }
        }

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    }
*/






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
            return new HashMap<Integer, List<TransactionDTO>>();
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
            return new HashMap<Integer, BigDecimal>(); // Return an empty map in case of exception
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
    public CompletableFuture<List<TransactionDTO>> getTransactionsWithRates(UUID limitId) {
        return CompletableFuture.supplyAsync(() -> {
            List<TransactionProjection> projections = transactionRepository.getTransactionsWithRates(limitId);
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
                            projection.getExchangeRate(), null, null, false)).collect(Collectors.toList());
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
                    saveTransactionWithHandling(TransactionMapper.INSTANCE.toEntity(transactionDTO));

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

            return groupedTransactions;
        }, customExecutor);
    }


    private TransactionDTO saveTransactionWithHandling(Transaction transaction) {
        if (transaction == null) {
            log.error("Transaction object is null.");
            throw new IllegalArgumentException();
        }

        int maxAttempts = 3;
        int attempt = 0;
        Exception lastException = null;

        while (attempt < maxAttempts) {
            try {
                var savedTr = transactionRepository.save(transaction);
                if (savedTr != null) {
                    return TransactionMapper.INSTANCE.toDTO(savedTr);
                }
            } catch (Exception e) {
                lastException = e;
                attempt++;
                log.error("Attempt " + attempt + " failed to save transaction. Error: " + e.getMessage());
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    log.error("Thread was interrupted during sleep. Error: " + ie.getMessage());
                }
            }
        }

        log.error("Error saving transaction after " + maxAttempts + " attempts: " + lastException.getMessage());
        return null;
    }

}