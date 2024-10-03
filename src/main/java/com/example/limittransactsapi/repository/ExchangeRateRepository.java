package com.example.limittransactsapi.repository;


import com.example.limittransactsapi.models.entity.ExchangeRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ExchangeRateRepository extends JpaRepository<ExchangeRate, UUID> {


        Optional<ExchangeRate> findTopByCurrencyPairOrderByDateTimeRateDesc(String currencyPair);




}
