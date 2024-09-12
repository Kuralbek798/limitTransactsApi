package com.example.limittransactsapi.repository;


import com.example.limittransactsapi.Entity.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;


@Repository
public interface LimitRepository extends JpaRepository<Limit, UUID> {

    Optional<Limit> findTopByOrderByLimitDatetimeDesc();
    // Optional<Limit>findTopByLimitSum(BigDecimal limitSum);

    default Optional<Limit> saveWithOptional(Limit limit) {
        return Optional.ofNullable(save(limit));}
}
