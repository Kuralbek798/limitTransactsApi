package com.example.limittransactsapi.Helpers.mapper;

import com.example.limittransactsapi.Models.DTO.LimitDTO;
import com.example.limittransactsapi.Models.Entity.Limit;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper
public interface LimitMapper {
    LimitMapper INSTANCE = Mappers.getMapper(LimitMapper.class);

    @Mapping(target = "id", source = "id")
    LimitDTO toDTO(Limit limit);

    @Mapping(target = "id", ignore = true)
    Limit toEntity(LimitDTO limitDTO);

    List<LimitDTO> toDTO(List<Limit> limits);

    List<Limit> toEntity(List<LimitDTO> limitDTOs);
}