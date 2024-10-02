package com.example.limittransactsapi.services;

import com.example.limittransactsapi.Helpers.Converter;
import com.example.limittransactsapi.Helpers.mapper.TransactionMapper;
import com.example.limittransactsapi.Models.DTO.ExchangeRateDTO;
import com.example.limittransactsapi.Models.DTO.LimitAccountDTO;
import com.example.limittransactsapi.Models.DTO.TransactionDTO;
import com.example.limittransactsapi.Models.Entity.CheckedOnLimit;

import com.example.limittransactsapi.Models.TransactionsContext;
import com.example.limittransactsapi.repository.CategoryRepository;
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
    private final CategoryRepository categoryRepository;
    private final List<String> CATEGORIES;
    // private final List<String> CATEGORIES = List.of("service", "product");


    @Autowired
    public TransactionService(TransactionRepository transactionRepository, LimitService limitService, CheckedOnLimitService checkedOnLimitService, Converter ratesConverter, ExchangeRateService exchangeRateService, @Qualifier("customExecutor") Executor customExecutor, TransactionCRUDService transactionCRUDService, CategoryRepository categoryRepository) {
        this.transactionRepository = transactionRepository;
        this.limitService = limitService;
        this.checkedOnLimitService = checkedOnLimitService;
        this.ratesConverter = ratesConverter;
        this.exchangeRateService = exchangeRateService;
        this.customExecutor = customExecutor;
        this.transactionCRUDService = transactionCRUDService;
        this.categoryRepository = categoryRepository;
        CATEGORIES  = Collections.unmodifiableList(categoryRepository.findByIsActiveTrue().stream()
                .map(category -> category.getName())
                .toList());
    }

    @Async("customExecutor")
    public CompletableFuture<ResponseEntity<String>> categorizeAndPrepareClientsTransactionsAsync(List<TransactionDTO> transactionsListFromServiceList) {
        // Check if the transaction list is null or empty
        if (transactionsListFromServiceList == null || transactionsListFromServiceList.isEmpty()) {
            return CompletableFuture.completedFuture(ResponseEntity.badRequest().body("List is empty"));
        }
       ConcurrentLinkedQueue<TransactionDTO> transactionsListFromService = new ConcurrentLinkedQueue<>(transactionsListFromServiceList);
        /// Get currency exchange rates
        CompletableFuture<ConcurrentHashMap<String, ExchangeRateDTO>> exchangeRatesMapFuture = getCurrencyRateAsMapAsync();

        /// Group client transactions by expense categories
        var groupedClientsTransactionsFuture =
                groupTransactionsByExpenseCategoryAsync(transactionsListFromService)
                        .thenCompose(groupedClients -> {
                            // Save transactions that do not fall into categories to the database
                            saveListTransactionsToDBAsync(groupedClients.get("notInCategories"));
                            // Remove transactions out of categories
                            groupedClients.remove("notInCategories");
                            // Return the modified map for further processing
                            return CompletableFuture.completedFuture(groupedClients);
                        });

        /// Combine grouped client's transactions and exchange rates for conversion
        var convertedClientsTransactionsFuture =
                groupedClientsTransactionsFuture.thenCombine(exchangeRatesMapFuture, (groupedClients, exchangeRates) ->
                                convertTransactionsSumAndCurrencyByUSDAsync(groupedClients, exchangeRates))
                        .thenCompose(Function.identity())
                        .thenCompose(clientsTransactions ->
                                /// grouping clients transactions by account in category expense (product/service)
                                groupByAccountTransactionsIntoMapInCategoryAsync(clientsTransactions));

        return convertedClientsTransactionsFuture.thenCombine(exchangeRatesMapFuture, (groupedClients, exchangeRates) ->
                        categorizeAndPrepareDBTransactionsAsync(groupedClients, exchangeRates))
                .thenCompose(v -> v)
                .exceptionally(ex -> {
                    // Log error and return a 500 Internal Server Error response
                    log.error("An error occurred: {}", ex.getMessage());
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body("An error occurred: " + ex.getMessage());
                });
    }

    @Async("customExecutor")
    CompletableFuture<ResponseEntity<String>> categorizeAndPrepareDBTransactionsAsync(
            TransactionsContext clientsTransactionsContext,
            ConcurrentHashMap<String, ExchangeRateDTO> exchangeRatesMap) {

        Integer[] accountNumbersArray = clientsTransactionsContext.getTransactionsAccounts().toArray(new Integer[0]);
        /// Get the current limit
        CompletableFuture<ConcurrentLinkedQueue<LimitAccountDTO>> limitFuture = limitService.getAllActiveLimits(accountNumbersArray);

        /// Get transactions with rates based on the current limit and convert transactions by rate
        CompletableFuture<ConcurrentLinkedQueue<TransactionDTO>> convertedDBTransactionsWithRates = limitFuture
                .thenCompose(limits -> getConvertedTransactionsWithRatesAsync(limits, exchangeRatesMap));

        /// Grouping limits by account
        CompletableFuture<ConcurrentMap<Integer, LimitAccountDTO>> limitsMapByAccounts =
                limitFuture.thenCompose(limits -> convertToMapAsync(limits));

        /// Grouping DB transactions by expense category
        CompletableFuture<ConcurrentHashMap<String, ConcurrentLinkedQueue<TransactionDTO>>> convertedAndGroupedByCategoryDBTransactions =
                convertedDBTransactionsWithRates.thenCompose(convertedDbTransactions -> {
                    if (convertedDbTransactions != null && !convertedDbTransactions.isEmpty()) {
                        return groupTransactionsByExpenseCategoryAsync(convertedDbTransactions)
                                .thenCompose(groupedDBTransactions -> {
                                    groupedDBTransactions.remove("notInCategories");
                                    return CompletableFuture.completedFuture(groupedDBTransactions);
                                });
                    }
                    return CompletableFuture.completedFuture(new ConcurrentHashMap<>());
                });

        /// Sending transactions and limits to the method processTransactionsAndLimitsAsync
        return convertedAndGroupedByCategoryDBTransactions
                .thenCombine(limitsMapByAccounts, (dbConvertedTr, limitsMap) ->
                        processTransactionsAndLimitsAsync(dbConvertedTr, clientsTransactionsContext, limitsMap))
                .thenCompose(Function.identity())
                .exceptionally(ex -> {
                    log.error("An error occurred: {}", ex.getMessage());
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body("An error occurred: " + ex.getMessage());
                });
    }

    @Async("customExecutor")
    public CompletableFuture<ResponseEntity<String>> processTransactionsAndLimitsAsync(
            ConcurrentHashMap<String, ConcurrentLinkedQueue<TransactionDTO>> dbTransactMap,
           TransactionsContext clientsTransactionsContext,
            ConcurrentMap<Integer, LimitAccountDTO> limitsMapByAccount) {

        clientsTransactionsContext.getTransactionsMap().entrySet();
        // List to hold CompletableFutures for each category processing
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        // Create a CompletableFuture for each category
        for (String category : CATEGORIES) {
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    // Extract data for the current category
                    ConcurrentHashMap<Integer, ConcurrentLinkedQueue<TransactionDTO>> clientsGroupedFuture = clientsTransactionsContext.getTransactionsMap().getOrDefault(category, new ConcurrentHashMap<>());
                    var dbTransactions = dbTransactMap.getOrDefault(category, new ConcurrentLinkedQueue<>());

                    ConcurrentHashMap<Integer, BigDecimal> dbSummarized = new ConcurrentHashMap<>();

                    // Check if there are database transactions to process
                    if (!dbTransactions.isEmpty()) {
                        // Group and summarize transactions
                        dbSummarized = groupTransactionsByAccountAsync(dbTransactions)
                                .thenCompose(groupedTransactions -> summarizeGroupedTransactionsAsync(groupedTransactions)).join();
                    }

                    // Process and save transactions with limit check
                    saveTransactionsWithLimitCheckAsync(dbSummarized, clientsGroupedFuture, limitsMapByAccount);
                } catch (Exception ex) {
                    // Log any exceptions that occur during processing
                    log.error("Error processing transactions for category {}: {}", category, ex.getMessage());
                    throw ex; // Re-throw to propagate the exception
                }
            });

            // Add each CompletableFuture to the list
            futures.add(future);
        }

        // Wait for all futures to complete and handle the result
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> ResponseEntity.ok("All categories processed successfully"))
                .exceptionally(ex -> {
                    // Log any exceptions that occur during the overall processing
                    log.error("Error processing transactions: {}", ex.getMessage());
                    return ResponseEntity.internalServerError().body("Error processing transactions");
                });
    }

    @Async("customExecutor")
    CompletableFuture<Void> saveTransactionsWithLimitCheckAsync(ConcurrentHashMap<Integer, BigDecimal> dbTransactionsForCompareMap,
                                                                ConcurrentHashMap<Integer, ConcurrentLinkedQueue<TransactionDTO>> clientsTransactionsMap,
                                                                ConcurrentMap<Integer, LimitAccountDTO> limitsMapByAccount) {
        // Логирование входных данных
        log.info("Method additionTransactionsWithComparisonOnLimit called with parameters:");
        log.info("Comparer Examples DB: {}", ForLogsMethods.describeComparerExamplesDB(dbTransactionsForCompareMap));
        log.info("Clients Transactions: {}", ForLogsMethods.describeClientsTransactions(clientsTransactionsMap));
        log.info("Limit DTO: {}", limitsMapByAccount);

        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (ConcurrentHashMap.Entry<Integer, ConcurrentLinkedQueue<TransactionDTO>> entry : clientsTransactionsMap.entrySet()) {
            Integer keyAccountFrom = entry.getKey();

            ConcurrentLinkedQueue<TransactionDTO> clientsTransactionsList = entry.getValue();

            LimitAccountDTO limit;
            if (limitsMapByAccount.containsKey(keyAccountFrom)) {

                limit = limitsMapByAccount.get(keyAccountFrom);
            } else if (limitsMapByAccount.containsKey(0)) {

                limit = limitsMapByAccount.get(0);
                if (limit == null) {
                    throw new IllegalArgumentException("Ключ 0 отсутсвует в карте лимитов");
                }
            } else {
                throw new IllegalArgumentException("Ключ 0 отсутсвует в карте лимитов");
            }

            log.info("limit sum: {}", limit);
            AtomicReference<BigDecimal> dbSum = new AtomicReference<>(dbTransactionsForCompareMap.getOrDefault(keyAccountFrom, BigDecimal.ZERO));

            for (TransactionDTO tr : clientsTransactionsList) {
                CompletableFuture<Void> future;
                log.info("Проверка ПЕРЕД сравнением: dbSum={}, tr.getConvertedSum()={}, limit={}", dbSum.get(), tr.getConvertedSum(), limit);
                if (dbSum.updateAndGet(v -> v.add(tr.getConvertedSum())).compareTo(limit.getLimitSum()) <= 0) {
                    log.info("ПРОВЕРКА после dbSum {}", dbSum);
                    //saving transactions to DB
                    CompletableFuture<TransactionDTO> savedTransactionFuture = CompletableFuture.supplyAsync(() ->
                            transactionCRUDService.saveTransactionWithHandling(TransactionMapper.INSTANCE.toEntity(tr))
                    );
                    future = savedTransactionFuture.thenCompose(savedTransaction -> {
                        // receiving id from saved transaction.
                        CheckedOnLimit checkedOnLimit = new CheckedOnLimit(savedTransaction.getId(), limit.getId(), false);
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
                        CheckedOnLimit checkedOnLimitExceeded = new CheckedOnLimit(savedTransaction.getId(), limit.getId(), true);
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
     CompletableFuture<ConcurrentHashMap<Integer, BigDecimal>> summarizeGroupedTransactionsAsync(
            ConcurrentHashMap<Integer, ConcurrentLinkedQueue<TransactionDTO>> groupedTransactions) {
        try {
            if (groupedTransactions == null) {
                log.error("Error: groupedTransactions is null.");
                // Return an empty map wrapped in a completed CompletableFuture
                return CompletableFuture.completedFuture(new ConcurrentHashMap<>());
            }
            // Create a result map
            ConcurrentHashMap<Integer, BigDecimal> result = new ConcurrentHashMap<>();

            // Iterate through each entry in the groupedTransactions
            for (Map.Entry<Integer, ConcurrentLinkedQueue<TransactionDTO>> entry : groupedTransactions.entrySet()) {
                Integer accountId = entry.getKey();
                ConcurrentLinkedQueue<TransactionDTO> transactions = entry.getValue();

                // Sum the converted sums for each transaction
                BigDecimal totalSum = BigDecimal.ZERO;
                for (TransactionDTO transaction : transactions) {
                    totalSum = totalSum.add(transaction.getConvertedSum());
                }
                // Put the result into the map
                result.put(accountId, totalSum);
            }
            // Return the result wrapped in a completed CompletableFuture
            return CompletableFuture.completedFuture(result);

        } catch (Exception e) {
            log.error("Error occurred while processing grouped transactions: {}", e.getMessage(), e);
            // Return a failed CompletableFuture indicating an error
            return CompletableFuture.failedFuture(e);
        }
    }

    @Async("customExecutor")
    CompletableFuture<ConcurrentHashMap<String, ExchangeRateDTO>> getCurrencyRateAsMapAsync() {
        CompletableFuture<ExchangeRateDTO> kztUsdFuture = exchangeRateService.getCurrencyRate(KZT_USD_PAIR);
        CompletableFuture<ExchangeRateDTO> rubUsdFuture = exchangeRateService.getCurrencyRate(RUB_USD_PAIR);

        return kztUsdFuture.thenCombine(rubUsdFuture, (kztUsdRate, rubUsdRate) -> {

            ConcurrentHashMap<String, ExchangeRateDTO> exchangeRateMap = new ConcurrentHashMap<>();
            exchangeRateMap.put(KZT_USD_PAIR, kztUsdRate);
            exchangeRateMap.put(RUB_USD_PAIR, rubUsdRate);
            return exchangeRateMap;
        }).exceptionally(ex -> {
            log.error("Failed to get currency rates: {}", ex.getMessage(), ex);
            throw new RuntimeException("Error getting currency rates", ex);
        });
    }

    @Async("customExecutor")
    CompletableFuture<ConcurrentLinkedQueue<TransactionDTO>> getConvertedTransactionsWithRatesAsync(
            ConcurrentLinkedQueue<LimitAccountDTO> limitDTOQueue,
            ConcurrentHashMap<String, ExchangeRateDTO> exchangeRateMap) {

        ConcurrentLinkedQueue<TransactionDTO> transactionDTOs = new ConcurrentLinkedQueue<>(); // Queue for final result
        ExchangeRateDTO rubUsdRate = exchangeRateMap.get(RUB_USD_PAIR);
        ExchangeRateDTO kztUsdRate = exchangeRateMap.get(KZT_USD_PAIR);
        ConcurrentLinkedQueue<CompletableFuture<TransactionDTO>> futures = new ConcurrentLinkedQueue<>(); // Queue for CompletableFuture<TransactionDTO>

        for (LimitAccountDTO limitDTO : limitDTOQueue) {
            UUID limitId = limitDTO.getId();
            log.info("Fetching transaction projections for limitId: {}", limitId);

            try {
                // Retrieve transactions by limitId
                ConcurrentLinkedQueue<TransactionProjection> projections = transactionRepository.findTransactionsWithRatesByLimitId(limitId);

                if (projections == null || projections.isEmpty()) {
                    log.info("No transaction projections found for limitId: {}", limitId);
                    continue; // Skip this limitId if no projections found
                }

                for (TransactionProjection projection : projections) {
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
                    // Create CompletableFuture for each TransactionDTO
                    CompletableFuture<TransactionDTO> future = convertTransactionToUSDAsync(transactionDTO, rubUsdRate, kztUsdRate)
                            .handle((result, ex) -> {
                                if (ex != null) {
                                    log.error("Error converting transaction to USD for limitId {}: {}", limitId, ex.getMessage());
                                    throw new CompletionException("Conversion failed for transaction " + transactionDTO.getId(), ex);
                                }
                                return result;
                            });

                    futures.add(future); // Adding CompletableFuture to the ConcurrentLinkedQueue
                }
            } catch (Exception e) {
                log.error("Error fetching transaction projections for limitId {}: {}", limitId, e.getMessage());
            }
        }
        // Combine all CompletableFuture and add the result
        CompletableFuture<Void> combinedFuture = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenRun(() -> {
                    futures.forEach(future -> {
                        try {
                            TransactionDTO result = future.join(); // Get the result
                            transactionDTOs.add(result); // Add to ConcurrentLinkedQueue
                        } catch (CompletionException ex) {
                            log.error("Error joining future  {}: {}", ex.getMessage());
                        }
                    });
                });

        // Wait for all operations to finish and return the result
        return combinedFuture.thenApply(v -> transactionDTOs);
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
    CompletableFuture<ConcurrentHashMap<String, ConcurrentLinkedQueue<TransactionDTO>>>
    groupTransactionsByExpenseCategoryAsync(ConcurrentLinkedQueue<TransactionDTO> transactions) {
        try {
            // Create a thread-safe map to store transaction categories
            ConcurrentHashMap<String, ConcurrentLinkedQueue<TransactionDTO>> map = new ConcurrentHashMap<>();

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

    @Async("customExecutor")
    CompletableFuture<ConcurrentHashMap<String, ConcurrentLinkedQueue<TransactionDTO>>> convertTransactionsSumAndCurrencyByUSDAsync(
            ConcurrentHashMap<String, ConcurrentLinkedQueue<TransactionDTO>> groupedTransactions,
            ConcurrentHashMap<String, ExchangeRateDTO> exchangeRateMap) {

        //extracting exchange rates.
        ExchangeRateDTO rubUsdRate = exchangeRateMap.get(RUB_USD_PAIR);
        ExchangeRateDTO kztUsdRate = exchangeRateMap.get(KZT_USD_PAIR);

        //checking exchange rates
        if (rubUsdRate == null || kztUsdRate == null) {
            log.error("Курсы обмена для RUB/USD или KZT/USD не найдены в exchangeRateMap.");
            throw new NoSuchElementException("Недопустимые курсы обмена");
        }

        ConcurrentHashMap<String, ConcurrentLinkedQueue<TransactionDTO>> resultMap = new ConcurrentHashMap<>();
        ConcurrentLinkedQueue<CompletableFuture<Void>> futures = new ConcurrentLinkedQueue<>();

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
                            return null;
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

        BigDecimal rate;

        if ("RUB".equalsIgnoreCase(transaction.getCurrency())) {
            rate = getExchangeRate(transaction.getExchangeRate(), changeRateRUB);
            return CompletableFuture.completedFuture(new TransactionDTO(
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
                    transaction.isLimitExceeded())
            );
        } else if ("KZT".equalsIgnoreCase(transaction.getCurrency())) {
            rate = getExchangeRate(transaction.getExchangeRate(), changeRateKZT);
            return CompletableFuture.completedFuture(new TransactionDTO(
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
                    transaction.isLimitExceeded())
            );
        } else if ("USD".equalsIgnoreCase(transaction.getCurrency())) {
            return CompletableFuture.completedFuture(new TransactionDTO(
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
                    transaction.isLimitExceeded())
            );
        } else {

            log.error("No conversions proceeded for currency: {}", transaction.getCurrency());
              /*  CompletableFuture<TransactionDTO> future = new CompletableFuture<>();
                future.completeExceptionally(new IllegalArgumentException("Unsupported currency: " + transaction.getCurrency()));
                return future;*/
            throw new IllegalArgumentException("Unsupported currency: " + transaction.getCurrency());
        }

    }

    @Async("customExecutor")
    CompletableFuture<TransactionsContext> groupByAccountTransactionsIntoMapInCategoryAsync(
            ConcurrentHashMap<String, ConcurrentLinkedQueue<TransactionDTO>> transactionsMap) {

        ConcurrentHashMap<String, ConcurrentHashMap<Integer, ConcurrentLinkedQueue<TransactionDTO>>> clientsTransactionsMap = new ConcurrentHashMap<>();
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        TransactionsContext transactionsContext = new TransactionsContext();

        for (String category : CATEGORIES) {
            ConcurrentLinkedQueue<TransactionDTO> transactionsQueue = transactionsMap.get(category);
            if (transactionsQueue != null) {
                CompletableFuture<Void> future = groupTransactionsByAccountAsync(transactionsQueue)
                        .thenAccept(groupedTransactions -> {
                            // ads unique accounts and transactions.
                            transactionsContext.getTransactionsAccounts().addAll(groupedTransactions.keySet());
                            transactionsContext.getTransactionsMap().put(category, groupedTransactions);
                        });

                futures.add(future);
            }
        }
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .handle((v, ex) -> {
                    if (ex != null) {
                        log.error("Error occurred: {}", ex.getMessage());

                    }
                    return transactionsContext;
                });
    }

    @Async("customExecutor")
    CompletableFuture<ConcurrentHashMap<Integer, ConcurrentLinkedQueue<TransactionDTO>>> groupTransactionsByAccountAsync(
            ConcurrentLinkedQueue<TransactionDTO> transactionsQueue) {
        try {
            if (transactionsQueue == null) {
                log.error("Error: transactionsQueue is null.");
                throw new IllegalArgumentException("transactionsQueue is null");
            }
            ConcurrentHashMap<Integer, ConcurrentLinkedQueue<TransactionDTO>> result = new ConcurrentHashMap<>();

            for (TransactionDTO transaction : transactionsQueue) {
                Integer accountFrom = transaction.getAccountFrom();
                if (accountFrom == null) {
                    log.error("Transaction has a null account number: {}", transaction);
                    throw new IllegalArgumentException("Transaction has a null account number");
                }
                result.computeIfAbsent(accountFrom, k -> new ConcurrentLinkedQueue<>()).add(transaction);
            }
            // Return the grouped transactions wrapped in a completed CompletableFuture
            return CompletableFuture.completedFuture(result);

        } catch (Exception e) {
            log.error("Error occurred while processing transactions: {}", e.getMessage(), e);
            return CompletableFuture.failedFuture(e); // Return a failed CompletableFuture to indicate an error
        }
    }

    @Async("customExecutor")
    public CompletableFuture<ConcurrentMap<Integer, LimitAccountDTO>> convertToMapAsync(ConcurrentLinkedQueue<LimitAccountDTO> limitList) {
        try {
            if (limitList == null || limitList.isEmpty()) {
                log.error("Limits list is empty");
                throw new RuntimeException("Limits list is empty");
            }
            var result = limitList.stream()
                    .collect(Collectors.toConcurrentMap(
                            limitAccountDTO -> {
                                Integer accountNumber = limitAccountDTO.getAccountNumber();
                                if (accountNumber == null && limitAccountDTO.getLimitSum() == null) {
                                    log.error("Limit is null and accountNumber is null for: {}", limitAccountDTO);
                                    throw new IllegalArgumentException("Limit and Account Number cannot both be null");
                                }
                                if (limitAccountDTO.getLimitSum() != null && limitAccountDTO.isBaseLimit() == true && limitAccountDTO.isActive() == true) {
                                    accountNumber = 0;
                                }
                                return accountNumber;
                            },
                            limitAccountDTO -> limitAccountDTO
                    ));

            return CompletableFuture.completedFuture(result);

        } catch (Exception e) {
            log.error("Error occurred while processing transactions: {}", e.getMessage(), e);
            return CompletableFuture.failedFuture(e); // Возвращаем failedFuture в случае ошибки
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
}
