package com.example.limittransactsapi.mapper;

import com.example.limittransactsapi.DTO.LimitDTO;
import com.example.limittransactsapi.Entity.Limit;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper
public interface LimitMapper {
    LimitMapper INSTANCE = Mappers.getMapper(LimitMapper.class);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "limitSum", source = "limitSum")
    @Mapping(target = "currency", source = "currency")
    @Mapping(target = "datetime", source = "datetime")
    LimitDTO toDTO(Limit limit);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "limitSum", source = "limitSum")
    @Mapping(target = "currency", source = "currency")
    @Mapping(target = "datetime", source = "datetime")
    Limit toEntity(LimitDTO limitDTO);
}