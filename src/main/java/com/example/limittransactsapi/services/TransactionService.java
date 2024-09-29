package com.example.limittransactsapi.services;


import com.example.limittransactsapi.Helpers.Converter;
import com.example.limittransactsapi.Helpers.mapper.TransactionMapper;
import com.example.limittransactsapi.Models.DTO.ExchangeRateDTO;
import com.example.limittransactsapi.Models.DTO.LimitAccountDTO;
import com.example.limittransactsapi.Models.DTO.LimitDTO;
import com.example.limittransactsapi.Models.DTO.TransactionDTO;
import com.example.limittransactsapi.Models.Entity.CheckedOnLimit;
import com.example.limittransactsapi.repository.projections.TransactionProjection;
import com.example.limittransactsapi.repository.TransactionRepository;
import com.example.limittransactsapi.services.crud.TransactionCRUDService;


import com.example.limittransactsapi.util.ForLogsMethods;
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
import java.util.concurrent.*;
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
    private final Converter ratesConverter;
    private final ExchangeRateService exchangeRateService;
    private final Executor customExecutor;
    private final TransactionCRUDService transactionCRUDService;


    @Autowired
    public TransactionService(TransactionRepository transactionRepository, LimitService limitService, CheckedOnLimitService checkedOnLimitService, Converter ratesConverter, ExchangeRateService exchangeRateService, @Qualifier("customExecutor") Executor customExecutor, TransactionCRUDService transactionCRUDService) {
        this.transactionRepository = transactionRepository;
        this.limitService = limitService;
        this.checkedOnLimitService = checkedOnLimitService;
        this.ratesConverter = ratesConverter;
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
        /// Get currency exchange rates
        CompletableFuture<Map<String, ExchangeRateDTO>> exchangeRatesMapFuture = getCurrencyRateAsMap();

        /// Get the current limit
        CompletableFuture<List<LimitAccountDTO>> limitFuture = limitService.getAllActiveLimits();
        // Get transactions with rates based on the current limit and convert transactions by rate
        CompletableFuture<List<TransactionDTO>> convertedDBTransactionsWithRates = exchangeRatesMapFuture
                .thenCombine(limitFuture, (exchangeRates, limitDTOList) -> getConvertedTransactionsWithRates(limitDTOList, exchangeRates))
                .thenCompose(Function.identity());

        /// Group client transactions by expense categories
        CompletableFuture<Map<String, ConcurrentLinkedQueue<TransactionDTO>>> groupedClientsTransactionsFuture =
                groupTransactionsByExpenseCategory(transactionsListFromService)
                        .thenCompose(groupedClients -> {
                            // Save transactions that do not fall into categories to the database
                            saveListTransactionsToDBAsync(groupedClients.get("notInCategories"));
                            // Remove transactions out of categories
                            groupedClients.remove("notInCategories");

                            // Return the modified map for further processing
                            return CompletableFuture.completedFuture(groupedClients);
                        });

        /// Combine grouped client's transactions and exchange rates for conversion
        CompletableFuture<Map<String, ConcurrentLinkedQueue<TransactionDTO>>> convertedClientsTransactionsFuture =
                groupedClientsTransactionsFuture.thenCombine(exchangeRatesMapFuture, (groupedClients, exchangeRates) ->
                        convertTransactionsSumAndCurrencyByUSDAsync(groupedClients, exchangeRates)
                ).thenCompose(Function.identity());

        /// Combine all results and sending on the next level
        return CompletableFuture.allOf(exchangeRatesMapFuture, convertedClientsTransactionsFuture, convertedDBTransactionsWithRates, limitFuture)
                .thenCompose(voidResult ->
                        exchangeRatesMapFuture.thenCombine(convertedClientsTransactionsFuture, (exchangeRates, groupedClients) ->
                                convertedDBTransactionsWithRates.thenCompose(transactAndRates ->
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
     CompletableFuture<ResponseEntity<String>> checkTransactionsOnActiveLimit(
            Map<String, ConcurrentLinkedQueue<TransactionDTO>> clientsTransactions,
            List<TransactionDTO> dbTransactions,
            Map<String, ExchangeRateDTO> exchangeRate,
            List<LimitAccountDTO> limitList) {


        CompletableFuture<Map<String, ConcurrentLinkedQueue<TransactionDTO>>> convertedAndGroupedByCategoryDBTransactions;
        /// groping DB transactions by expense category
        if (dbTransactions != null && !dbTransactions.isEmpty()) {

            convertedAndGroupedByCategoryDBTransactions = groupTransactionsByExpenseCategory(dbTransactions)
                    .thenCompose(groupedDBTransactions -> {

                        groupedDBTransactions.remove("notInCategories");

                        return CompletableFuture.completedFuture(groupedDBTransactions);
                    });
        } else {

            convertedAndGroupedByCategoryDBTransactions = CompletableFuture.completedFuture(Collections.emptyMap());
        }
        /// grouping clients transactions by account from in category expense (product/service)
        CompletableFuture<Map<String, Map<Integer, ConcurrentLinkedQueue<TransactionDTO>>>>
                clientsTransactionsMap = groupClientTransactionsByAccountInCategory(clientsTransactions);

        return convertedAndGroupedByCategoryDBTransactions
                .thenCombine(clientsTransactionsMap, (dbConvertedTr, clientsTrMap) ->
                        processTransactions(dbConvertedTr, clientsTrMap, limitList))
                .thenCompose(Function.identity())
                .exceptionally(ex -> {
                    log.error("An error occurred: {}", ex.getMessage());
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body("An error occurred: " + ex.getMessage());
                });

    }

    @Async("customExecutor")
    public CompletableFuture<Map<Integer, BigDecimal>> convertToMapAsync(List<LimitAccountDTO> limitList) {
        try {
            if (limitList == null || limitList.isEmpty()) {
                log.error("Limits list is empty");
                throw new RuntimeException("Limits list is empty");
            }

            var result = limitList.stream()
                    .collect(Collectors.toMap(
                            limitAccountDTO -> {
                                Integer accountNumber = limitAccountDTO.getAccountNumber();
                                if (accountNumber == null && limitAccountDTO.getLimitSum() != null) {
                                    return 0;
                                } else if (accountNumber == null && limitAccountDTO.getLimitSum() == null) {
                                    log.error("limit is null and accountNumber is null");
                                }
                                return accountNumber;
                            },
                            LimitAccountDTO::getLimitSum
                    ));

            return CompletableFuture.completedFuture(result);

        } catch (Exception e) {
            log.error("Error occurred while processing transactions: {}", e.getMessage(), e);
            return CompletableFuture.failedFuture(e); // Возвращаем failedFuture в случае ошибки
        }
    }


    @Async("customExecutor")
    public CompletableFuture<ResponseEntity<String>> processTransactions(
            Map<String, ConcurrentLinkedQueue<TransactionDTO>> dbTransactMap,
            Map<String, Map<Integer, ConcurrentLinkedQueue<TransactionDTO>>> clientsTransactMap,
            List<LimitAccountDTO> limitList) {

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
            //grouping limits by account.
            CompletableFuture<Map<Integer,BigDecimal>> limitsByAccounts = convertToMapAsync(limitList);

            CompletableFuture<Void> future = dbSummarizedFuture
                    .thenCombine(limitsByAccounts,(dbSummaries,limitsByAccountsMap) ->
                            additionTransactionsWithComparisonOnLimit(dbSummaries, clientsGroupedFuture, limitsByAccountsMap))
                    .thenCompose(Function.identity())
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
     CompletableFuture<Void> additionTransactionsWithComparisonOnLimit(Map<Integer, BigDecimal> comparerExamplesDB,
                                                                             Map<Integer, ConcurrentLinkedQueue<TransactionDTO>> clientsTransactions,
                                                                             Map<Integer,BigDecimal> limitDTO) {
        // Логирование входных данных
        log.info("Method additionTransactionsWithComparisonOnLimit called with parameters:");
        log.info("Comparer Examples DB: {}", ForLogsMethods.describeComparerExamplesDB(comparerExamplesDB));
        log.info("Clients Transactions: {}", ForLogsMethods.describeClientsTransactions(clientsTransactions));
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

    @Async("customExecutor")
     CompletableFuture<Map<Integer, ConcurrentLinkedQueue<TransactionDTO>>> groupTransactionsByAccount(
            ConcurrentLinkedQueue<TransactionDTO> transactionsQueue) {
        try {
            if (transactionsQueue == null) {
                log.error("Error: transactionsList is null.");
                return CompletableFuture.completedFuture(new ConcurrentHashMap<>());
            }
            // Group transactions by account
            Map<Integer, ConcurrentLinkedQueue<TransactionDTO>> result = transactionsQueue.stream()
                    .collect(Collectors.groupingBy(
                            transactionDTO -> transactionDTO.getAccountFrom(),
                            Collectors.toCollection(() -> new ConcurrentLinkedQueue<TransactionDTO>())
                    ))
                    .entrySet().stream()
                    .collect(Collectors.toConcurrentMap(integerConcurrentLinkedQueueEntry -> integerConcurrentLinkedQueueEntry.getKey(), e -> new ConcurrentLinkedQueue<>(e.getValue()))); // Collecting to ConcurrentHashMap

            // Return the grouped transactions map wrapped in a completed CompletableFuture
            return CompletableFuture.completedFuture(result);

        } catch (Exception e) {
            log.error("Error occurred while processing transactions: {}", e.getMessage(), e);
            // Return a failed CompletableFuture to indicate an error
            return CompletableFuture.failedFuture(e);
        }
    }

    @Async("customExecutor")
     CompletableFuture<Map<Integer, BigDecimal>> summarizeGroupedTransactions(Map<Integer, ConcurrentLinkedQueue<TransactionDTO>> groupedTransactions) {
        try {
            if (groupedTransactions == null) {
                log.error("Error: groupedTransactions is null.");
                // Return an empty map wrapped in a completed CompletableFuture
                return CompletableFuture.completedFuture(new HashMap<>());
            }
            // Summarize transactions for each account
            Map<Integer, BigDecimal> result = groupedTransactions.entrySet().stream()
                    .collect(Collectors.toMap(
                            integerConcurrentLinkedQueueEntry -> integerConcurrentLinkedQueueEntry.getKey(), // Map key: account ID
                            entry -> entry.getValue().stream()
                                    .map(transactionDTO -> transactionDTO.getConvertedSum()) // Extract the converted sum from each transaction
                                    .reduce(BigDecimal.ZERO, (bigDecimal, augend) -> bigDecimal.add(augend)) // Sum all converted sums
                    ));

            // Return the result wrapped in a completed CompletableFuture
            return CompletableFuture.completedFuture(result);

        } catch (Exception e) {
            log.error("Error occurred while processing grouped transactions: {}", e.getMessage(), e);
            // Return a failed CompletableFuture indicating an error
            return CompletableFuture.failedFuture(e);
        }
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
     CompletableFuture<List<TransactionDTO>> getConvertedTransactionsWithRates(List<LimitAccountDTO> limitDTOList, Map<String, ExchangeRateDTO> exchangeRateMap) {
        List<TransactionDTO> transactionDTOs = new ArrayList<>(); // List for final result
        ExchangeRateDTO rubUsdRate = exchangeRateMap.get(RUB_USD_PAIR);
        ExchangeRateDTO kztUsdRate = exchangeRateMap.get(KZT_USD_PAIR);

        List<CompletableFuture<Void>> allFutures = new ArrayList<>(); // List for all CompletableFuture<Void>

        for (LimitAccountDTO limitDTO : limitDTOList) {
            UUID limitId = limitDTO.getId();
            log.info("Fetching transaction projections for limitId: {}", limitId);

            try {
                // receiving transactions by limitId
                List<TransactionProjection> projections = transactionRepository.findTransactionsWithRatesByLimitId(limitId);

                if (projections == null || projections.isEmpty()) {
                    log.info("No transaction projections found for limitId: {}", limitId);
                    continue;
                }

                // creating CompletableFuture for each TransactionDTO
                List<CompletableFuture<TransactionDTO>> futures = projections.stream()
                        .map(projection -> {
                            TransactionDTO transactionDTO = new TransactionDTO(
                                    projection.getId(),
                                    projection.getSum(),
                                    projection.getCurrency(),
                                    OffsetDateTime.ofInstant(projection.getDatetimeTransaction(), ZoneOffset.UTC),
                                    projection.getAccountFrom(),
                                    projection.getAccountTo(),
                                    projection.getExpenseCategory(),
                                    OffsetDateTime.ofInstant(projection.getTrDate(), ZoneOffset.UTC),
                                    projection.getExchangeRate(), null, null, false);

                            return convertTransactionToUSDAsync(transactionDTO, rubUsdRate, kztUsdRate)
                                    .handle((result, ex) -> {
                                        if (ex != null) {
                                            log.error("Error converting transaction to USD for limitId {}: {}", limitId, ex.getMessage());
                                            throw new CompletionException("Conversion failed for transaction " + transactionDTO.getId(), ex);
                                        }
                                        return result;
                                    });
                        })
                        .collect(Collectors.toList());

                // combining all CompletableFuture for current limitId
                CompletableFuture<Void> combinedFuture = CompletableFuture.allOf(
                        futures.toArray(new CompletableFuture[0])
                ).thenRun(() -> {
                    // adding resul to the list
                    List<TransactionDTO> results = futures.stream()
                            .map(CompletableFuture::join)
                            .collect(Collectors.toList());
                    transactionDTOs.addAll(results);
                });

                allFutures.add(combinedFuture); // adding combined future to the common list
            } catch (Exception e) {
                log.error("Error fetching transaction projections for limitId {}: {}", limitId, e.getMessage());
            }
        }

        // waiting for finishing all operations.
        return CompletableFuture.allOf(allFutures.toArray(new CompletableFuture[0]))
                .thenApply(v -> transactionDTOs);
    }




    @Async("customExecutor")
     CompletableFuture<Void> saveListTransactionsToDBAsync(ConcurrentLinkedQueue<TransactionDTO> transactionDTOListFromService) {
        if (transactionDTOListFromService == null || transactionDTOListFromService.isEmpty()) {
            log.warn("Transactions list is empty, no data to save in DB.");
            return CompletableFuture.completedFuture(null);
        }
        try {
            //saving transactions
            transactionDTOListFromService.forEach(transactionDTO -> {
                transactionCRUDService.saveTransactionWithHandling(TransactionMapper.INSTANCE.toEntity(transactionDTO));
            });

            log.info("Successfully saved {} transactions to the database.", transactionDTOListFromService.size());
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            log.error("Error occurred while saving data to DB: {}", e.getMessage(), e);
            return CompletableFuture.failedFuture(new RuntimeException("Failed to save transactions: " + e.getMessage(), e));
        }
    }


    @Async("customExecutor")
     CompletableFuture<Map<String, ConcurrentLinkedQueue<TransactionDTO>>> groupTransactionsByExpenseCategory(List<TransactionDTO> transactions) {
        try {
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

            // Return the result wrapped in a completed CompletableFuture
            return CompletableFuture.completedFuture(map);

        } catch (Exception ex) {
            log.error("Error occurred while grouping transactions by category: {}", ex.getMessage(), ex);
            // Return a failed CompletableFuture to indicate an error
            return CompletableFuture.failedFuture(ex);
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

    @Async("customExecutor")
     CompletableFuture<Map<String, ConcurrentLinkedQueue<TransactionDTO>>> convertTransactionsSumAndCurrencyByUSDAsync(
            Map<String, ConcurrentLinkedQueue<TransactionDTO>> groupedTransactions,
            Map<String, ExchangeRateDTO> exchangeRateMap) {

        //extracting exchange rates.
        ExchangeRateDTO rubUsdRate = exchangeRateMap.get(RUB_USD_PAIR);
        ExchangeRateDTO kztUsdRate = exchangeRateMap.get(KZT_USD_PAIR);

        //checking exchange rates
        if (rubUsdRate == null || kztUsdRate == null) {
            log.error("Курсы обмена для RUB/USD или KZT/USD не найдены в exchangeRateMap.");
            throw new IllegalArgumentException("Недопустимые курсы обмена");
        }

        Map<String, ConcurrentLinkedQueue<TransactionDTO>> resultMap = new ConcurrentHashMap<>();
        List<CompletableFuture<Void>> futures = new ArrayList<>(); // List for CompletableFuture

        // loop over transactions categories (product/service)
        groupedTransactions.forEach((key, transactionQueue) -> {
            ConcurrentLinkedQueue<TransactionDTO> convertedQueue = new ConcurrentLinkedQueue<>();
            resultMap.put(key, convertedQueue); // creating a new queue in the result map

            transactionQueue.forEach(transaction -> {
                // async convertion
                CompletableFuture<Void> future = convertTransactionToUSDAsync(transaction, rubUsdRate, kztUsdRate)
                        .handle((result, ex) -> {
                            if (ex != null) {
                                log.error("Error converting transaction to USD for key {}: {}", key, ex.getMessage());
                                throw new CompletionException("Conversion failed for transaction " + transaction.getId(), ex);
                            }
                            // if handle catch no exceptions and result not null then adding to the queue
                            if (result != null) {
                                convertedQueue.add(result);
                            }
                            return null; //return null because it won't do any harm here.
                        });

                futures.add(future); // saving  CompletableFuture to the list
            });
        });

        // wait until all tasks finish.
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])) // Ждем завершения всех CompletableFuture
                .thenApply(v -> {
                    log.info("Транзакции успешно преобразованы в USD. Количество групп: {}", resultMap.size());
                    return resultMap; // returning resulMap
                });
    }









    @Async("customExecutor")
     CompletableFuture<TransactionDTO> convertTransactionToUSDAsync(TransactionDTO transaction, ExchangeRateDTO changeRateRUB, ExchangeRateDTO changeRateKZT) {
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
                        ratesConverter.currencyConverter(transaction.getSum(), rate),
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
                        ratesConverter.currencyConverter(transaction.getSum(), rate),
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
     CompletableFuture<Map<String, Map<Integer, ConcurrentLinkedQueue<TransactionDTO>>>>
    groupClientTransactionsByAccountInCategory(Map<String, ConcurrentLinkedQueue<TransactionDTO>> clientsTransactions) {

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
