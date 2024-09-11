package com.example.limittransactsapi.mapper;

import com.example.limittransactsapi.DTO.ExchangeRateDTO;
import com.example.limittransactsapi.Entity.ExchangeRate;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface ExchangeRateMapper {
    ExchangeRateMapper INSTANCE = Mappers.getMapper(ExchangeRateMapper.class);
    ExchangeRateDTO toDTO(ExchangeRate exchangeRate);
    ExchangeRate toEntity(ExchangeRateDTO exchangeRateDTO);

}
