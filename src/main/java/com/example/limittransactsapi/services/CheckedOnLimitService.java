package com.example.limittransactsapi.services;

import com.example.limittransactsapi.DTO.CheckedOnLimitDTO;
import com.example.limittransactsapi.DTO.TransactionLimitDTO;
import com.example.limittransactsapi.Entity.CheckedOnLimit;
import com.example.limittransactsapi.exceptions.CustomDataAccessException;
import com.example.limittransactsapi.exceptions.CustomGenericException;
import com.example.limittransactsapi.mapper.CheckedOnLimitMapper;
import com.example.limittransactsapi.repository.CheckedOnLimitRepository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

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


    // Retrieve all records
    @Async("customExecutor")
    public CompletableFuture<List<CheckedOnLimitDTO>> findAllAsync() {
        return CompletableFuture.supplyAsync(() -> {
            List<CheckedOnLimit> entities = checkedOnLimitRepository.findAll();
            return entities.stream()
                    .map((e -> CheckedOnLimitMapper.INSTANCE.toDTO(e)))
                    .collect(Collectors.toList());
        }).exceptionally(ex ->{
            log.error("Ошибка при получении данных в репозитории метод findAllAsync");
           return new ArrayList<>();
        });
    }
    // Find a record by ID
    public Optional<CheckedOnLimitDTO> findById(UUID id) {
        var checkedOnLimit = checkedOnLimitRepository.findById(id);
        return checkedOnLimit.map(element -> CheckedOnLimitMapper.INSTANCE.toDTO(element));
    }
    @Async("customExecutor")
    public CompletableFuture<List<CheckedOnLimitDTO>> getCheckedOnLimitDTOsByLimitId(UUID limitId) {
        return CompletableFuture.supplyAsync(() -> {
                    if (limitId == null) {
                        throw new IllegalArgumentException("Limit Id can't be null");
                    }
                    List<CheckedOnLimit> entities = checkedOnLimitRepository.findAllByLimitId(limitId);
                    return entities.stream()
                            .map(element -> CheckedOnLimitMapper.INSTANCE.toDTO(element))
                            .collect(Collectors.toList());
                })
                .exceptionally(ex ->
                {
                    log.error("Ошибка при получении в репозитории, метод getCheckedOnLimitDTOsByLimitId");
                  return new ArrayList<>();
                });
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




    // Delete a record by ID
    public void deleteById(UUID id) {
        checkedOnLimitRepository.deleteById(id);
    }

}