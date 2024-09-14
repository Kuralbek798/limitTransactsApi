package com.example.limittransactsapi.controllers;



import com.example.limittransactsapi.DTO.LimitDtoFromClient;
import com.example.limittransactsapi.services.LimitService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/limits")
public class ClientController {

    @Autowired
    private LimitService limitService;



    // Setting a new spending limit
    @PostMapping
    public ResponseEntity<LimitDtoFromClient> setLimit(@RequestBody LimitDtoFromClient limit) {

        return limitService.setLimit(limit);
    }
/*

    // Getting all limits
    @GetMapping("/limits")
    public ResponseEntity<List<Limit>> getAllLimits() {
        List<Limit> limits = limitService.getAllLimits();
        return ResponseEntity.ok(limits);
    }

    // Getting transactions that exceed the limit
    @GetMapping("/transactions/exceeding")
    public ResponseEntity<List<Transaction>> getTransactionsExceedingLimit() {
        List<Transaction> transactions = transactionService.getTransactionsExceedingLimit();
        return ResponseEntity.ok(transactions);
    }
*/

}
