package com.example.limittransactsapi.mapper;

import com.example.limittransactsapi.DTO.ExchangeRateDTO;
import com.example.limittransactsapi.Entity.ExchangeRate;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper
public interface ExchangeRateMapper {

    ExchangeRateMapper INSTANCE = Mappers.getMapper(ExchangeRateMapper.class);

    // Mapping fields explicitly where necessary
    @Mapping(source = "dateTimeRate", target = "dateTimeRate")
    ExchangeRateDTO toDTO(ExchangeRate exchangeRate);

    @Mapping(source = "dateTimeRate", target = "dateTimeRate")
    ExchangeRate toEntity(ExchangeRateDTO exchangeRateDTO);
}