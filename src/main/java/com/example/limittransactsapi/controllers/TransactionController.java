package com.example.limittransactsapi.controllers;


import com.example.limittransactsapi.DTO.ExchangeRateDTO;
import com.example.limittransactsapi.DTO.TransactionDTO;
import com.example.limittransactsapi.Entity.Transaction;
import com.example.limittransactsapi.services.CurrencyService;
import com.example.limittransactsapi.services.TransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<TransactionDTO> setTransactions(@RequestBody TransactionDTO transactionDTO) {
        return transactionService.createTransaction(transactionDTO);
    }

//Don't forget to delete for testing purpose only!!!!!!!
    @GetMapping("/currancy")
    public ExchangeRateDTO getExchangeRate(String currencyPair){
        return currencyService.getCurrencyRate(currencyPair);
    }

}
