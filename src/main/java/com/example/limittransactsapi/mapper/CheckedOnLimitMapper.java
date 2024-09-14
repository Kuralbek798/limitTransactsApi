package com.example.limittransactsapi.mapper;


import com.example.limittransactsapi.DTO.CheckedOnLimitDTO;
import com.example.limittransactsapi.Entity.CheckedOnLimit;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
@Mapper
public interface CheckedOnLimitMapper {
    CheckedOnLimitMapper INSTANCE = Mappers.getMapper(CheckedOnLimitMapper.class);
     CheckedOnLimitDTO toDTO(CheckedOnLimit checkedOnLimit);
     CheckedOnLimit toEntity(CheckedOnLimitDTO checkedOnLimitDTO);

}
