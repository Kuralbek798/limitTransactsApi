package com.example.limittransactsapi.repository;

import com.example.limittransactsapi.Entity.PathForApi;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PathForApiRepository extends JpaRepository<PathForApi, UUID> {

    PathForApi findByDescription(String description);
}
