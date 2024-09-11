package com.example.limittransactsapi.mapper;

import com.example.limittransactsapi.DTO.LimitDTO;
import com.example.limittransactsapi.Entity.Limit;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Mapper
public interface LimitMapper {
    LimitMapper INSTANCE = Mappers.getMapper(LimitMapper.class);
    LimitDTO toDTO(Limit limit);
    Limit toEntity(LimitDTO limitDTO);

}
