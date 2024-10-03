package com.example.limittransactsapi.helpers.mapper;


import com.example.limittransactsapi.models.DTO.CheckedOnLimitDTO;
import com.example.limittransactsapi.models.entity.CheckedOnLimit;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper
public interface CheckedOnLimitMapper {

    CheckedOnLimitMapper INSTANCE = Mappers.getMapper(CheckedOnLimitMapper.class);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "transactionId", source = "transactionId")
    @Mapping(target = "limitId", source = "limitId")
    @Mapping(target = "limitExceeded", source = "limitExceeded")
    @Mapping(target = "datetime", source = "datetime")
    CheckedOnLimitDTO toDTO(CheckedOnLimit CheckedOnLimit);

    @Mapping(target = "id", ignore = true)
    @Mapping(source = "transactionId", target = "transactionId")
    @Mapping(source = "limitId", target = "limitId")
    @Mapping(source = "limitExceeded", target = "limitExceeded")
    CheckedOnLimit toEntity(CheckedOnLimitDTO checkedOnLimitDTO);
}
