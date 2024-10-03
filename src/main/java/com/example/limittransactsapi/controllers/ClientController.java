package com.example.limittransactsapi.controllers;


import com.example.limittransactsapi.models.DTO.*;
import com.example.limittransactsapi.models.DTO.LimitDtoFromClient;
import com.example.limittransactsapi.services.CheckedOnLimitService;
import com.example.limittransactsapi.services.LimitService;
import com.example.limittransactsapi.services.ShedullerService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/limits")

public class ClientController {


    private final LimitService limitService;
    private final CheckedOnLimitService checkedOnLimitService;
    private final ShedullerService shedullerService;

    @Autowired
    public ClientController(LimitService limitService, CheckedOnLimitService checkedOnLimitService, ShedullerService shedullerService) {
        this.limitService = limitService;
        this.checkedOnLimitService = checkedOnLimitService;
        this.shedullerService = shedullerService;
    }

    // Setting a new spending limit
    @PostMapping("/setNewLimit")
    public CompletableFuture<ResponseEntity<String>> setLimit(@Valid @RequestBody LimitDtoFromClient limit) {

        if (limit.getLimitSum().scale() < 2) {
            return CompletableFuture.completedFuture(
                    ResponseEntity.badRequest().body("Limit sum must be type of Big decimal and have two decimal places."));
        }

        return limitService.setLimitAsync(limit)
                .exceptionally(ex -> {
                    ex.printStackTrace();
                    // Check type of exception and return appropriate response
                    if (ex.getCause() instanceof BindingResult) {
                        BindingResult result = (BindingResult) ex.getCause();
                        for (ObjectError error : result.getAllErrors()) {
                            // Логируем все ошибки валидации
                            System.out.println("Validation error: " + error.getDefaultMessage());
                        }
                    }
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
                });

    }

    @GetMapping("/exceededTransactions")
    public List<TransactionLimitDTO> getReports() {
        return checkedOnLimitService.getExceededLimitsTransactions();
    }


    @GetMapping("all")
    public List<LimitDTO> getAlLimits() {
        return limitService.getAllLimits();
    }

}

