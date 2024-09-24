package com.example.limittransactsapi.mapper;

import com.example.limittransactsapi.DTO.TransactionDTO;
import com.example.limittransactsapi.Entity.Transaction;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper
public interface TransactionMapper {
    TransactionMapper INSTANCE = Mappers.getMapper(TransactionMapper.class);

    // Map entity to DTO, ignoring fields that are not present in the entity
    @Mapping(target = "id", source = "id")
    @Mapping(target = "sum", source = "sum")
    @Mapping(target = "currency", source = "currency")
    @Mapping(target = "datetimeTransaction", source = "datetimeTransaction")
    @Mapping(target = "accountFrom", source = "accountFrom")
    @Mapping(target = "accountTo", source = "accountTo")
    @Mapping(target = "expenseCategory", source = "expenseCategory")
    @Mapping(target = "trDate", ignore = true)  // Ignored as it is not present in the entity
    @Mapping(target = "exchangeRate", ignore = true) // Ignored as it is not present in the entity
    @Mapping(target = "convertedSum", ignore = true) // Ignored as it is not present in the entity
    @Mapping(target = "convertedCurrency", ignore = true) // Ignored as it is not present in the entity
    @Mapping(target = "limitExceeded", ignore = true) // Ignored as it is not present in the entity
    TransactionDTO toDTO(Transaction transaction);

    // Map DTO to entity, ignoring fields that are not present in the DTO
    @Mapping(target = "id", source = "id")
    @Mapping(target = "sum", source = "sum")
    @Mapping(target = "currency", source = "currency")
    @Mapping(target = "datetimeTransaction", source = "datetimeTransaction")
    @Mapping(target = "accountFrom", source = "accountFrom")
    @Mapping(target = "accountTo", source = "accountTo")
    @Mapping(target = "expenseCategory", source = "expenseCategory")
    // Fields that are not present in the DTO and should be ignored during mapping
    Transaction toEntity(TransactionDTO transactionDTO);
}