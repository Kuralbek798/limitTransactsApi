/*
package com.example.limittransactsapi.mapper;

import com.example.limittransactsapi.DTO.LimitDtoFromClient;
import com.example.limittransactsapi.Entity.Limit;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper
public interface ClientLimitMapper {

    ClientLimitMapper INSTANCE = Mappers.getMapper(ClientLimitMapper.class);

    @Mapping(source = "limitSum", target = "limitSum")
    @Mapping(source = "currency", target = "currency")
    LimitDtoFromClient toDTO(Limit limit);

    @Mapping(source = "limitSum", target = "limitSum")
    @Mapping(source = "currency", target = "currency")
    Limit toEntity(LimitDtoFromClient limitDTO);
}*/
