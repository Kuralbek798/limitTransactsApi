package com.example.limittransactsapi.mapper;

import com.example.limittransactsapi.DTO.TransactionDTO;
import com.example.limittransactsapi.Entity.Transaction;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface TransactionMapper {
    TransactionMapper INSTANCE = Mappers.getMapper(TransactionMapper.class);
    TransactionDTO toDTO(Transaction transaction);
    Transaction toEntity(TransactionDTO transactionDTO);

}
