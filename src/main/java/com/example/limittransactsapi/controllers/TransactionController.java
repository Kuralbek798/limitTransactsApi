package com.example.limittransactsapi.controllers;



import com.example.limittransactsapi.DTO.ExchangeRateDTO;
import com.example.limittransactsapi.DTO.TransactionDTO;

import com.example.limittransactsapi.services.ExchangeRateService;
import com.example.limittransactsapi.services.TransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
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
        return exchangeRateService.getCurrencyRate(currencyPair);
    }

    //Don't forget to delete for testing purpose only!!!!!!!
    @GetMapping("/{limitId}")
    public CompletableFuture<List<TransactionDTO>> getTransactions(@PathVariable UUID limitId) {
       var a = transactionService.getTransactionsWithRates(limitId);
       //var b = transactionService.getTransactionsWithCalculatedSum(a);

        //return transactionService.groupAndSummarizeTransactions(b);
        return a;
    }

}
