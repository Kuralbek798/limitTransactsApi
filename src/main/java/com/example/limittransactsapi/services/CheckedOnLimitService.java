package com.example.limittransactsapi.services;

import com.example.limittransactsapi.DTO.CheckedOnLimitDTO;
import com.example.limittransactsapi.Entity.CheckedOnLimit;
import com.example.limittransactsapi.mapper.CheckedOnLimitMapper;
import com.example.limittransactsapi.repository.CheckedOnLimitRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service

public class CheckedOnLimitService {

    private final CheckedOnLimitRepository checkedOnLimitRepository;

    @Autowired
    public CheckedOnLimitService(CheckedOnLimitRepository checkedOnLimitRepository) {
        this.checkedOnLimitRepository = checkedOnLimitRepository;
    }

    // Retrieve all records
    public List<CheckedOnLimitDTO> findAll() {
        var entities = checkedOnLimitRepository.findAll();
        return entities.stream().map(element -> CheckedOnLimitMapper.INSTANCE.toDTO(element)).collect(Collectors.toList());
    }

    // Find a record by ID
    public Optional<CheckedOnLimitDTO> findById(UUID id) {
        var checkedOnLimit = checkedOnLimitRepository.findById(id);
        return checkedOnLimit.map(element -> CheckedOnLimitMapper.INSTANCE.toDTO(element));
    }

    public List<CheckedOnLimitDTO> getCheckedOnLimitDTOsByLimitId(UUID limitId) {
        return checkedOnLimitRepository.findAllByLimitId(limitId)
                .stream()
                .map(element -> CheckedOnLimitMapper.INSTANCE.toDTO(element))
                .collect(Collectors.toList());
    }


    // Save a record
    public CheckedOnLimitDTO saveCheckedOnLimit(CheckedOnLimit entity) {
        try {
            var savedEntity = checkedOnLimitRepository.save(entity);
            log.info("checkedOnLimit entity saved: {}", savedEntity.getId().toString());
            return CheckedOnLimitMapper.INSTANCE.toDTO(savedEntity);
        } catch (Exception e) {
            log.error("An error occurred while saving CheckedOnLimit:{}", e.getMessage());
            throw new RuntimeException("Ошибка при сохранении CheckedOnLimit", e);
        }
    }

    // Delete a record by ID
    public void deleteById(UUID id) {
        checkedOnLimitRepository.deleteById(id);
    }

}