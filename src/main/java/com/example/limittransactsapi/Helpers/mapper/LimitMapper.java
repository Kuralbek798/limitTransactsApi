package com.example.limittransactsapi.Helpers.mapper;


import com.example.limittransactsapi.Models.DTO.LimitDTO;
import com.example.limittransactsapi.Models.Entity.Limit;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper
public interface LimitMapper {
    LimitMapper INSTANCE = Mappers.getMapper(LimitMapper.class);


    LimitDTO toDTO(Limit limit);

    @Mapping(target = "id", ignore = true)
    Limit toEntity(LimitDTO limitDTO);
}