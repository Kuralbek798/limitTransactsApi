package com.example.limittransactsapi.helpers.mapper;


import com.example.limittransactsapi.models.DTO.ExchangeRateDTO;
import com.example.limittransactsapi.models.entity.ExchangeRate;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper
public interface ExchangeRateMapper {

    ExchangeRateMapper INSTANCE = Mappers.getMapper(ExchangeRateMapper.class);

    @Mapping(target = "currencyPair", source = "currencyPair")
    @Mapping(target = "rate", source = "rate")
    @Mapping(target = "close", source = "close")
    @Mapping(target = "dateTimeRate", source = "dateTimeRate")
    ExchangeRateDTO toDTO(ExchangeRate exchangeRate);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "currencyPair", source = "currencyPair")
    @Mapping(target = "rate", source = "rate")
    @Mapping(target = "close", source = "close")
    @Mapping(target = "dateTimeRate", source = "dateTimeRate")
    ExchangeRate toEntity(ExchangeRateDTO exchangeRateDTO);
}