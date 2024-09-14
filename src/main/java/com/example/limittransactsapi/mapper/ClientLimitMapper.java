package com.example.limittransactsapi.mapper;


import com.example.limittransactsapi.DTO.LimitDtoFromClient;
import com.example.limittransactsapi.Entity.Limit;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface ClientLimitMapper {


    ClientLimitMapper INSTANCE = Mappers.getMapper(ClientLimitMapper.class);
        LimitDtoFromClient toDTO(Limit limit);
        Limit toEntity(LimitDtoFromClient limitDTO);



}
