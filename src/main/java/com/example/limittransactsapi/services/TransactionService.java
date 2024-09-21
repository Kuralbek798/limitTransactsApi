package com.example.limittransactsapi.services;

import com.example.limittransactsapi.DTO.ExchangeRateDTO;
import com.example.limittransactsapi.DTO.LimitDTO;
import com.example.limittransactsapi.DTO.TransactionDTO;
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
        return limitService.getLatestLimitAsync().thenCompose(currentLimit -> getTransactionsWithRates(currentLimit.getId()).thenCompose(transactionsWithRates -> checkTransactionsOnActiveLimit(transactionsListFromService, transactionsWithRates, currentLimit))).exceptionally(ex -> {
            log.error("Error occurred: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred: " + ex.getMessage());
        });
    }

    @Async("customExecutor")
    CompletableFuture<ResponseEntity<String>> checkTransactionsOnActiveLimit(List<TransactionDTO> clientsTransactions, List<TransactionDTO> dbTransactions, LimitDTO currentLimit) {

        //receiving currency rate
        CompletableFuture<Map<String, ExchangeRateDTO>> exchangeRatesMapFuture = getCurrencyRateAsMap();
        //grouping data by category product/service/notInCategory
        CompletableFuture<Map<String, List<TransactionDTO>>> groupedDBTransactionsFuture = groupTransactions(dbTransactions);
        groupedDBTransactionsFuture.thenAccept(groupedDBTransactions -> groupedDBTransactions.remove("notInCategories"));
        CompletableFuture<Map<String, List<TransactionDTO>>> groupedClientsTransactionsFuture = groupTransactions(clientsTransactions);

        CompletableFuture<Void> outCategoryTransactionsFuture = groupedClientsTransactionsFuture.thenAccept(groupedClients -> {
            List<TransactionDTO> transactionsOutCategory = groupedClients.getOrDefault("notInCategories", new ArrayList<>());
            //save to DB
            saveListTransactionsToDBAsync(transactionsOutCategory);
            //remove data from map
            groupedClients.remove("notInCategories");
        });

        CompletableFuture<Map<String, List<TransactionDTO>>> convertedClientsTransactionsFuture = groupedClientsTransactionsFuture
                .thenCombine(exchangeRatesMapFuture, this::convertTransactionsSumAndCurrencyByUSDAsync)
                .thenCompose(CompletableFuture -> CompletableFuture);


        CompletableFuture<Map<String, List<TransactionDTO>>> convertedDBTransactionsFuture = groupedDBTransactionsFuture
                .thenCombine(exchangeRatesMapFuture, this::convertTransactionsSumAndCurrencyByUSDAsync)
                .thenCompose(CompletableFuture -> CompletableFuture);
        // .thenCompose(transactions -> groupAndSummarizeDBTransactionsByAccount(transactions));

        return CompletableFuture.allOf(outCategoryTransactionsFuture, convertedClientsTransactionsFuture, convertedDBTransactionsFuture).thenCompose(v -> CompletableFuture.completedFuture(ResponseEntity.ok("Transactions processed successfully"))).exceptionally(ex -> {
            log.error("An error occurred during transaction processing: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred during transaction processing: " + ex.getMessage());
        });
    }

    private void compareTransactionsOnLimit(Map<String, List<TransactionDTO>> clientsTransactMap,
                                            Map<String, List<TransactionDTO>> dbTransactMap, LimitDTO limit) {
        // CompletableFuture.thenCompose(transactions -> groupAndSummarizeDBTransactionsByAccount(transactions));
        List<String> categories = List.of("service", "product");
        for (String category : categories) {
            var clientsTransactions = clientsTransactMap.getOrDefault(category, Collections.emptyList());
            var dbTransactions = dbTransactMap.getOrDefault(category, Collections.emptyList());

            var clientsGroupedListMap = groupByAccountAndSort(clientsTransactions);
            var dbGroupedListMap = groupAndSummarizeDBTransactionsByAccount(dbTransactions);

        }

    }
    @Async("customExecutor")
    CompletableFuture<List<Map.Entry<Integer, List<TransactionDTO>>>> groupByAccountAndSort(List<TransactionDTO> clietnsTransactions) {

        return CompletableFuture.supplyAsync(() -> {
            List<Map.Entry<Integer, List<TransactionDTO>>> sortedClientsTransactions = clietnsTransactions.stream()
                    .collect(Collectors.groupingBy(TransactionDTO::getAccountFrom))
                    .entrySet()
                    .stream()
                    .collect(Collectors.toList());
            return sortedClientsTransactions;
        });

    }

    @Async("customExecutor")
    public CompletableFuture<Map<String, ExchangeRateDTO>> getCurrencyRateAsMap() {
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
            return projections.stream().map(projection -> new TransactionDTO(projection.getId(), projection.getSum(), projection.getCurrency(), OffsetDateTime.ofInstant(projection.getDatetimeTransaction(), ZoneOffset.UTC), projection.getAccountFrom(), projection.getAccountTo(), projection.getExpenseCategory(), OffsetDateTime.ofInstant(projection.getTrDate(), ZoneOffset.UTC), projection.getExchangeRate(), null, null, false)).collect(Collectors.toList());
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
                    transactionRepository.save(TransactionMapper.INSTANCE.toEntity(transactionDTO));
                });
            } catch (Exception e) {
                log.error("Error occurred while saving data to DB: {}", e.getMessage(), e);
                throw new RuntimeException("Failed to save transactions: " + e.getMessage(), e);
            }
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

    private CompletableFuture<Map<String, List<TransactionDTO>>> groupTransactions(List<TransactionDTO> transactions) {
        return CompletableFuture.supplyAsync(() -> transactions.stream().collect(Collectors.groupingBy(TransactionDTO::getExpenseCategory)), customExecutor);
    }

    private CompletableFuture<Map<String, List<TransactionDTO>>> convertTransactionsSumAndCurrencyByUSDAsync(Map<String, List<TransactionDTO>> groupedTransactions, Map<String, ExchangeRateDTO> exchangeRateMap) {
        return CompletableFuture.supplyAsync(() -> {
            ExchangeRateDTO rubUsdRate = exchangeRateMap.get(RUB_USD_PAIR);
            ExchangeRateDTO kztUsdRate = exchangeRateMap.get(KZT_USD_PAIR);

            groupedTransactions.values().forEach(transactionsList -> transactionsList.forEach(transaction -> convertTransactionToUSD(transaction, rubUsdRate, kztUsdRate)));

            return groupedTransactions;
        }, customExecutor);
    }

    @Async("customExecutor")
    public CompletableFuture<Map<Integer, BigDecimal>> groupAndSummarizeDBTransactionsByAccount(List<TransactionDTO> transactionsDBList) {
        return CompletableFuture.supplyAsync(() -> {
            if (transactionsDBList == null) {
                log.error("Error: transactionsDBList is null.");
                return new HashMap<Integer, BigDecimal>();
            }

            try {
                Map<Integer, BigDecimal> groupedTransactions = transactionsDBList.stream()
                        .collect(Collectors.groupingBy(
                                TransactionDTO::getAccountFrom,
                                Collectors.reducing(BigDecimal.ZERO, TransactionDTO::getConvertedSum, BigDecimal::add)
                        ));

                return groupedTransactions;
            } catch (Exception e) {
                log.error("Error occurred while processing transactions: {}", e.getMessage());
                return new HashMap<Integer, BigDecimal>(); // Specifying the type here
            }
        }).exceptionally(ex -> {
            log.error("Error in asynchronous processing: {}", ex.getMessage());
            return new HashMap<Integer, BigDecimal>();
        });
    }




   /* private List<TransactionDTO> summarizeTransactionsByAccount(Map<Integer, List<TransactionDTO>> transactions) {
        List<TransactionDTO> summarizedTransactions = new ArrayList<>();

        for (Map.Entry<Integer, List<TransactionDTO>> entry : transactions.entrySet()) {
            Integer accountFrom = entry.getKey();
            List<TransactionDTO> transactionList = entry.getValue();

            BigDecimal totalConvertedSum = transactionList.stream()
                    .map(transactionDTO -> transactionDTO.getConvertedSum())
                    .reduce(BigDecimal.ZERO, (bigDecimal, augend) -> bigDecimal.add(augend));

            String expenseCategory = transactionList.get(0).getExpenseCategory();

            TransactionDTO summaryTransaction =
                    new TransactionDTO(totalConvertedSum,
                            "USD",
                            expenseCategory,
                            accountFrom,
                            BigDecimal.ZERO);

            summarizedTransactions.add(summaryTransaction);
        }

        return summarizedTransactions;
    }*/


}