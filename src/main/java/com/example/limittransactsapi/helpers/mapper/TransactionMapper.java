package com.example.limittransactsapi.helpers.mapper;


import com.example.limittransactsapi.models.DTO.TransactionDTO;
import com.example.limittransactsapi.models.entity.Transaction;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper
public interface TransactionMapper {
    TransactionMapper INSTANCE = Mappers.getMapper(TransactionMapper.class);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "sum", source = "sum")
    @Mapping(target = "currency", source = "currency")
    @Mapping(target = "datetimeTransaction", source = "datetimeTransaction")
    @Mapping(target = "accountFrom", source = "accountFrom")
    @Mapping(target = "accountTo", source = "accountTo")
    @Mapping(target = "expenseCategory", source = "expenseCategory")
    @Mapping(target = "trDate", ignore = true)
    @Mapping(target = "exchangeRate", ignore = true)
    @Mapping(target = "convertedSum", ignore = true)
    @Mapping(target = "convertedCurrency", ignore = true)
    @Mapping(target = "limitExceeded", ignore = true)
    TransactionDTO toDTO(Transaction transaction);


    @Mapping(target = "id", ignore = true)
    @Mapping(target = "sum", source = "sum")
    @Mapping(target = "currency", source = "currency")
    @Mapping(target = "datetimeTransaction", source = "datetimeTransaction")
    @Mapping(target = "accountFrom", source = "accountFrom")
    @Mapping(target = "accountTo", source = "accountTo")
    @Mapping(target = "expenseCategory", source = "expenseCategory")

    Transaction toEntity(TransactionDTO transactionDTO);
}