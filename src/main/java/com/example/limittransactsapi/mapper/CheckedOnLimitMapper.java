package com.example.limittransactsapi.mapper;

import com.example.limittransactsapi.DTO.CheckedOnLimitDTO;
import com.example.limittransactsapi.DTO.TransactionDTO;
import com.example.limittransactsapi.Entity.CheckedOnLimit;
import com.example.limittransactsapi.Entity.Transaction;
import org.mapstruct.factory.Mappers;

public interface CheckedOnLimitMapper {

    CheckedOnLimitMapper INSTANCE = Mappers.getMapper(CheckedOnLimitMapper.class);
     CheckedOnLimitDTO toDTO(CheckedOnLimit checkedOnLimit);
     CheckedOnLimit toEntity(CheckedOnLimitDTO checkedOnLimitDTO);

}
