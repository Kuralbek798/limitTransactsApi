package com.example.limittransactsapi.repository;


import com.example.limittransactsapi.Entity.ExchangeRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ExchangeRateRepository extends JpaRepository<ExchangeRate, Long> {

    //@Query("SELECT e FROM YourEntity e WHERE e.dateField >= :date AND e.timeField >= :time " +
    //            "ORDER BY e.dateField DESC, e.timeField DESC")
    //    Optional<YourEntity> findLatestData(@Param("date") OffsetDateTime date, @Param("time") OffsetDateTime time);
      Optional<ExchangeRate> findTopByCurrencyPairOrderByDateDesc(String currencyPair);





}
