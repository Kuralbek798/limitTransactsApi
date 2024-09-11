package com.example.limittransactsapi.controllers;


import com.example.limittransactsapi.DTO.ExchangeRateDTO;
import com.example.limittransactsapi.Entity.Transaction;
import com.example.limittransactsapi.Model.RateDataFromJson;
import com.example.limittransactsapi.services.CurrencyService;
import com.example.limittransactsapi.services.TransactionService;
import io.swagger.v3.core.util.Json;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/transactions")
public class TransactionController {

    private final TransactionService transactionService;
    private final CurrencyService currencyService;
    @Autowired
    public TransactionController(TransactionService transactionService, CurrencyService currencyService) {
        this.transactionService = transactionService;
        this.currencyService = currencyService;
    }


    @PostMapping
    public Transaction createTransaction(@RequestBody Transaction transaction) {
        return transactionService.createTransaction(transaction);
    }
    @GetMapping("/exceeded")
    public List<Transaction> getExceededTransactions() {
        return transactionService.getExceedingTransactions();
    }

    @GetMapping("/currancy")
    public ExchangeRateDTO getExchangeRate(String currencyPair){
        return currencyService.getCurrencyRate(currencyPair);
    }

}
