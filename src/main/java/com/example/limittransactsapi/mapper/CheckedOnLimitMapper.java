package com.example.limittransactsapi.mapper;

import com.example.limittransactsapi.DTO.CheckedOnLimitDTO;
import com.example.limittransactsapi.Entity.CheckedOnLimit;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper
public interface CheckedOnLimitMapper {

    CheckedOnLimitMapper INSTANCE = Mappers.getMapper(CheckedOnLimitMapper.class);

    @Mapping(source = "id", target = "id")
    @Mapping(source = "transactionId", target = "transactionId")
    @Mapping(source = "limitId", target = "limitId")
    @Mapping(source = "limitExceeded", target = "limitExceeded")
    @Mapping(source = "datetime", target = "datetime")
    CheckedOnLimitDTO toDTO(CheckedOnLimit checkedOnLimit);

    @Mapping(source = "id", target = "id")
    @Mapping(source = "transactionId", target = "transactionId")
    @Mapping(source = "limitId", target = "limitId")
    @Mapping(source = "limitExceeded", target = "limitExceeded")
    @Mapping(source = "datetime", target = "datetime")
    CheckedOnLimit toEntity(CheckedOnLimitDTO checkedOnLimitDTO);
}