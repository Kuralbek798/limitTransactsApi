package com.example.limittransactsapi.controllers;



import com.example.limittransactsapi.DTO.LimitDTO;
import com.example.limittransactsapi.services.LimitService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/limits")
public class LimitController {

    @Autowired
    private LimitService limitService;

    @PostMapping
    public ResponseEntity<LimitDTO> setLimit(@RequestBody LimitDTO limit) {
        return limitService.setLimit(limit);
    }

}
