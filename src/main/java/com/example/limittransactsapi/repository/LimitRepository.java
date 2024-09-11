package com.example.limittransactsapi.repository;


import com.example.limittransactsapi.Entity.Limit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigDecimal;
import java.util.Optional;

public interface LimitRepository extends JpaRepository<Limit, Long> {

    Optional<Limit> findTopByOrderByLimitDatetimeDesc();
    // Optional<Limit>findTopByLimitSum(BigDecimal limitSum);

    default Optional<Limit> saveWithOptional(Limit limit) {
        return Optional.ofNullable(save(limit));}
}
