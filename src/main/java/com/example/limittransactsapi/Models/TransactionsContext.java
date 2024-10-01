package com.example.limittransactsapi.Models;

import com.example.limittransactsapi.Models.DTO.TransactionDTO;
import lombok.Getter;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListSet;

@Getter
public class TransactionsContext {
    private final ConcurrentSkipListSet<Integer> transactionsAccounts ;
    private final ConcurrentHashMap<String, ConcurrentHashMap<Integer, ConcurrentLinkedQueue<TransactionDTO>>> transactionsMap;

    public TransactionsContext() {
        transactionsAccounts = new ConcurrentSkipListSet<>();
        transactionsMap = new ConcurrentHashMap<>();
    }
}
