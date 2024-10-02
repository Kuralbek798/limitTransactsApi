package com.example.limittransactsapi.controllers;




import com.example.limittransactsapi.Models.DTO.TransactionDTO;
import com.example.limittransactsapi.services.ExchangeRateService;
import com.example.limittransactsapi.services.TransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
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


    @PostMapping("/process")
    public CompletableFuture<ResponseEntity<String>> setTransactions(@RequestBody List<TransactionDTO> transactions) {
        // Directly delegate the processing to the service
        var a = transactionService.categorizeAndPrepareClientsTransactionsAsync(transactions);
        return a;
    }


}
