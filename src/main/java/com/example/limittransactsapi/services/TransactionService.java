package com.example.limittransactsapi.services;


import com.example.limittransactsapi.DTO.CheckedOnLimitDTO;
import com.example.limittransactsapi.DTO.LimitDTO;
import com.example.limittransactsapi.DTO.TransactionDTO;
import com.example.limittransactsapi.Entity.Transaction;
import com.example.limittransactsapi.mapper.LimitMapper;
import com.example.limittransactsapi.mapper.TransactionMapper;
import com.example.limittransactsapi.repository.LimitRepository;
import com.example.limittransactsapi.repository.TransactionRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.StoredProcedureQuery;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class TransactionService {


    private final TransactionRepository transactionRepository;
    private final LimitService limitService;
    private final CheckedOnLimitService checkedOnLimitService;

    @Autowired
    public TransactionService(TransactionRepository transactionRepository, LimitRepository limitRepository, LimitService limitService, CheckedOnLimitService checkedOnLimitService) {
        this.transactionRepository = transactionRepository;
        this.limitService = limitService;
        this.checkedOnLimitService = checkedOnLimitService;
    }

    //Creates transactions
    public ResponseEntity<TransactionDTO> createTransaction(TransactionDTO transactionDTO) {
        try {
            //receiving current limit for  category.
            Optional<LimitDTO> optionalCurrentLimitDto = limitService.getLatestLimitInOptionalLimitDto();
            if (optionalCurrentLimitDto.isPresent()) {
                 List<CheckedOnLimitDTO> listCheckedOnLimitDTO = checkedOnLimitService.getCheckedOnLimitDTOsByLimitId(optionalCurrentLimitDto.get().getId());
                if(listCheckedOnLimitDTO.size() > 0){
                   for(int i = 0; i < listCheckedOnLimitDTO.size(); i++){
                       listCheckedOnLimitDTO.get(i).getTransactionId();
                   }
                }

                Transaction transaction = transactionRepository.save(TransactionMapper.INSTANCE.toEntity(transactionDTO));
                log.info("Transaction Created: {}");
                // Calculating expenses for current month
                //checking limit

                BigDecimal limitSum = optionalCurrentLimitDto.get().getLimitSum();
                if (transaction.getId() != null) {
                    return ResponseEntity.ok(TransactionMapper.INSTANCE.toDTO(transaction));
                }


            }

            return null;

        } catch (DataIntegrityViolationException e) {
            log.error("Ошибка при сохранении в БД: {}", e.getMessage(), e);
            throw new DataIntegrityViolationException("Ошибка при сохранении транзакции: " + e.getMessage(), e);
        } catch (DataAccessException e) {
            log.error("Ошибка при доступе к БД: {}", e.getMessage(), e);
            throw new DataAccessException("Ошибка при сохранении транзакции: " + e.getMessage(), e) {
            };
        }
    }


}
