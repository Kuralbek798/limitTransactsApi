package com.example.limittransactsapi.services;


import com.example.limittransactsapi.DTO.CheckedOnLimitDTO;
import com.example.limittransactsapi.DTO.ExchangeRateDTO;
import com.example.limittransactsapi.DTO.LimitDTO;
import com.example.limittransactsapi.DTO.TransactionDTO;


import com.example.limittransactsapi.mapper.CheckedOnLimitMapper;
import com.example.limittransactsapi.mapper.TransactionMapper;
import com.example.limittransactsapi.repository.TransactionProjection;
import com.example.limittransactsapi.repository.TransactionRepository;
import com.example.limittransactsapi.util.ConverterUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.*;
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


    @Autowired
    public TransactionService(TransactionRepository transactionRepository, LimitService limitService,
                              CheckedOnLimitService checkedOnLimitService, ConverterUtil converterUtil, ExchangeRateService exchangeRateService) {
        this.transactionRepository = transactionRepository;
        this.limitService = limitService;
        this.checkedOnLimitService = checkedOnLimitService;
        this.converterUtil = converterUtil;
        this.exchangeRateService = exchangeRateService;
    }

    //Creates transactions
    public ResponseEntity sendTransactionsToDB(List<TransactionDTO> transactionsListFromService) {
        try {
            if (!transactionsListFromService.isEmpty()) {
                //receiving current limit for category product/service.
                var currentLimit = getCurrentLimit();
                //receiving transactions and rates by limit id with limit_exceeded false
                var transactionsWithRates = getTransactionsWithRates(currentLimit.getId());
                checkTransactionsOnActiveLimit(transactionsListFromService, transactionsWithRates, currentLimit);

                return ResponseEntity.ok().build();

            } else {
                return ResponseEntity.badRequest().body("The list was empty");
            }

        } catch (DataIntegrityViolationException e) {
            log.error("Ошибка при сохранении в БД: {}", e.getMessage(), e);
            throw new DataIntegrityViolationException("Ошибка при сохранении транзакции: " + e.getMessage(), e);
        } catch (DataAccessException e) {
            log.error("Ошибка при доступе к БД: {}", e.getMessage(), e);
            throw new DataAccessException("Ошибка при сохранении транзакции: " + e.getMessage(), e) {
            };
        }
    }

    private LimitDTO getCurrentLimit() {
        try {
            Optional<LimitDTO> optionalCurrentLimitDto = limitService.getLatestLimitInOptionalLimitDto();
            LimitDTO currentLimit = null;
            if (optionalCurrentLimitDto.isPresent()) {
                currentLimit = optionalCurrentLimitDto.get();
            }
            return currentLimit;
        } catch (Exception e) {
            log.error("TransactionService class error occurred in the method getCurrentLimit {}", e.getMessage(), e);
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public void checkTransactionsOnActiveLimit(List<TransactionDTO> transactionsListFromService, List<TransactionDTO> transactionsWithRates, LimitDTO currentLimit) {
        for (TransactionDTO transaction : transactionsListFromService) {
            try {
                var exchangeRatesMap = getCurrencyRateAsMap();

                var transactionsByAccountAndCategory = groupTransactionsByAccountAndCategory(transactionsWithRates);



                // Check the transaction based on the expense category
                if ("product".equalsIgnoreCase(transaction.getExpenseCategory()) || "service".equalsIgnoreCase(transaction.getExpenseCategory())) {
                    BigDecimal transactionSum = getConvertedSumInUSD(transaction, exchangeRatesMap);
                    //creating total sum of transactions
                    BigDecimal additionalSum = findTheSameAccountFromAndExpenseCategory(transactionsByAccountAndCategory, transaction);
                    var totalSum = transactionSum.add(additionalSum);
                    var checkedOnLimitDTO = getCheckedOnLimitDTO(transaction, currentLimit, totalSum);
                    //saving data to DB.
                    transactionRepository.save(TransactionMapper.INSTANCE.toEntity(transaction));
                    checkedOnLimitService.saveCheckedOnLimit(CheckedOnLimitMapper.INSTANCE.toEntity(checkedOnLimitDTO));
                    if (checkedOnLimitDTO.isLimitExceeded() == false) {
                        TransactionDTO transactionDTO = createTransactionDTOwithData(transaction, exchangeRatesMap);
                        transactionsWithRates.add(transactionDTO);
                    }
                }
            } catch (Exception e) {
                log.error("Error processing transaction ID: {}. Exception: {}", transaction.getId(), e.getMessage(), e);
            }
        }
    }

    private TransactionDTO createTransactionDTOwithData(TransactionDTO transaction, Map<String, ExchangeRateDTO> exchangeRatesMap) {

        var rate = getConvertedSumInUSD(transaction, exchangeRatesMap);
        return new TransactionDTO() {
            {
                setAmount(transaction.getAmount());
                setCurrency(transaction.getCurrency());
                setDatetimeTransaction(transaction.getDatetimeTransaction());
                setAccountFrom(transaction.getAccountFrom());
                setAccountTo(transaction.getAccountTo());
                setExpenseCategory(transaction.getExpenseCategory());
                setTrDate(transaction.getDatetimeTransaction().truncatedTo(ChronoUnit.DAYS));
                setExchangeRate(rate);
            }
        };
    }

    private List<TransactionDTO> groupTransactionsByAccountAndCategory(List<TransactionDTO> transactionsWithRates) {

        //Initial field sum on rate and amount
        var transactionsWithSumByRates = getTransactionsWithCalculatedSum(transactionsWithRates);
        //create list by group accountFrom and expense
        List<TransactionDTO> transactionsByAccountAndCategory = groupAndSummarizeTransactions(transactionsWithSumByRates);

        return transactionsByAccountAndCategory;
    }

    private BigDecimal getConvertedSumInUSD(TransactionDTO transaction, Map<String, ExchangeRateDTO> exchangeRatesMap) {
        try {
            BigDecimal sum = null;
            if (!"USD".equalsIgnoreCase(transaction.getCurrency())) {
                if (transaction.getCurrency().equalsIgnoreCase("RUB")) {
                    var exchangeRate = exchangeRatesMap.get(RUB_USD_PAIR);
                    if (exchangeRate != null) {
                        // Convert the transaction sum to USD using the latest currency rate
                        return converterUtil.currencyConverter(transaction.getSum(), exchangeRate.getRate());
                    }
                } else if (transaction.getCurrency().equalsIgnoreCase("KZT")) {
                    var exchangeRate = exchangeRatesMap.get(KZT_USD_PAIR);
                    if (exchangeRate != null) {
                        // Convert the transaction sum to USD using the latest currency rate
                        return converterUtil.currencyConverter(transaction.getSum(), exchangeRate.getRate());
                    }
                }
            }
            if ("USD".equalsIgnoreCase(transaction.getCurrency())) {
                sum = transaction.getSum();
            }
            return sum;
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private Map<String, ExchangeRateDTO> getCurrencyRateAsMap() {
        try {
            Map<String, ExchangeRateDTO> exchangeRateMap = new HashMap<>();
            exchangeRateMap.put("KZT/USD", exchangeRateService.getCurrencyRate("KZT/USD"));
            exchangeRateMap.put("RUB/USD", exchangeRateService.getCurrencyRate("RUB/USD"));

            return exchangeRateMap;

        } catch (Exception e) {
            log.error("Failed to get currency rate for currency: {}. Exception: {}", e.getMessage(), e);
            throw new RuntimeException(e.getMessage(), e);
        }
    }


    private BigDecimal findTheSameAccountFromAndExpenseCategory(List<TransactionDTO> transactionsByAccountAndCategory, TransactionDTO transaction) {
        try {
            // Filtering transactions with matching 'accountFrom' и 'expenseCategory'
            BigDecimal totalSum = transactionsByAccountAndCategory.stream()
                    .filter(transactionFromList -> transactionFromList.getExpenseCategory().equalsIgnoreCase(transaction.getExpenseCategory()) &&
                            transactionFromList.getAccountFrom().equals(transaction.getAccountFrom()))
                    // summing up all found transactions
                    .map(transactionFromList -> Optional.ofNullable(transactionFromList.getSum()).orElse(BigDecimal.ZERO))
                    .reduce(BigDecimal.ZERO, BigDecimal::add); // summing up all found sums.

            // Adding the amount of transferred transaction
            totalSum = totalSum.add(Optional.ofNullable(transaction.getSum()).orElse(BigDecimal.ZERO));

            return totalSum;
        } catch (Exception e) {
            log.error("Ошибка при вычислении дополнительной суммы для категории: {}. Exception: {}", e.getMessage(), e);
            throw new RuntimeException("TransactionService: ошибка в методе findTheSameAccountFromAndExpenseCategory: " + e.getMessage(), e);
        }
    }
/*
    private BigDecimal findTheSameAccountFromAndExpenseCategory(List<TransactionDTO> transactionsByAccountAndCategory, TransactionDTO transaction) {
        try {
            return transactionsByAccountAndCategory.stream()
                    .filter(transactionFromList -> transactionFromList.getExpenseCategory().equalsIgnoreCase(transaction.getExpenseCategory()) &&
                            transactionFromList.getAccountFrom().equals(transaction.getAccountFrom()))
                    .findFirst()//search for first match.
                    .map(transactionFromList -> {
                        BigDecimal sum = Optional.ofNullable(transactionFromList.getSum()).orElse(BigDecimal.ZERO)
                                .add(Optional.ofNullable(transaction.getSum()).orElse(BigDecimal.ZERO));
                        return sum;
                    })
                    .orElse(BigDecimal.ZERO);//if there are no match found return 0;

        } catch (Exception e) {
            log.error("Error calculating additional sum for category: {}. Exception: {}", e.getMessage(), e);
            throw new RuntimeException("TransactionSerfis error ocured in the method findTheSameAccountFromAndExpenseCategory" + e.getMessage(), e);
        }
    }*/

    private CheckedOnLimitDTO getCheckedOnLimitDTO(TransactionDTO transaction, LimitDTO currentLimit, BigDecimal totalSum) {
        try {
            CheckedOnLimitDTO checkedOnLimit = new CheckedOnLimitDTO();
            boolean isExceeded = totalSum.compareTo(currentLimit.getLimitAmount()) > 0;

            checkedOnLimit.setTransactionId(transaction.getId());
            checkedOnLimit.setLimitId(currentLimit.getId());
            checkedOnLimit.setLimitExceeded(isExceeded); // true if the transaction sum over limit.
            log.info("Transaction ID: {} processed. Limit exceeded: {}", transaction.getId(), checkedOnLimit.isLimitExceeded());
            return checkedOnLimit;
        } catch (Exception e) {
            log.error("Error checking limit status for transaction ID: {}. Exception: {}", transaction.getId(), e.getMessage(), e);
            throw new RuntimeException("TransactionService error occurred in the method checkLimitStatus" + e.getMessage(), e);
        }
    }

    public List<TransactionDTO> getTransactionsWithCalculatedSum(List<TransactionDTO> transactionsWithRates) {
        transactionsWithRates.stream()
                .filter(transaction -> transaction.getAmount() != null && transaction.getExchangeRate() != null) // filtering transactions
                .forEach(transaction -> { // loop transactions
                    BigDecimal calculatedSum = transaction.getAmount().multiply(transaction.getExchangeRate());
                    transaction.setSum(calculatedSum);
                    transaction.setConvertedCurrency("USD"); // setting currency USD
                });
        return transactionsWithRates;
    }

    public List<TransactionDTO> groupAndSummarizeTransactions(List<TransactionDTO> transactions) {
        return transactions.stream()
                // creating groups of transactions by the field `accountFrom`
                .collect(Collectors.groupingBy(transaction -> transaction.getAccountFrom()))
                .entrySet()
                .stream()
                .flatMap(accountGroup -> accountGroup.getValue().stream()
                        // now grouping on the field `expenseCategory`
                        .collect(Collectors.groupingBy(transaction -> transaction.getExpenseCategory(),
                                // Aggravating `sum` for each group on category
                                Collectors.reducing(BigDecimal.ZERO,
                                        // tacking sum and if null setting 0
                                        transaction -> transaction.getSum() != null ? transaction.getSum() : BigDecimal.ZERO,
                                        // calculating`sum` for each transaction in group
                                        (sum1, sum2) -> sum1.add(sum2))))
                        .entrySet()
                        .stream()
                        // Create a summarized TransactionDTO for each 'expenseCategory' in the 'accountFrom' group
                        .map(expenseGroup -> {
                            TransactionDTO summaryTransaction = new TransactionDTO(
                                    "USD",
                                    expenseGroup.getKey(),
                                    accountGroup.getKey(),
                                    BigDecimal.ZERO
                            );
                            //setting sum into summaryTransaction
                            summaryTransaction.setSum(expenseGroup.getValue());

                            return summaryTransaction;
                        })
                )
                // To list
                .collect(Collectors.toList());
    }

    //Gets transactions and rates
    public List<TransactionDTO> getTransactionsWithRates(UUID limitId) {
        List<TransactionProjection> projections = transactionRepository.getTransactionsWithRates(limitId);

        List<TransactionDTO> transactions = new ArrayList<>();

        for (TransactionProjection projection : projections) {
            TransactionDTO dto = new TransactionDTO();
            dto.setId(projection.getId());
            dto.setAmount(projection.getAmount());
            dto.setCurrency(projection.getCurrency());
            dto.setDatetimeTransaction(OffsetDateTime.ofInstant(projection.getDatetimeTransaction(), ZoneOffset.UTC)); // Преобразуем Instant в OffsetDateTime
            dto.setAccountFrom(projection.getAccountFrom());
            dto.setAccountTo(projection.getAccountTo());
            dto.setExpenseCategory(projection.getExpenseCategory());
            dto.setTrDate(OffsetDateTime.ofInstant(projection.getTrDate(), ZoneOffset.UTC)); // Преобразуем Instant в OffsetDateTime
            dto.setExchangeRate(projection.getExchangeRate());

            transactions.add(dto);
        }
        return transactions;
    }

    private void saveTransactionsToDB(List<TransactionDTO> transactionDTOListFromService) {
        try {
            transactionDTOListFromService.forEach(transactionDTO -> {
                transactionRepository.save(TransactionMapper.INSTANCE.toEntity(transactionDTO));
            });
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e.getMessage(), e);
        }
    }
}



