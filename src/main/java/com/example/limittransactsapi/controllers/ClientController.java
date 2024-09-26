package com.example.limittransactsapi.controllers;



import com.example.limittransactsapi.DTO.LimitDTO;
import com.example.limittransactsapi.DTO.LimitDtoFromClient;
import com.example.limittransactsapi.services.LimitService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/limits")
public class ClientController {

    @Autowired
    private LimitService limitService;



    // Setting a new spending limit
    @PostMapping
    public CompletableFuture<ResponseEntity<LimitDtoFromClient>> setLimit(@Valid @RequestBody LimitDtoFromClient limit) {
        return limitService.setLimitAsync(limit)
                .exceptionally(ex -> {
                    // Log the exception details
                    ex.printStackTrace();
                    // Check type of exception and return appropriate response
                    if (ex.getCause() instanceof IllegalArgumentException) {
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
                    } else {
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
                    }
                });
    }

 @GetMapping("/limit")
    public CompletableFuture<LimitDTO> getLimit(String s) {

     CompletableFuture<LimitDTO> n;
     if(s.equals("s")){

        //  n = limitService.getLatestLimitAsync();
          n = limitService.getLatestLimitAsync();
         return n ;
     }else {
         return null;
     }


 }
}
