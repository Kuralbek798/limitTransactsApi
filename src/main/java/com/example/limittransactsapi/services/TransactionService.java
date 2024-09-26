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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
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
        // Check if the transaction list is null or empty
        if (transactionsListFromService == null || transactionsListFromService.isEmpty()) {
            return CompletableFuture.completedFuture(ResponseEntity.badRequest().body("List is empty"));
        }

        // Get currency exchange rates
        CompletableFuture<Map<String, ExchangeRateDTO>> exchangeRatesMapFuture = getCurrencyRateAsMap();

        // Get the current limit
        CompletableFuture<LimitDTO> limitFuture = limitService.getLatestLimitAsync();

        // Get transactions with rates based on the current limit
        CompletableFuture<List<TransactionDTO>> dbTransactionsWithRates = limitFuture.thenCompose(limit -> getTransactionsWithRates(limit));

        // Group client transactions by expense categories
        CompletableFuture<Map<String, ConcurrentLinkedQueue<TransactionDTO>>> groupedClientsTransactionsFuture =
                groupTransactionsByExpenseCategory(transactionsListFromService)
                        .thenCompose(groupedClients -> {
                            // Save transactions that do not fall into categories to the database
                            saveListTransactionsToDBAsync(groupedClients.get("notInCategories"));
                            // Remove transactions outside of categories
                            groupedClients.remove("notInCategories");

                            // Return the modified map for further processing
                            return CompletableFuture.completedFuture(groupedClients);
                        });

        // Combine results for conversion
        CompletableFuture<Map<String, ConcurrentLinkedQueue<TransactionDTO>>> convertedClientsTransactionsFuture =
                groupedClientsTransactionsFuture.thenCombine(exchangeRatesMapFuture, (groupedClients, exchangeRates) ->
                        convertTransactionsSumAndCurrencyByUSDAsync(groupedClients, exchangeRates)
                ).thenCompose(Function.identity());

        // Combine results and perform validity checks
        return CompletableFuture.allOf(exchangeRatesMapFuture, convertedClientsTransactionsFuture, dbTransactionsWithRates, limitFuture)
                .thenCompose(voidResult ->
                        exchangeRatesMapFuture.thenCombine(convertedClientsTransactionsFuture, (exchangeRates, groupedClients) ->
                                dbTransactionsWithRates.thenCompose(transactAndRates ->
                                        limitFuture.thenCompose(limit ->
                                                // Now all data is collected, we can pass it to the validation method
                                                checkTransactionsOnActiveLimit(groupedClients, transactAndRates, exchangeRates, limit)
                                        )
                                )
                        ).thenCompose(Function.identity()) // Unwrap the final CompletableFuture from the validation
                )
                .exceptionally(ex -> {
                    // Log error and return a 500 Internal Server Error response
                    log.error("An error occurred: {}", ex.getMessage());
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body("An error occurred: " + ex.getMessage());
                });
    }

    //.....................................................................................................................................
    @Async("customExecutor")
    public CompletableFuture<ResponseEntity<String>> checkTransactionsOnActiveLimit(
            Map<String, ConcurrentLinkedQueue<TransactionDTO>> clientsTransactions,
            List<TransactionDTO> dbTransactions,
            Map<String, ExchangeRateDTO> exchangeRate,
            LimitDTO limitDTO) {


        CompletableFuture<Map<String, ConcurrentLinkedQueue<TransactionDTO>>> convertedDBTransactionsFuture;

        if (dbTransactions != null && !dbTransactions.isEmpty()) {

            convertedDBTransactionsFuture = groupTransactionsByExpenseCategory(dbTransactions)
                    .thenCompose(groupedDBTransactions -> {

                        groupedDBTransactions.remove("notInCategories");

                        return convertTransactionsSumAndCurrencyByUSDAsync(groupedDBTransactions, exchangeRate);
                    });
        } else {

            convertedDBTransactionsFuture = CompletableFuture.completedFuture(Collections.emptyMap());
        }

        CompletableFuture<Map<String, Map<Integer, ConcurrentLinkedQueue<TransactionDTO>>>>
                clientsTransactionsMap = groupClientTransactionsByCategory(clientsTransactions);

        return convertedDBTransactionsFuture
                .thenCombine(clientsTransactionsMap, (dbConvertedTr, clientsTrMap) ->
                        processTransactions(dbConvertedTr, clientsTrMap, limitDTO))
                .thenCompose(Function.identity()) // Убедитесь, что идет компоновка без прямого разложения
                .exceptionally(ex -> {
                    log.error("An error occurred: {}", ex.getMessage());
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body("An error occurred: " + ex.getMessage());
                });

    }

    @Async("customExecutor")
    public CompletableFuture<ResponseEntity<String>> processTransactions(
            Map<String, ConcurrentLinkedQueue<TransactionDTO>> dbTransactMap,
            Map<String, Map<Integer, ConcurrentLinkedQueue<TransactionDTO>>> clientsTransactMap,
            LimitDTO limitDTO) {

        List<String> categories = List.of("service", "product");
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        // Итерация по предопределённым категориям
        for (String category : categories) {
            Map<Integer, ConcurrentLinkedQueue<TransactionDTO>> clientsGroupedFuture = clientsTransactMap.getOrDefault(category, new ConcurrentHashMap<>());
            var dbTransactions = dbTransactMap.getOrDefault(category, new ConcurrentLinkedQueue<>());

            CompletableFuture<Map<Integer, BigDecimal>> dbSummarizedFuture;

            if (!dbTransactions.isEmpty()) {
                // Группировка и суммирование транзакций БД
                dbSummarizedFuture = groupTransactionsByAccount(dbTransactions)
                        .thenCompose((Map<Integer, ConcurrentLinkedQueue<TransactionDTO>> groupedTransactions) -> summarizeGroupedTransactions(groupedTransactions));
            } else {
                dbSummarizedFuture = CompletableFuture.completedFuture(Collections.emptyMap());
            }


            CompletableFuture<Void> future = dbSummarizedFuture
                    .thenCompose(dbSummaries ->
                            additionTransactionsWithComparisonOnLimit(dbSummaries, clientsGroupedFuture, limitDTO))
                    .exceptionally(ex -> {
                        log.error("Error processing transactions for category {}: {}", category, ex.getMessage());
                        return null;
                    });
            futures.add(future);
        }


        // Пример исправления с учетом исключений
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> ResponseEntity.ok("All categories processed successfully"))
                .exceptionally(ex -> {
                    log.error("Error processing transactions: {}", ex.getMessage());
                    return ResponseEntity.internalServerError().body("Error while processing transactions");
                });

    }

    /// ===========================================================================

    @Async("customExecutor")
    public CompletableFuture<Void> additionTransactionsWithComparisonOnLimit(Map<Integer, BigDecimal> comparerExamplesDB,
                                                                             Map<Integer, ConcurrentLinkedQueue<TransactionDTO>> clientsTransactions,
                                                                             LimitDTO limitDTO) {
        // Логирование входных данных
        log.info("Method additionTransactionsWithComparisonOnLimit called with parameters:");
        log.info("Comparer Examples DB: {}", describeComparerExamplesDB(comparerExamplesDB));
        log.info("Clients Transactions: {}", describeClientsTransactions(clientsTransactions));
        log.info("Limit DTO: {}", limitDTO);


        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (Map.Entry<Integer, ConcurrentLinkedQueue<TransactionDTO>> entry : clientsTransactions.entrySet()) {
            Integer accountFrom = entry.getKey();

            ConcurrentLinkedQueue<TransactionDTO> clientsTransactionsList = entry.getValue();

            BigDecimal limit = limitDTO.getLimitSum();
            log.info("limit sum: {}", limit);
            AtomicReference<BigDecimal> dbSum = new AtomicReference<>(comparerExamplesDB.getOrDefault(accountFrom, BigDecimal.ZERO));


            for (TransactionDTO tr : clientsTransactionsList) {
                CompletableFuture<Void> future;
                log.info("Проверка ПЕРЕД сравнением: dbSum={}, tr.getConvertedSum()={}, limit={}", dbSum.get(), tr.getConvertedSum(), limit);
                if (dbSum.updateAndGet(v -> v.add(tr.getConvertedSum())).compareTo(limit) <= 0) {
                    log.info("ПРОВЕРКА после dbSum {}", dbSum);
                    //saving transactions to DB
                    CompletableFuture<TransactionDTO> savedTransactionFuture = CompletableFuture.supplyAsync(() ->
                            transactionCRUDService.saveTransactionWithHandling(TransactionMapper.INSTANCE.toEntity(tr))
                    );
                    future = savedTransactionFuture.thenCompose(savedTransaction -> {
                        // receiving id from saved transaction.
                        CheckedOnLimit checkedOnLimit = new CheckedOnLimit(savedTransaction.getId(), limitDTO.getId(), false);
                        log.info("Сохраняем CheckedOnLimit с ID транзакции: {}", checkedOnLimit.getTransactionId());
                        // saving CheckedOnLimit - data with sign of exceeded or not
                        return checkedOnLimitService.saveCheckedOnLimitAsync(checkedOnLimit);
                    }).thenRun(() -> {
                        //adding saved transactions value to dbSum.
                        dbSum.updateAndGet(value -> value.add(tr.getConvertedSum()));
                        log.info("method additionTransactionsWithComparisonOnLimit dbSum обновлён до {}", dbSum.get());
                    }).exceptionally(ex -> {
                        log.error("An error occurred while processing transaction ID {}: {}", tr.getId(), ex.getMessage());
                        return null;
                    });

                } else {
                    //save transaction to DB
                    CompletableFuture<TransactionDTO> savedTransactionFuture = CompletableFuture.supplyAsync(() ->
                            transactionCRUDService.saveTransactionWithHandling(TransactionMapper.INSTANCE.toEntity(tr))
                    );
                    future = savedTransactionFuture.thenCompose(savedTransaction -> {
                        // receiving id from saved transaction.
                        CheckedOnLimit checkedOnLimitExceeded = new CheckedOnLimit(savedTransaction.getId(), limitDTO.getId(), true);
                        log.info("СОХРАНЯЕМ CheckedOnLimit с превышением лимита: {} {}", checkedOnLimitExceeded.getTransactionId(), checkedOnLimitExceeded.isLimitExceeded());
                        // saving CheckedOnLimit - data with sign of exceeded or not
                        return checkedOnLimitService.saveCheckedOnLimitAsync(checkedOnLimitExceeded);
                    }).handle((result, ex) -> {
                        if (ex != null) {
                            log.error("Error saving checked on limit for transaction ID {}: {}", tr.getId(), ex.getMessage());
                        } else {
                            log.info("Successfully saved checked on limit for transaction ID {}", tr.getId());
                        }
                        return null;
                    });

                }
                futures.add(future);
            }
        }

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    }

    /// ----------------------------------------------------------------------------------------------------


    // Вспомогательный метод для составления описания comparerExamplesDB
    private String describeComparerExamplesDB(Map<Integer, BigDecimal> comparerExamplesDB) {
        if (comparerExamplesDB == null) {
            return "null";
        }
        return comparerExamplesDB.entrySet().stream()
                .map(entry -> entry.getKey() + ": " + entry.getValue())
                .collect(Collectors.joining(", ", "{", "}"));
    }

    // Вспомогательный метод для составления описания клиентовских транзакций
    private String describeClientsTransactions(Map<Integer, ConcurrentLinkedQueue<TransactionDTO>> clientsTransactions) {
        if (clientsTransactions == null) {
            return "null";
        }
        return clientsTransactions.entrySet().stream()
                .map(entry -> entry.getKey() + " has [" + entry.getValue().size() + " transactions: " +
                        entry.getValue().stream().map(TransactionDTO::toString).collect(Collectors.joining(", ")) + "]")
                .collect(Collectors.joining(", ", "{", "}"));
    }

    //+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Async("customExecutor")
    public CompletableFuture<Map<Integer, ConcurrentLinkedQueue<TransactionDTO>>> groupTransactionsByAccount(ConcurrentLinkedQueue<TransactionDTO> transactionsList) {
        return CompletableFuture.supplyAsync(() -> {

            if (transactionsList == null) {
                log.error("Error: transactionsList is null.");
                return new HashMap<Integer, ConcurrentLinkedQueue<TransactionDTO>>();
            }
            // group transactions by account
            try {
                // create map where key - account from  and value queue of transactions
                return transactionsList.stream()
                        .collect(Collectors.groupingBy(
                                transactionDTO -> transactionDTO.getAccountFrom(),
                                Collectors.toCollection(() -> new ConcurrentLinkedQueue<TransactionDTO>())
                        ))
                        .entrySet().stream()
                        .collect(Collectors.toConcurrentMap(Map.Entry::getKey, e -> new ConcurrentLinkedQueue<>(e.getValue()))); // collecting to ConcurrentHashMap
            } catch (Exception e) {
                log.error("Error occurred while processing transactions: {}", e.getMessage());
                return new HashMap<Integer, ConcurrentLinkedQueue<TransactionDTO>>();
            }
        }).exceptionally(ex -> {
            log.error("Error in asynchronous processing: {}", ex.getMessage());
            return new ConcurrentHashMap<>();
        });
    }


    @Async("customExecutor")
    CompletableFuture<Map<Integer, BigDecimal>> summarizeGroupedTransactions(Map<Integer, ConcurrentLinkedQueue<TransactionDTO>> groupedTransactions) {
        return CompletableFuture.supplyAsync(() -> {
            if (groupedTransactions == null) {
                log.error("Error: groupedTransactions is null.");
                return new HashMap<Integer, BigDecimal>();
            }
            try {
                return groupedTransactions.entrySet().stream()
                        .collect(Collectors.toMap(
                                integerConcurrentLinkedQueueEntry -> integerConcurrentLinkedQueueEntry.getKey(),
                                entry -> entry.getValue().stream()
                                        .map(transactionDTO -> transactionDTO.getConvertedSum())
                                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                        ));
            } catch (Exception e) {
                log.error("Error occurred while processing grouped transactions: {}", e.getMessage());
                return new HashMap<Integer, BigDecimal>(); // Вернуть пустую карту в случае ошибки
            }
        }).exceptionally(ex -> {
            log.error("Error in asynchronous processing: {}", ex.getMessage());
            return new HashMap<>();
        });
    }


    @Async("customExecutor")
    CompletableFuture<Map<String, ExchangeRateDTO>> getCurrencyRateAsMap() {
        CompletableFuture<ExchangeRateDTO> kztUsdFuture = exchangeRateService.getCurrencyRate(KZT_USD_PAIR);
        CompletableFuture<ExchangeRateDTO> rubUsdFuture = exchangeRateService.getCurrencyRate(RUB_USD_PAIR);

        return kztUsdFuture.thenCombine(rubUsdFuture, (kztUsdRate, rubUsdRate) -> {

            Map<String, ExchangeRateDTO> exchangeRateMap = new ConcurrentHashMap<>();
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


            return Collections.emptyList();
        });
    }

    @Async("customExecutor")
    public CompletableFuture<Void> saveListTransactionsToDBAsync(ConcurrentLinkedQueue<TransactionDTO> transactionDTOListFromService) {
        return CompletableFuture.runAsync(() -> {
            if (transactionDTOListFromService == null || transactionDTOListFromService.isEmpty()) {
                log.warn("Transactions list is empty, no data to save in DB.");
                return;
            }
            try {
                // Сохранение транзакций
                transactionDTOListFromService.forEach(transactionDTO -> {
                    transactionCRUDService.saveTransactionWithHandling(TransactionMapper.INSTANCE.toEntity(transactionDTO));
                });

                log.info("Successfully saved {} transactions to the database.", transactionDTOListFromService.size());
            } catch (Exception e) {
                log.error("Error occurred while saving data to DB: {}", e.getMessage(), e);
                throw new RuntimeException("Failed to save transactions: " + e.getMessage(), e);
            }
        });
    }

    @Async("customExecutor")
    CompletableFuture<Map<String, ConcurrentLinkedQueue<TransactionDTO>>> groupTransactionsByExpenseCategory(List<TransactionDTO> transactions) {
        return CompletableFuture.supplyAsync(() -> {
            // Create a thread-safe map to store transaction categories
            Map<String, ConcurrentLinkedQueue<TransactionDTO>> map = new ConcurrentHashMap<>();

            // Initialize queues for each category
            map.put("service", new ConcurrentLinkedQueue<>());
            map.put("product", new ConcurrentLinkedQueue<>());
            map.put("notInCategories", new ConcurrentLinkedQueue<>());

            // Group transactions by category
            for (TransactionDTO transaction : transactions) {
                // Get the expense category of the transaction in lowercase
                String category = transaction.getExpenseCategory().toLowerCase();

                // Get the corresponding queue to add the transaction
                ConcurrentLinkedQueue<TransactionDTO> queue = map.getOrDefault(category, map.get("notInCategories"));
                queue.add(transaction); // Add the transaction to the appropriate queue
            }

            return map; // Return the map with categories and queues
        });
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

    @Async("customExecutor")
    CompletableFuture<Map<String, ConcurrentLinkedQueue<TransactionDTO>>> convertTransactionsSumAndCurrencyByUSDAsync(
            Map<String, ConcurrentLinkedQueue<TransactionDTO>> groupedTransactions,
            Map<String, ExchangeRateDTO> exchangeRateMap) {

        // checking the exchange rates.
        return CompletableFuture.supplyAsync(() -> {
            ExchangeRateDTO rubUsdRate = exchangeRateMap.get(RUB_USD_PAIR);
            ExchangeRateDTO kztUsdRate = exchangeRateMap.get(KZT_USD_PAIR);

            if (rubUsdRate == null || kztUsdRate == null) {
                log.error("Курсы обмена для RUB/USD или KZT/USD не найдены в exchangeRateMap.");
                throw new IllegalArgumentException("Недопустимые курсы обмена");
            }

            return new AbstractMap.SimpleEntry<>(groupedTransactions, new ExchangeRateDTO[]{rubUsdRate, kztUsdRate});
        }, customExecutor).thenCompose(entry -> {
            Map<String, ConcurrentLinkedQueue<TransactionDTO>> map = entry.getKey();
            ExchangeRateDTO rubUsdRate = entry.getValue()[0];
            ExchangeRateDTO kztUsdRate = entry.getValue()[1];

            // create queue for saving CompletableFuture.
            ConcurrentLinkedQueue<CompletableFuture<Void>> futures = new ConcurrentLinkedQueue<>();
            Map<String, ConcurrentLinkedQueue<TransactionDTO>> resultMap = new ConcurrentHashMap<>();

            // loop over transactions group
            map.forEach((key, transactionQueue) -> {
                ConcurrentLinkedQueue<TransactionDTO> convertedQueue = new ConcurrentLinkedQueue<>();
                resultMap.put(key, convertedQueue);

                transactionQueue.forEach(transaction -> {
                    CompletableFuture<Void> future = convertTransactionToUSDAsync(transaction, rubUsdRate, kztUsdRate)
                            .thenAccept(convertedTransaction -> {
                                if (convertedTransaction != null) {
                                    convertedQueue.add(convertedTransaction);
                                }
                            });
                    futures.add(future);
                });
            });


            return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .thenApply(v -> {
                        log.info("Транзакции успешно преобразованы в USD. Количество групп: {}", resultMap.size());
                        return resultMap;
                    });
        });
    }

    @Async("customExecutor")
    public CompletableFuture<TransactionDTO> convertTransactionToUSDAsync(TransactionDTO transaction, ExchangeRateDTO changeRateRUB, ExchangeRateDTO changeRateKZT) {
        return CompletableFuture.supplyAsync(() -> {
            // Логика преобразования
            BigDecimal rate;

            if ("RUB".equalsIgnoreCase(transaction.getCurrency())) {
                rate = getExchangeRate(transaction.getExchangeRate(), changeRateRUB);
                return new TransactionDTO(
                        transaction.getId(),
                        transaction.getSum(),
                        transaction.getCurrency(),
                        transaction.getDatetimeTransaction(),
                        transaction.getAccountFrom(),
                        transaction.getAccountTo(),
                        transaction.getExpenseCategory(),
                        transaction.getTrDate(),
                        transaction.getExchangeRate(),
                        converterUtil.currencyConverter(transaction.getSum(), rate),
                        "USD",
                        transaction.isLimitExceeded()
                );
            } else if ("KZT".equalsIgnoreCase(transaction.getCurrency())) {
                rate = getExchangeRate(transaction.getExchangeRate(), changeRateKZT);
                return new TransactionDTO(
                        transaction.getId(),
                        transaction.getSum(),
                        transaction.getCurrency(),
                        transaction.getDatetimeTransaction(),
                        transaction.getAccountFrom(),
                        transaction.getAccountTo(),
                        transaction.getExpenseCategory(),
                        transaction.getTrDate(),
                        transaction.getExchangeRate(),
                        converterUtil.currencyConverter(transaction.getSum(), rate),
                        "USD",
                        transaction.isLimitExceeded()
                );
            } else if ("USD".equalsIgnoreCase(transaction.getCurrency())) {
                return new TransactionDTO(
                        transaction.getId(),
                        transaction.getSum(),
                        transaction.getCurrency(),
                        transaction.getDatetimeTransaction(),
                        transaction.getAccountFrom(),
                        transaction.getAccountTo(),
                        transaction.getExpenseCategory(),
                        transaction.getTrDate(),
                        transaction.getExchangeRate(),
                        transaction.getSum(),
                        "USD",
                        transaction.isLimitExceeded()
                );
            } else {
                log.error("No conversions proceeded for currency: {}", transaction.getCurrency());
                throw new IllegalArgumentException("Unsupported currency: " + transaction.getCurrency());
            }
        });
    }

    @Async("customExecutor")
    public CompletableFuture<Map<String, Map<Integer, ConcurrentLinkedQueue<TransactionDTO>>>>
    groupClientTransactionsByCategory(Map<String, ConcurrentLinkedQueue<TransactionDTO>> clientsTransactions) {

        Map<String, Map<Integer, ConcurrentLinkedQueue<TransactionDTO>>> clientsTransactionsMap = new ConcurrentHashMap<>();
        clientsTransactionsMap.put("service", new ConcurrentHashMap<>());
        clientsTransactionsMap.put("product", new ConcurrentHashMap<>());

        List<String> categories = List.of("service", "product");

        CompletableFuture<Void> allFutures = CompletableFuture.completedFuture(null);

        for (String category : categories) {
            ConcurrentLinkedQueue<TransactionDTO> concurrentLinkedQueueMap = clientsTransactions.get(category);
            if (concurrentLinkedQueueMap != null) {
                allFutures = allFutures.thenCompose(v ->
                        groupTransactionsByAccount(concurrentLinkedQueueMap)
                                .thenAccept(groupedTransactions -> {
                                    clientsTransactionsMap.get(category).putAll(groupedTransactions);
                                })
                );
            }
        }

        return allFutures.thenApply(v -> clientsTransactionsMap);
    }

}