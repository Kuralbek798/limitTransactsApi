package com.example.limittransactsapi.services;

import com.example.limittransactsapi.models.DTO.CheckedOnLimitDTO;
import com.example.limittransactsapi.models.DTO.TransactionLimitDTO;
import com.example.limittransactsapi.models.entity.CheckedOnLimit;
import com.example.limittransactsapi.helpers.exceptions.CustomDataAccessException;
import com.example.limittransactsapi.helpers.exceptions.CustomGenericException;
import com.example.limittransactsapi.helpers.mapper.CheckedOnLimitMapper;
import com.example.limittransactsapi.repository.CheckedOnLimitRepository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Slf4j
@Service
public class CheckedOnLimitService {

    private final CheckedOnLimitRepository checkedOnLimitRepository;
    private final Executor customExecutor;


    @Autowired
    public CheckedOnLimitService(CheckedOnLimitRepository checkedOnLimitRepository, @Qualifier("customExecutor") Executor customExecutor) {
        this.checkedOnLimitRepository = checkedOnLimitRepository;
        this.customExecutor = customExecutor;
    }

    public List<TransactionLimitDTO> getExceededLimitsTransactions() {
        return checkedOnLimitRepository.findExceededLimits();
    }

    @Transactional
    @Async("customExecutor")
    // Save a record
    public CompletableFuture<CheckedOnLimitDTO> saveCheckedOnLimitAsync(CheckedOnLimit entity) {
        if (entity == null) {
            log.error("Attempted to save a null entity"); // Проверка на null-сущность
            return CompletableFuture.failedFuture(new IllegalArgumentException("Entity cannot be null")); // Возвращаем завершенный с ошибкой Future
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Saving CheckedOnLimit entity:{}, isLimitExceeded {}", entity.getTransactionId().toString(),entity.isLimitExceeded());
                var savedEntity = checkedOnLimitRepository.save(entity);
                log.info("CheckedOnLimit entity saved: {} isLimitExceeded {}", savedEntity.getId().toString(),savedEntity.isLimitExceeded());
                return CheckedOnLimitMapper.INSTANCE.toDTO(savedEntity);
            } catch (DataAccessException dae) {
                log.error("Data access error while saving CheckedOnLimit: {}", dae.getMessage());
                throw new CustomDataAccessException("Ошибка доступа к данным при сохранении CheckedOnLimit", dae);
            } catch (Exception e) {
                log.error("An error occurred while saving CheckedOnLimit: {}", e.getMessage());
                throw new CustomGenericException("Ошибка при сохранении CheckedOnLimit", e);
            }
        });
    }

}