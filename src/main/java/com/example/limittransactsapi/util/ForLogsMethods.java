package com.example.limittransactsapi.util;

import com.example.limittransactsapi.Models.DTO.TransactionDTO;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

public class ForLogsMethods
{



    // Вспомогательный метод для составления описания comparerExamplesDB
    public static String describeComparerExamplesDB(Map<Integer, BigDecimal> comparerExamplesDB) {
        if (comparerExamplesDB == null) {
            return "null";
        }
        return comparerExamplesDB.entrySet().stream()
                .map(entry -> entry.getKey() + ": " + entry.getValue())
                .collect(Collectors.joining(", ", "{", "}"));
    }


    public static String describeClientsTransactions(Map<Integer, ConcurrentLinkedQueue<TransactionDTO>> clientsTransactions) {
        if (clientsTransactions == null) {
            return "null";
        }
        return clientsTransactions.entrySet().stream()
                .map(entry -> entry.getKey() + " has [" + entry.getValue().size() + " transactions: " +
                        entry.getValue().stream().map(TransactionDTO::toString).collect(Collectors.joining(", ")) + "]")
                .collect(Collectors.joining(", ", "{", "}"));
    }


}
