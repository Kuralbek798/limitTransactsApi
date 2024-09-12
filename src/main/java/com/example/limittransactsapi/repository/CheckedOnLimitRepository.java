package com.example.limittransactsapi.repository;

import com.example.limittransactsapi.Entity.CheckedOnLimit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CheckedOnLimitRepository extends JpaRepository<CheckedOnLimit, UUID>{

    List<CheckedOnLimit> findAllByLimitId(UUID limitId);
}
