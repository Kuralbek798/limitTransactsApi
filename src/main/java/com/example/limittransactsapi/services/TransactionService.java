package com.example.limittransactsapi.services;


import com.example.limittransactsapi.DTO.ExchangeRateDTO;
import com.example.limittransactsapi.DTO.LimitDTO;
import com.example.limittransactsapi.DTO.TransactionDTO;
import com.example.limittransactsapi.Entity.CheckedOnLimit;
import com.example.limittransactsapi.Entity.ExchangeRate;


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
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class TransactionService {


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
    public ResponseEntity sendTransactionsToDB(List<TransactionDTO> transactionDTOListFromService) {
        try {
            //receiving current limit for  category.
            Optional<LimitDTO> optionalCurrentLimitDto = limitService.getLatestLimitInOptionalLimitDto();
            if (optionalCurrentLimitDto.isPresent()) {
                //receiving transactions and rates by limit id with limit_exceeded false
               var transactionsWithRates = getTransactionsWithRates(optionalCurrentLimitDto.get().getId());
                //calculate sum on rate and amount
                transactionsWithRates = getTransactionsWithCalculatedSum(transactionsWithRates);
                //create list by group accountFrom and expense
                transactionsWithRates = groupAndSummarizeTransactions(transactionsWithRates);

                checkTransactionsOnActiveLimit(transactionDTOListFromService,transactionsWithRates,optionalCurrentLimitDto);
            }
            saveTransactionsToDB(transactionDTOListFromService);
            return ResponseEntity.ok().build();

        } catch (DataIntegrityViolationException e) {
            log.error("Ошибка при сохранении в БД: {}", e.getMessage(), e);
            throw new DataIntegrityViolationException("Ошибка при сохранении транзакции: " + e.getMessage(), e);
        } catch (DataAccessException e) {
            log.error("Ошибка при доступе к БД: {}", e.getMessage(), e);
            throw new DataAccessException("Ошибка при сохранении транзакции: " + e.getMessage(), e) {
            };
        }
    }
private void saveTransactionsToDB(List<TransactionDTO> transactionDTOListFromService) {
        try{
            transactionDTOListFromService.forEach(transactionDTO -> {
                transactionRepository.save(TransactionMapper.INSTANCE.toEntity(transactionDTO));
            });
        }catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e.getMessage(), e);
        }
}
    public void checkTransactionsOnActiveLimit(List<TransactionDTO> transactionDTOListFromService,
                                               List<TransactionDTO> listTransactionsWithRates,
                                               Optional<LimitDTO> optionalCurrentLimitDto) {

        if (optionalCurrentLimitDto.isPresent()) {
            LimitDTO currentLimitDto = optionalCurrentLimitDto.get();

            for (TransactionDTO transaction : transactionDTOListFromService) {
                try {
                    // Initialize the currency rate
                    ExchangeRateDTO exchangeRateDTO = getCurrencyRateDTO(transaction);

                    if (exchangeRateDTO != null) {
                        log.info("Converting transaction sum for transaction ID: {}", transaction.getId());

                        // Convert the transaction sum to USD using the latest currency rate
                        BigDecimal convertedSumOfTransaction = converterUtil.currencyConverter(
                                transaction.getSum(), exchangeRateDTO.getRate());

                        // Check the transaction based on the expense category
                        if ("product".equals(transaction.getExpenseCategory()) ||
                                "service".equals(transaction.getExpenseCategory())) {

                            BigDecimal additionalSum = calculateAdditionalSum(listTransactionsWithRates, transaction.getExpenseCategory());
                            convertedSumOfTransaction = convertedSumOfTransaction.add(additionalSum);

                            checkAndSaveLimitStatus(transaction, currentLimitDto, convertedSumOfTransaction);
                        }
                    }
                } catch (Exception e) {
                    log.error("Error processing transaction ID: {}. Exception: {}", transaction.getId(), e.getMessage(), e);
                }
            }
        } else {
            log.warn("No current limit available for processing transactions.");
        }
    }

    private ExchangeRateDTO getCurrencyRateDTO(TransactionDTO transaction) {
        try {
            if ("KZT".equals(transaction.getCurrency())) {
                return exchangeRateService.getCurrencyRate("KZT/USD");
            } else if ("RUB".equals(transaction.getCurrency())) {
                return exchangeRateService.getCurrencyRate("RUB/USD");
            }
        } catch (Exception e) {
            log.error("Failed to get currency rate for currency: {}. Exception: {}", transaction.getCurrency(), e.getMessage(), e);
        }
        return null;
    }

    private BigDecimal calculateAdditionalSum(List<TransactionDTO> listTransactionsWithRates, String expenseCategory) {
        try {
            return listTransactionsWithRates.stream()
                    .filter(transactionWithRate -> transactionWithRate.getExpenseCategory().equals(expenseCategory))
                    .map(TransactionDTO::getSum)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        } catch (Exception e) {
            log.error("Error calculating additional sum for category: {}. Exception: {}", expenseCategory, e.getMessage(), e);
            return BigDecimal.ZERO;  // Return zero if sum calculation fails
        }
    }

    private void checkAndSaveLimitStatus(TransactionDTO transaction, LimitDTO currentLimitDto, BigDecimal convertedSum) {
        try {
            int compareResult = currentLimitDto.getLimitAmount().compareTo(convertedSum);

            CheckedOnLimit checkedOnLimit = new CheckedOnLimit();
            checkedOnLimit.setTransactionId(transaction.getId());
            checkedOnLimit.setLimitId(currentLimitDto.getId());
            checkedOnLimit.setLimitExceeded(compareResult < 0);

            checkedOnLimitService.saveCheckedOnLimit(checkedOnLimit);

            log.info("Transaction ID: {} processed. Limit exceeded: {}", transaction.getId(), checkedOnLimit.isLimitExceeded());

        } catch (Exception e) {
            log.error("Error checking limit status for transaction ID: {}. Exception: {}", transaction.getId(), e.getMessage(), e);
        }
    }

    public List<TransactionDTO> getTransactionsWithCalculatedSum(List<TransactionDTO> transactionsWithRates) {
       transactionsWithRates.stream()
                .filter(transaction -> transaction.getAmount() != null && transaction.getExchangeRate() != null) // filtering transactions
                .forEach(transaction -> { // loop transactions
                    BigDecimal calculatedSum = transaction.getAmount().multiply(transaction.getExchangeRate());
                    transaction.setSum(calculatedSum);
                    transaction.setCurrency("USD"); // setting currency USD
                });
        return transactionsWithRates;
    }

    public List<TransactionDTO> groupAndSummarizeTransactions(List<TransactionDTO> transactions) {
        return transactions.stream()
                // Группируем список транзакций по полю `accountFrom`
                .collect(Collectors.groupingBy(transaction -> transaction.getAccountFrom()))
                .entrySet()
                .stream()
                // Для каждой группы, полученной на предыдущем шаге, обрабатываем дальнейшие действия
                .flatMap(accountGroup -> accountGroup.getValue().stream()
                        // Для каждой группы также группируем по полю `expenseCategory`
                        .collect(Collectors.groupingBy(transaction -> transaction.getExpenseCategory(),
                                // Агрегируем `sum` для каждой подгруппы по категории
                                Collectors.reducing(BigDecimal.ZERO,
                                        // Извлекаем сумму транзакции и устанавливаем ее в 0, если она равна null
                                        transaction -> transaction.getSum() != null ? transaction.getSum() : BigDecimal.ZERO,
                                        // Суммируем значения `sum` для каждой транзакции в подгруппе
                                        (sum1, sum2) -> sum1.add(sum2))))
                        .entrySet()
                        .stream()
                        // Преобразуем каждую подгруппу в новый объект TransactionDTO
                        .map(expenseGroup -> {
                            // Создаем новый TransactionDTO с только необходимой информацией
                            TransactionDTO summaryTransaction = new TransactionDTO(
                                    "USD",
                                    expenseGroup.getKey(),
                                    accountGroup.getKey(),
                                    BigDecimal.ZERO
                            );
                            // Устанавливаем суммированное значение в sum
                            summaryTransaction.setSum(expenseGroup.getValue());

                            return summaryTransaction;
                        })
                )
                // Собираем все преобразованные группы обратно в список транзакций
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

}



