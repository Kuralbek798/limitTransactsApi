package com.example.limittransactsapi.controllers;


import com.example.limittransactsapi.Models.DTO.*;
import com.example.limittransactsapi.Models.DTO.LimitDtoFromClient;
import com.example.limittransactsapi.services.CheckedOnLimitService;
import com.example.limittransactsapi.services.LimitService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/limits")

public class ClientController {


    private LimitService limitService;
    private CheckedOnLimitService checkedOnLimitService;

    @Autowired
    public ClientController(LimitService limitService, CheckedOnLimitService checkedOnLimitService) {
        this.limitService = limitService;
        this.checkedOnLimitService = checkedOnLimitService;
    }


    // Setting a new spending limit
    @PostMapping
    public CompletableFuture<ResponseEntity<String>> setLimit(@Valid @RequestBody LimitDtoFromClient limit) {

        if (limit.getLimitSum().scale() < 2) {
            return CompletableFuture.completedFuture(
                    ResponseEntity.badRequest().body("Limit sum must be type of Big decimal and have two decimal places."));
        }

                var a =   limitService.setLimitAsync(limit);
                        return a
                                .exceptionally(ex -> {
                                    // Log the exception details
                                    ex.printStackTrace(); // Печать трассировки стека
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

    @GetMapping("/ExceededLimits")
    public List<TransactionLimitDTO> getReports() {
        return checkedOnLimitService.getExceededLimitsTransactions();
    }


    @GetMapping("TestService")
    public List<LimitAccountDTO>testMethod(){
     // limitService.updateStatusIsActive();
      //  limitService.setMonthlyLimitByDefault();
        //var a = limitService.getLimitById(id);
        var a = limitService.getAllActiveLimits();
      return   a;
    }
}

