package com.example.limittransactsapi.controllers;



import com.example.limittransactsapi.DTO.ExchangeRateDTO;
import com.example.limittransactsapi.DTO.LimitDTO;
import com.example.limittransactsapi.DTO.TransactionDTO;

import com.example.limittransactsapi.services.ExchangeRateService;
import com.example.limittransactsapi.services.TransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/transactions")
public class TransactionController {

    private final TransactionService transactionService;
    private final ExchangeRateService exchangeRateService;
    @Autowired
    public TransactionController(TransactionService transactionService, ExchangeRateService exchangeRateService) {
        this.transactionService = transactionService;
        this.exchangeRateService = exchangeRateService;
    }


    @PostMapping
/*    public ResponseEntity<TransactionDTO> setTransactions(@RequestBody TransactionDTO transactionDTO) {
        return transactionService.createTransaction(transactionDTO);
    }*/

//Don't forget to delete for testing purpose only!!!!!!!
    @GetMapping("/currancy")
    public CompletableFuture<ExchangeRateDTO> getExchangeRate(String currencyPair){
       var temp = exchangeRateService.getCurrencyRate(currencyPair);
       return temp;
    }

    //Don't forget to delete for testing purpose only!!!!!!!
    @GetMapping("/{limitId}")
    public CompletableFuture<List<TransactionDTO>> getTransactions(@PathVariable LimitDTO limitDTO) {
       var a = transactionService.getTransactionsWithRates(limitDTO);
       //var b = transactionService.getTransactionsWithCalculatedSum(a);

        //return transactionService.groupAndSummarizeTransactions(b);
        return a;
    }


    @PostMapping("/process")
    public CompletableFuture<ResponseEntity<String>> setTransactions(@RequestBody List<TransactionDTO> transactions) {
        // Directly delegate the processing to the service
        var a = transactionService.setTransactionsToDB(transactions);
        return a;
    }


}
